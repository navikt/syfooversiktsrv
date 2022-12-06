package no.nav.syfo.pdlpersonhendelse.db

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.domain.PersonIdent

const val queryUpdatePersonOversiktStatusNavnToNull =
    """
    UPDATE PERSON_OVERSIKT_STATUS
    SET name = NULL
    WHERE fnr = ?
    """

fun DatabaseInterface.updatePersonOversiktStatusNavnToNull(
    personIdent: PersonIdent,
): Int {
    var rowsAffected: Int
    this.connection.use { connection ->
        rowsAffected = connection.prepareStatement(queryUpdatePersonOversiktStatusNavnToNull).use {
            it.setString(1, personIdent.value)
            it.executeUpdate()
        }.also {
            connection.commit()
        }
    }
    return rowsAffected
}
