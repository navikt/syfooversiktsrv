package no.nav.syfo.db

import com.zaxxer.hikari.HikariConfig

import com.zaxxer.hikari.HikariDataSource
import no.nav.syfo.Environment
import org.flywaydb.core.Flyway
import java.sql.Connection
import java.sql.ResultSet

enum class Role {
    ADMIN, USER, READONLY;
    override fun toString() = name.toLowerCase()
}

data class DaoConfig(val jdbcUrl: String, val password: String, val username: String, val databaseName: String) {
    var poolSize: Int
        get() = 3
        set(size) {
            this.poolSize = size
        }

}


class DevDatabase(daoConfig: DaoConfig) : Dao(daoConfig, null) {

    override fun runFlywayMigrations() = Flyway.configure().run {
            dataSource(daoConfig.jdbcUrl, daoConfig.username, daoConfig.password)
            load().migrate()
    }
}

class ProdDatabase(daoConfig: DaoConfig, initBlock: (context: Dao) -> Unit) : Dao(daoConfig, initBlock) {

    override fun runFlywayMigrations() = Flyway.configure().run {
        dataSource(daoConfig.jdbcUrl, daoConfig.username, daoConfig.password)
        initSql("SET ROLE \"${daoConfig.databaseName}-${Role.ADMIN}\"") // required for assigning proper owners for the tables
        load().migrate()
    }
}

/**
 * Base Database implementation.
 * Hooks up the database with the provided configuration/credentials
 */
abstract class Dao(val daoConfig: DaoConfig, private val initBlock: ((context: Dao) -> Unit)?) : DatabaseInterface {

    var dataSource: HikariDataSource

    init {

        dataSource = HikariDataSource(HikariConfig().apply {
            jdbcUrl = daoConfig.jdbcUrl
            username = daoConfig.username
            password = daoConfig.password
            maximumPoolSize = daoConfig.poolSize
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        }).also { it.validate() }

        runFlywayMigrations()

        afterInit()
    }


    fun updateCredentials(username: String, password: String) {
        dataSource.apply {
            hikariConfigMXBean.setPassword(password)
            hikariConfigMXBean.setUsername(username)
            hikariPoolMXBean.softEvictConnections()
        }
    }

    override val connection: Connection
        get() = dataSource.connection

    private fun afterInit() = initBlock?.let { run(it) }

    abstract fun runFlywayMigrations(): Int

}


fun <T> ResultSet.toList(mapper: ResultSet.() -> T) = mutableListOf<T>().apply {
    while (next()) {
        add(mapper())
    }
}

interface DatabaseInterface {
    val connection: Connection
}
