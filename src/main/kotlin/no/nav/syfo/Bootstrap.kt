package no.nav.syfo

import com.auth0.jwk.JwkProviderBuilder
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.application.*
import io.ktor.auth.Authentication
import io.ktor.auth.jwt.JWTPrincipal
import io.ktor.auth.jwt.jwt
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.apache.Apache
import io.ktor.client.engine.apache.ApacheEngineConfig
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.logging.*
import io.ktor.features.*
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.jackson
import io.ktor.request.ApplicationRequest
import io.ktor.response.respond
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.prometheus.client.hotspot.DefaultExports
import kotlinx.coroutines.*
import kotlinx.coroutines.slf4j.MDCContext
import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.api.getWellKnown
import no.nav.syfo.api.registerNaisApi
import no.nav.syfo.db.*
import no.nav.syfo.personstatus.*
import no.nav.syfo.tilgangskontroll.TilgangskontrollConsumer
import no.nav.syfo.vault.Vault
import org.slf4j.LoggerFactory
import java.net.URL
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

data class ApplicationState(var running: Boolean = true, var initialized: Boolean = false)

val log: org.slf4j.Logger = LoggerFactory.getLogger("no.nav.syfo")

val backgroundTasksContext = Executors.newFixedThreadPool(4).asCoroutineDispatcher() + MDCContext()

fun main() = runBlocking(Executors.newFixedThreadPool(4).asCoroutineDispatcher()) {
    val env = getEnvironment()
    val applicationState = ApplicationState()

    DefaultExports.initialize()

    val vaultCredentialService = VaultCredentialService()
    val database = Database(env, vaultCredentialService)

    launch(backgroundTasksContext) {
        try {
            Vault.renewVaultTokenTask(applicationState)
        } finally {
            applicationState.running = false
        }
    }

    launch(backgroundTasksContext) {
        try {
            vaultCredentialService.runRenewCredentialsTask { applicationState.running }
        } finally {
            applicationState.running = false
        }
    }

    val applicationServer = embeddedServer(Netty, env.applicationPort) {
        install(CORS) {
            host(host = "nais.adeo.no", schemes = listOf("https"), subDomains = listOf("syfooversikt"))
            host(host = "nais.preprod.local", schemes = listOf("https"), subDomains = listOf("syfooversikt-q1"))
            host(host = "localhost", schemes = listOf("http", "https"))
            allowCredentials = true
        }
        initRouting(applicationState, database, env)
    }.start(wait = false)

    Runtime.getRuntime().addShutdownHook(Thread {
        applicationServer.stop(10, 10, TimeUnit.SECONDS)
    })

    applicationState.initialized = true
}

fun Application.initRouting(
        applicationState: ApplicationState,
        database: DatabaseInterface,
        env: Environment
) {
    val wellKnown = getWellKnown(env.aadDiscoveryUrl)
    val jwkProvider = JwkProviderBuilder(URL(wellKnown.jwks_uri))
            .cached(10, 24, TimeUnit.HOURS)
            .rateLimited(10, 1, TimeUnit.MINUTES)
            .build()
    install(ContentNegotiation) {
        jackson {
            registerKotlinModule()
            registerModule(JavaTimeModule())
            configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        }
    }
    install(Authentication) {
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
        registerNaisApi(applicationState)
        registerPersonoversiktApi(tilgangskontrollConsumer, personoversiktStatusService)
        registerPersonTildelingApi(tilgangskontrollConsumer, personTildelingService)
    }
}

internal fun ApplicationRequest.url(): String {
    val port = when (origin.port) {
        in listOf(80, 443) -> ""
        else -> ":${origin.port}"
    }
    return "${origin.scheme}://${origin.host}$port${origin.uri}"
}
