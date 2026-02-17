package no.nav.syfo.personstatus.application

import java.sql.Connection

interface ITransactionManager {
    fun <T> transaction(block: (Connection) -> T): T
}
