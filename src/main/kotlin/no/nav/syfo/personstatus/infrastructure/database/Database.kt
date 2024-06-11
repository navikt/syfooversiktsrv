package no.nav.syfo.personstatus.infrastructure.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import com.zaxxer.hikari.metrics.prometheus.PrometheusMetricsTrackerFactory
import no.nav.syfo.metric.METRICS_REGISTRY
import org.flywaydb.core.Flyway
import java.sql.Connection
import java.sql.ResultSet
import kotlin.apply
import kotlin.run

data class DatabaseConfig(
    val jdbcUrl: String,
    val password: String,
    val username: String,
    val poolSize: Int = 8,
)

class Database(
    private val databaseConfig: DatabaseConfig
) : DatabaseInterface {

    override val connection: Connection
        get() = dataSource.connection

    private var dataSource: HikariDataSource = HikariDataSource(
        HikariConfig().apply {
            jdbcUrl = databaseConfig.jdbcUrl
            username = databaseConfig.username
            password = databaseConfig.password
            maximumPoolSize = databaseConfig.poolSize
            minimumIdle = 1
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_READ_COMMITTED"
            metricsTrackerFactory = PrometheusMetricsTrackerFactory(METRICS_REGISTRY.prometheusRegistry)
            validate()
        }
    )

    init {
        runFlywayMigrations()
    }

    private fun runFlywayMigrations() = Flyway.configure().run {
        dataSource(
            databaseConfig.jdbcUrl,
            databaseConfig.username,
            databaseConfig.password,
        )
        lockRetryCount(-1)
        load().migrate().migrationsExecuted
    }
}

interface DatabaseInterface {
    val connection: Connection
}

fun <T> ResultSet.toList(mapper: ResultSet.() -> T) = mutableListOf<T>().apply {
    while (next()) {
        add(mapper())
    }
}
