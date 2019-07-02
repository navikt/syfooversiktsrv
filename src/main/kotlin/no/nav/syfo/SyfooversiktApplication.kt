package no.nav.syfo

import com.auth0.jwk.JwkProviderBuilder
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.typesafe.config.ConfigFactory
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.application.log
import io.ktor.auth.Authentication
import io.ktor.auth.jwt.JWTPrincipal
import io.ktor.auth.jwt.jwt
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.apache.Apache
import io.ktor.client.engine.apache.ApacheEngineConfig
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.logging.DEFAULT
import io.ktor.client.features.logging.LogLevel
import io.ktor.client.features.logging.Logger
import io.ktor.client.features.logging.Logging
import io.ktor.config.HoconApplicationConfig
import io.ktor.features.CORS
import io.ktor.features.CallId
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.jackson
import io.ktor.response.respond
import io.ktor.routing.routing
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.slf4j.MDCContext
import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.api.getWellKnown
import no.nav.syfo.api.registerNaisApi
import no.nav.syfo.db.*
import no.nav.syfo.personstatus.*
import no.nav.syfo.personstatus.domain.VeilederBrukerKnytning
import no.nav.syfo.tilgangskontroll.TilgangskontrollConsumer
import no.nav.syfo.vault.Vault
import org.slf4j.LoggerFactory
import java.net.URL
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


data class ApplicationState(var running: Boolean = true, var initialized: Boolean = false)

val LOG = LoggerFactory.getLogger("no.nav.syfo.SyfooversiktApplicationKt")

val backgroundTasksContext = Executors.newFixedThreadPool(4).asCoroutineDispatcher() + MDCContext()

/**
 * Application entry point
 */
fun main() {
    val server = embeddedServer(Netty, applicationEngineEnvironment {
        log = LoggerFactory.getLogger("ktor.application")
        config = HoconApplicationConfig(ConfigFactory.load())

        connector {
            port = env.applicationPort
        }

        module {
            init()
            mainModule()
        }
    })
    Runtime.getRuntime().addShutdownHook(Thread {
        server.stop(10, 10, TimeUnit.SECONDS)
    })

    server.start(wait = true)
}


lateinit var database: DatabaseInterface
val state: ApplicationState = ApplicationState(running = false, initialized = false)
val env: Environment = getEnvironment()

/**
 * Init module, setup a database connection and initialize
 */
fun Application.init() {

    isDev {
        database = DevDatabase(DaoConfig(
                jdbcUrl = "jdbc:postgresql://localhost:5432/",
                databaseName = "syfooversiktsrv_dev",
                password = "password",
                username = "username")
        )

        state.initialized = true
        state.running = true

        launch {
            database.lagreBrukerKnytningPaEnhet(VeilederBrukerKnytning("999999", "fnr1", "0315"))
            database.lagreBrukerKnytningPaEnhet(VeilederBrukerKnytning("999999", "fnr2", "0315"))
            database.lagreBrukerKnytningPaEnhet(VeilederBrukerKnytning("999999", "fnr3", "0315"))
            database.lagreBrukerKnytningPaEnhet(VeilederBrukerKnytning("999999", "fnr4", "0315"))
        }
    }

    isProd {
        val vaultCredentialService = VaultCredentialService()

        val newCredentials = vaultCredentialService.getNewCredentials(env.mountPathVault, env.databaseName, Role.USER)

        database = ProdDatabase(DaoConfig(
                jdbcUrl = env.syfooversiktsrvDBURL,
                username = newCredentials.username,
                password = newCredentials.password,
                databaseName = env.databaseName)) { prodDatabase->
            // post init block
            // after successfully connecting to db

            // grab admin-role credentials to run flyway migrations
            vaultCredentialService.getNewCredentials(env.mountPathVault, env.databaseName, Role.ADMIN)
                    .let { prodDatabase.runFlywayMigrations(env.syfooversiktsrvDBURL, it.username, it.password) }


            // start a new renew-task and update credentials in the background
            vaultCredentialService.renewCredentialsTaskData = RenewCredentialsTaskData(env.mountPathVault, env.databaseName, Role.USER) {
                prodDatabase.updateCredentials(username = it.username, password = it.password)
            }

            state.initialized = true
            state.running = true
        }

        launch(backgroundTasksContext) {
            try {
                Vault.renewVaultTokenTask(state)
            } finally {
                state.running = false
            }
        }

        launch(backgroundTasksContext) {
            try {
                vaultCredentialService.runRenewCredentialsTask { state.running }
            } finally {
                state.running = false
            }
        }

    }
}


/**
 * Application main module, setting up all Features and routing for the application.
 * Loaded after the init-module (@see [Application.init])
 */
fun Application.mainModule() {

    val env = getEnvironment()

    install(CORS) {
        host(host = "nais.adeo.no", schemes = listOf("https"), subDomains = listOf("syfooversikt"))
        host(host = "nais.preprod.local", schemes = listOf("https"), subDomains = listOf("syfooversikt-q1"))
        host(host = "localhost", schemes = listOf("http", "https"))
        allowCredentials = true
    }

    install(ContentNegotiation) {
        jackson {
            registerKotlinModule()
            registerModule(JavaTimeModule())
            configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        }
    }


    install(Authentication) {
        val wellKnown = getWellKnown(env.aadDiscoveryUrl)
        val jwkProvider = JwkProviderBuilder(URL(wellKnown.jwks_uri))
                .cached(10, 24, TimeUnit.HOURS)
                .rateLimited(10, 1, TimeUnit.MINUTES)
                .build()
        jwt(name = "jwt") {
            verifier(jwkProvider, wellKnown.issuer)
            validate { credentials ->
                if (!credentials.payload.audience.contains(env.clientid)) {
                    log.warn(
                            "Auth: Unexpected audience for jwt {}, {}, {}",
                            StructuredArguments.keyValue("issuer", credentials.payload.issuer),
                            StructuredArguments.keyValue("audience", credentials.payload.audience),
                            StructuredArguments.keyValue("expectedAudience", env.clientid)
                    )
                    null
                } else {
                    JWTPrincipal(credentials.payload)
                }
            }
        }
    }
    install(CallId) {
        generate { UUID.randomUUID().toString() }
        verify { callId: String -> callId.isNotEmpty() }
        header(HttpHeaders.XCorrelationId)
    }

    install(StatusPages) {
        exception<Throwable> { cause ->
            call.respond(HttpStatusCode.InternalServerError, cause.message ?: "Unknown error")

            log.error("Caught exception", cause)
            throw cause
        }
    }

    isDev {
        LOG.info("Running in development mode")

    }

    isProd {
        LOG.info("Running in production mode")

    }

    val config: HttpClientConfig<ApacheEngineConfig>.() -> Unit = {
        install(JsonFeature) {
            serializer = JacksonSerializer {
                registerKotlinModule()
                registerModule(JavaTimeModule())
                configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            }
        }
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.INFO
        }
    }
    val httpClient = HttpClient(Apache, config)

    val personTildelingService = PersonTildelingService(database)
    val personoversiktStatusService = PersonoversiktStatusService(database)
    val tilgangskontrollConsumer = TilgangskontrollConsumer(env.syfotilgangskontrollUrl, httpClient)

    routing {
        registerNaisApi(state)
        registerPersonoversiktApi(tilgangskontrollConsumer, personoversiktStatusService)
        registerPersonTildelingApi(tilgangskontrollConsumer, personTildelingService)
    }
}



val Application.envKind get() = environment.config.property("ktor.environment").getString()
fun Application.isDev(block: () -> Unit) {
    if (envKind == "dev") block()
}

fun Application.isProd(block: () -> Unit) {
    if (envKind != "dev") {
        block()
    }
}
