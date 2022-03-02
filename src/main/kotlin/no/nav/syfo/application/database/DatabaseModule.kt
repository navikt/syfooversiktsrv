package no.nav.syfo.application.database

import io.ktor.application.*
import no.nav.syfo.application.Environment
import no.nav.syfo.isDev
import no.nav.syfo.isProd

lateinit var database: DatabaseInterface

fun Application.databaseModule(
    environment: Environment,
) {
    isDev {
        database = Database(
            databaseConfig = DatabaseConfig(
                jdbcUrl = "jdbc:postgresql://localhost:5432/syfooversiktsrv_dev",
                password = "password",
                username = "username"
            )
        )
    }

    isProd {
        database = Database(
            databaseConfig = DatabaseConfig(
                jdbcUrl = environment.jdbcUrl(),
                username = environment.databaseUsername,
                password = environment.databasePassword,
            )
        )
    }
}
