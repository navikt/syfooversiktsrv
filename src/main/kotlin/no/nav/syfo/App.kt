package no.nav.syfo

import com.typesafe.config.ConfigFactory
import io.ktor.application.*
import io.ktor.config.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.*
import kotlinx.coroutines.slf4j.MDCContext
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.api.apiModule
import no.nav.syfo.application.api.authentication.getWellKnown
import no.nav.syfo.db.*
import no.nav.syfo.kafka.setupKafka
import no.nav.syfo.oversikthendelsetilfelle.OversikthendelstilfelleService
import no.nav.syfo.personstatus.OversiktHendelseService
import no.nav.syfo.util.getFileAsString
import no.nav.syfo.vault.Vault
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

val LOG: org.slf4j.Logger = LoggerFactory.getLogger("no.nav.syfo.AppKt")

val backgroundTasksContext = Executors.newFixedThreadPool(4).asCoroutineDispatcher() + MDCContext()

fun main() {
    val applicationState = ApplicationState()
    val environment = getEnvironment()
    val wellKnownVeilederV2 = getWellKnown(
        wellKnownUrl = environment.azureAppWellKnownUrl,
    )

    val applicationEngineEnvironment = applicationEngineEnvironment {
        log = LoggerFactory.getLogger("ktor.application")
        config = HoconApplicationConfig(ConfigFactory.load())

        connector {
            port = environment.applicationPort
        }

        module {
            databaseModule(
                applicationState = applicationState,
                environment = environment,
            )
            apiModule(
                applicationState = applicationState,
                database = database,
                environment = environment,
                wellKnownVeilederV2 = wellKnownVeilederV2,
            )
            kafkaModule(
                applicationState = applicationState,
                environment = environment,
            )
        }
    }

    val server = embeddedServer(
        environment = applicationEngineEnvironment,
        factory = Netty,
    )
    Runtime.getRuntime().addShutdownHook(
        Thread {
            server.stop(10, 10, TimeUnit.SECONDS)
        }
    )

    server.start(wait = false)
}

lateinit var database: DatabaseInterface

fun Application.databaseModule(
    applicationState: ApplicationState,
    environment: Environment,
) {
    isDev {
        database = DevDatabase(
            DbConfig(
                jdbcUrl = "jdbc:postgresql://localhost:5432/syfooversiktsrv_dev",
                databaseName = "syfooversiktsrv_dev",
                password = "password",
                username = "username"
            )
        )

        applicationState.alive = true
    }

    isProd {
        val vaultCredentialService = VaultCredentialService()

        val newCredentials = vaultCredentialService.getNewCredentials(
            environment.mountPathVault,
            environment.databaseName,
            Role.USER,
        )

        database = ProdDatabase(
            DbConfig(
                jdbcUrl = environment.syfooversiktsrvDBURL,
                username = newCredentials.username,
                password = newCredentials.password,
                databaseName = environment.databaseName,
                runMigrationsOninit = false,
            )
        ) { prodDatabase ->

            // i prod må vi kjøre flyway migrations med et eget sett brukernavn/passord
            vaultCredentialService.getNewCredentials(environment.mountPathVault, environment.databaseName, Role.ADMIN)
                .let {
                    prodDatabase.runFlywayMigrations(environment.syfooversiktsrvDBURL, it.username, it.password)
                }

            vaultCredentialService.renewCredentialsTaskData =
                RenewCredentialsTaskData(environment.mountPathVault, environment.databaseName, Role.USER) {
                    prodDatabase.updateCredentials(username = it.username, password = it.password)
                }

            applicationState.alive = true
        }

        launch(backgroundTasksContext) {
            try {
                Vault.renewVaultTokenTask(applicationState)
            } finally {
                applicationState.alive = false
            }
        }

        launch(backgroundTasksContext) {
            try {
                vaultCredentialService.runRenewCredentialsTask { applicationState.alive }
            } finally {
                applicationState.alive = false
            }
        }
    }
}

fun Application.kafkaModule(
    applicationState: ApplicationState,
    environment: Environment,
) {
    val oversiktHendelseService = OversiktHendelseService(
        database = database,
    )
    val oversikthendelstilfelleService = OversikthendelstilfelleService(
        database = database,
    )

    launch(backgroundTasksContext) {
        val vaultSecrets = VaultSecrets(
            serviceuserPassword = getFileAsString("/secrets/serviceuser/syfooversiktsrv/password"),
            serviceuserUsername = getFileAsString("/secrets/serviceuser/syfooversiktsrv/username"),
        )
        setupKafka(
            applicationState = applicationState,
            environment = environment,
            oversiktHendelseService = oversiktHendelseService,
            oversikthendelstilfelleService = oversikthendelstilfelleService,
            vaultSecrets = vaultSecrets,
        )
    }
}

fun CoroutineScope.createListener(
    applicationState: ApplicationState,
    action: suspend CoroutineScope.() -> Unit,
): Job =
    launch {
        try {
            action()
        } finally {
            applicationState.alive = false
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
