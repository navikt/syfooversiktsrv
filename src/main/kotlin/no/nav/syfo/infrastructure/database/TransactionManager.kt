package no.nav.syfo.infrastructure.database

import no.nav.syfo.application.ITransactionManager
import java.sql.Connection

class TransactionManager(private val database: DatabaseInterface) : ITransactionManager {

    override fun <T> transaction(block: (Connection) -> T): T =
        database.connection.use { connection ->
            val result = block(connection)
            connection.commit()
            result
        }
}
