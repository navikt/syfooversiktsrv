package no.nav.syfo.application

import java.sql.Connection

interface ITransactionManager {
    fun <T> transaction(block: (Connection) -> T): T
}
