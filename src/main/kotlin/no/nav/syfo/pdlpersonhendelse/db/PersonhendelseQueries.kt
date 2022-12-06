package no.nav.syfo.pdlpersonhendelse.db

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.domain.PersonIdent

const val queryUpdatePersonOversiktStatusNavn =
    """
    UPDATE PERSON_OVERSIKT_STATUS
    SET navn = NULL
    WHERE fnr = ?
    """

fun DatabaseInterface.updatePersonOversiktStatusNavn(
    personIdent: PersonIdent,
): Int {
    var rowsAffected: Int
    this.connection.use { connection ->
        rowsAffected = connection.prepareStatement(queryUpdatePersonOversiktStatusNavn).use {
            it.setString(1, personIdent.value)
            it.executeUpdate()
        }.also {
            connection.commit()
        }
    }
    return rowsAffected
}
