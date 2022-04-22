package no.nav.syfo.application.database

import io.ktor.server.application.*
import no.nav.syfo.isDev
import no.nav.syfo.isProd

lateinit var database: DatabaseInterface

fun Application.databaseModule(
    databaseEnvironment: DatabaseEnvironment,
) {
    isDev {
        database = Database(
            databaseConfig = DatabaseConfig(
                jdbcUrl = "jdbc:postgresql://localhost:5432/syfooversiktsrv_dev",
                password = "password",
                username = "username",
            )
        )
    }

    isProd {
        database = Database(
            databaseConfig = DatabaseConfig(
                jdbcUrl = databaseEnvironment.jdbcUrl(),
                username = databaseEnvironment.username,
                password = databaseEnvironment.password,
            )
        )
    }
}
