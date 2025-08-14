package no.nav.syfo.personstatus.infrastructure.database.queries

import no.nav.syfo.personstatus.infrastructure.database.DatabaseInterface
import no.nav.syfo.personstatus.domain.PersonIdent
import java.sql.Timestamp
import java.time.Instant

const val queryUpdatePersonOversiktStatusNavnToNull =
    """
    UPDATE PERSON_OVERSIKT_STATUS
    SET name = NULL, sist_endret = ?
    WHERE fnr = ?
    """

fun DatabaseInterface.updatePersonOversiktStatusNavnToNull(
    personIdent: PersonIdent,
): Int {
    var rowsAffected: Int
    val now = Timestamp.from(Instant.now())
    this.connection.use { connection ->
        rowsAffected = connection.prepareStatement(queryUpdatePersonOversiktStatusNavnToNull).use {
            it.setObject(1, now)
            it.setString(2, personIdent.value)
            it.executeUpdate()
        }.also {
            connection.commit()
        }
    }
    return rowsAffected
}
