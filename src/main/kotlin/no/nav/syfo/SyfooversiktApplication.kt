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
import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.api.getWellKnown
import no.nav.syfo.api.registerNaisApi
import no.nav.syfo.db.*
import no.nav.syfo.personstatus.PersonTildelingService
import no.nav.syfo.personstatus.PersonoversiktStatusService
import no.nav.syfo.personstatus.registerPersonTildelingApi
import no.nav.syfo.personstatus.registerPersonoversiktApi
import no.nav.syfo.tilgangskontroll.TilgangskontrollConsumer
import org.slf4j.LoggerFactory
import java.net.URL
import java.util.*
import java.util.concurrent.TimeUnit


val LOG = LoggerFactory.getLogger("no.nav.syfo.SyfooversiktApplication")

/**
 * Main method to run the server
 */
fun main() {
    val server= embeddedServer(Netty, applicationEngineEnvironment {
        log = LoggerFactory.getLogger("ktor.application")
        config = HoconApplicationConfig(ConfigFactory.load())

        connector {
            port = 8081
        }

        module {
            init()
            mainModule()
        }
    })
    Runtime.getRuntime().addShutdownHook(Thread {
        server.stop(10, 10, TimeUnit.SECONDS)
    })
    server.start(true)
}

lateinit var database: DatabaseInterface
lateinit var state: ApplicationState

val Application.env: Environment
    get() = getEnvironment()

fun Application.init() {
    state = ApplicationState(running = false, initialized = false)

    isDev {
        database = DevDatabase(DaoConfig(
                jdbcUrl = "jdbc:postgresql://localhost:5432/",
                databaseName = "test",
                password = "password",
                username = "username")
        )

        state.initialized = true
        state.running = true
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
            // start a new renew-task and update credentials
            vaultCredentialService.renewCredentialsTaskData = RenewCredentialsTaskData(env.mountPathVault, env.databaseName, Role.USER) {
                prodDatabase.updateCredentials(username = it.username, password = it.password)
            }
            state.initialized = true
            state.running = true
        }
    }
}


/**
 * Application main module. Loaded through the application.conf
 * located in: resources/application.conf
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
