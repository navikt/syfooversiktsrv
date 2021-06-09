package no.nav.syfo

import com.typesafe.config.ConfigFactory
import io.ktor.application.*
import io.ktor.config.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.*
import kotlinx.coroutines.slf4j.MDCContext
import no.nav.syfo.api.apiModule
import no.nav.syfo.api.authentication.getWellKnown
import no.nav.syfo.db.*
import no.nav.syfo.kafka.setupKafka
import no.nav.syfo.oversikthendelsetilfelle.OversikthendelstilfelleService
import no.nav.syfo.personstatus.OversiktHendelseService
import no.nav.syfo.util.getFileAsString
import no.nav.syfo.vault.Vault
import org.slf4j.LoggerFactory
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

        val environment = getEnvironment()
        val wellKnownVeileder = getWellKnown(env.aadDiscoveryUrl)

        module {
            init()
            kafkaModule()
            apiModule(
                applicationState = state,
                database = database,
                environment = environment,
                wellKnownVeileder = wellKnownVeileder,
                isProd = (envKind == "production")
            )
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

fun CoroutineScope.createListener(applicationState: ApplicationState, action: suspend CoroutineScope.() -> Unit): Job =
    launch {
        try {
            action()
        } finally {
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
