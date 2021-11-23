package no.nav.syfo.application.database

import io.ktor.application.*
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.slf4j.MDCContext
import no.nav.syfo.*
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.db.*
import no.nav.syfo.vault.Vault
import java.util.concurrent.Executors

val backgroundTasksContext = Executors.newFixedThreadPool(4).asCoroutineDispatcher() + MDCContext()

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
        }

        launch(backgroundTasksContext) {
            try {
                Vault.renewVaultTokenTask(applicationState)
            } finally {
                applicationState.alive = false
                applicationState.ready = false
            }
        }

        launch(backgroundTasksContext) {
            try {
                vaultCredentialService.runRenewCredentialsTask { applicationState.alive }
            } finally {
                applicationState.alive = false
                applicationState.ready = false
            }
        }
    }
}
