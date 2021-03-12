package no.nav.syfo

import com.auth0.jwk.JwkProviderBuilder
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.typesafe.config.ConfigFactory
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.config.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.jackson.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.*
import kotlinx.coroutines.slf4j.MDCContext
import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.api.*
import no.nav.syfo.auth.isInvalidToken
import no.nav.syfo.batch.UpdateEnhetCronjob
import no.nav.syfo.batch.enhet.BehandlendeEnhetClient
import no.nav.syfo.batch.leaderelection.PodLeaderCoordinator
import no.nav.syfo.batch.sts.StsRestClient
import no.nav.syfo.db.*
import no.nav.syfo.kafka.setupKafka
import no.nav.syfo.oversikthendelsetilfelle.OversikthendelstilfelleService
import no.nav.syfo.personstatus.*
import no.nav.syfo.tilgangskontroll.TilgangskontrollConsumer
import no.nav.syfo.util.*
import no.nav.syfo.vault.Vault
import org.slf4j.LoggerFactory
import java.net.URL
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

data class ApplicationState(
    var running: Boolean = true,
    var initialized: Boolean = false
)

val LOG: org.slf4j.Logger = LoggerFactory.getLogger("no.nav.syfo.SyfooversiktApplicationKt")

val backgroundTasksContext = Executors.newFixedThreadPool(4).asCoroutineDispatcher() + MDCContext()

fun main() {
    val server = embeddedServer(Netty, applicationEngineEnvironment {
        log = LoggerFactory.getLogger("ktor.application")
        config = HoconApplicationConfig(ConfigFactory.load())

        connector {
            port = env.applicationPort
        }

        module {
            init()
            kafkaModule()
            serverModule()
            batchModule()
        }
    })

    Runtime.getRuntime().addShutdownHook(Thread {
        server.stop(10, 10, TimeUnit.SECONDS)
    })

    server.start(wait = false)
}

lateinit var database: DatabaseInterface
val state: ApplicationState = ApplicationState(running = false, initialized = false)
val env: Environment = getEnvironment()

fun Application.init() {
    isDev {
        database = DevDatabase(DbConfig(
            jdbcUrl = "jdbc:postgresql://localhost:5432/syfooversiktsrv_dev",
            databaseName = "syfooversiktsrv_dev",
            password = "password",
            username = "username")
        )

        state.running = true
    }

    isProd {
        val vaultCredentialService = VaultCredentialService()

        val newCredentials = vaultCredentialService.getNewCredentials(env.mountPathVault, env.databaseName, Role.USER)

        database = ProdDatabase(DbConfig(
            jdbcUrl = env.syfooversiktsrvDBURL,
            username = newCredentials.username,
            password = newCredentials.password,
            databaseName = env.databaseName,
            runMigrationsOninit = false)) { prodDatabase ->

            // i prod må vi kjøre flyway migrations med et eget sett brukernavn/passord
            vaultCredentialService.getNewCredentials(env.mountPathVault, env.databaseName, Role.ADMIN).let {
                prodDatabase.runFlywayMigrations(env.syfooversiktsrvDBURL, it.username, it.password)
            }

            vaultCredentialService.renewCredentialsTaskData = RenewCredentialsTaskData(env.mountPathVault, env.databaseName, Role.USER) {
                prodDatabase.updateCredentials(username = it.username, password = it.password)
            }

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

fun Application.batchModule() {
    val vaultSecrets = VaultSecrets(
        serviceuserPassword = getFileAsString("/secrets/serviceuser/syfooversiktsrv/password"),
        serviceuserUsername = getFileAsString("/secrets/serviceuser/syfooversiktsrv/username")
    )

    val stsClientRest = StsRestClient(
        baseUrl = env.stsRestUrl,
        username = vaultSecrets.serviceuserUsername,
        password = vaultSecrets.serviceuserPassword
    )
    val behandlendeEnhetClient = BehandlendeEnhetClient(
        baseUrl = env.behandlendeenhetUrl,
        stsRestClient = stsClientRest
    )
    val podLeaderCoordinator = PodLeaderCoordinator(
        env = env
    )

    val updateEnhetCronjob = UpdateEnhetCronjob(
        databaseInterface = database,
        applicationState = state,
        podLeaderCoordinator = podLeaderCoordinator,
        behandlendeEnhetClient = behandlendeEnhetClient
    )

    createListenerCronjob(state) {
        updateEnhetCronjob.start()
    }
}

fun Application.kafkaModule() {

    isDev {
    }

    isProd {
        val oversiktHendelseService = OversiktHendelseService(database)
        val oversikthendelstilfelleService = OversikthendelstilfelleService(database)

        launch(backgroundTasksContext) {
            val vaultSecrets = VaultSecrets(
                serviceuserPassword = getFileAsString("/secrets/serviceuser/syfooversiktsrv/password"),
                serviceuserUsername = getFileAsString("/secrets/serviceuser/syfooversiktsrv/username")
            )
            setupKafka(vaultSecrets, oversiktHendelseService, oversikthendelstilfelleService)
        }
    }
}

fun Application.serverModule() {

    val env = getEnvironment()

    install(CORS) {
        host(host = "nais.adeo.no", schemes = listOf("https"), subDomains = listOf("syfooversikt"))
        host(host = "nais.preprod.local", schemes = listOf("https"), subDomains = listOf("syfooversikt-q1", "syfooversikt"))
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
        retrieve { it.request.headers["X-Nav-CallId"] }
        retrieve { it.request.headers[HttpHeaders.XCorrelationId] }
        generate { UUID.randomUUID().toString() }
        verify { callId: String -> callId.isNotEmpty() }
        header(NAV_CALL_ID_HEADER)
    }

    install(StatusPages) {
        exception<Throwable> { cause ->
            call.respond(HttpStatusCode.InternalServerError, cause.message ?: "Unknown error")
            log.error("Caught exception", cause, getCallId())
            throw cause
        }
    }

    isProd {
        intercept(ApplicationCallPipeline.Call) {
            if (call.request.uri.contains(Regex("is_alive|is_ready|prometheus"))) {
                proceed()
                return@intercept
            }
            val cookies = call.request.cookies
            if (isInvalidToken(cookies)) {
                call.respond(HttpStatusCode.Unauthorized, "Ugyldig token")
                finish()
            } else {
                proceed()
            }
        }
    }

    val personTildelingService = PersonTildelingService(database)
    val personoversiktStatusService = PersonoversiktStatusService(database)
    val tilgangskontrollConsumer = TilgangskontrollConsumer(env.syfotilgangskontrollUrl)

    routing {
        registerPodApi(state)
        registerPrometheusApi()
        registerPersonoversiktApi(tilgangskontrollConsumer, personoversiktStatusService)
        registerPersonTildelingApi(tilgangskontrollConsumer, personTildelingService)
    }

    state.initialized = true
}

fun CoroutineScope.createListener(applicationState: ApplicationState, action: suspend CoroutineScope.() -> Unit): Job =
    launch {
        try {
            action()
        } finally {
            applicationState.running = false
        }
    }

fun createListenerCronjob(
    applicationState: ApplicationState,
    action: suspend CoroutineScope.() -> Unit
): Job =
    GlobalScope.launch {
        try {
            action()
        } catch (ex: Exception) {
            LOG.error("Noe gikk veldig galt, avslutter applikasjon: {}", ex.message)
        } finally {
            applicationState.initialized = false
            applicationState.running = false
        }
    }

val Application.envKind get() = environment.config.property("ktor.environment").getString()

fun Application.isDev(block: () -> Unit) {
    if (envKind == "dev") block()
}

fun Application.isProd(block: () -> Unit) {
    if (envKind == "production") block()
}

fun isPreProd(): Boolean = getEnvVar("NAIS_CLUSTER_NAME", "dev-fss") == "dev-fss"
