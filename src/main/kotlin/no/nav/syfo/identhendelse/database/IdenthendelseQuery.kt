package no.nav.syfo.identhendelse.database

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.domain.PersonIdent

const val queryUpdatePersonOversiktStatusFnr =
    """
        UPDATE PERSON_OVERSIKT_STATUS
        SET fnr = ?
        WHERE fnr = ?
    """

fun DatabaseInterface.updatePersonOversiktStatusFnr(nyPersonident: PersonIdent, gammelPersonident: PersonIdent): Int {
    var updatedRows: Int
    this.connection.use { connection ->
        updatedRows = connection.prepareStatement(queryUpdatePersonOversiktStatusFnr).use {
            it.setString(1, nyPersonident.value)
            it.setString(2, gammelPersonident.value)
            it.executeUpdate()
        }.also {
            connection.commit()
        }
    }
    return updatedRows
}