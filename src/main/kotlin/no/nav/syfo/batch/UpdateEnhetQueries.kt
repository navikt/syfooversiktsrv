package no.nav.syfo.batch

import no.nav.syfo.db.DatabaseInterface
import no.nav.syfo.db.toList
import no.nav.syfo.personstatus.domain.PPersonOversiktStatus
import no.nav.syfo.personstatus.toPPersonOversiktStatus
import java.sql.Timestamp
import java.time.Instant

fun DatabaseInterface.getPersonToUpdateEnhet(): List<PPersonOversiktStatus> {
    val query = """
                         SELECT *
                         FROM PERSON_OVERSIKT_STATUS
                         WHERE sist_endret > '2021-03-09' and sist_endret < '2021-03-13'
                """
    return connection.use { connection ->
        connection.prepareStatement(query).use {
            it.executeQuery().toList { toPPersonOversiktStatus() }
        }
    }
}

const val queryOppdaterPersonEnhetBatch =
    """
    UPDATE PERSON_OVERSIKT_STATUS
    SET tildelt_enhet = ?, sist_endret = ?
    WHERE fnr = ?
    """

fun DatabaseInterface.oppdaterPersonEnhetBatch(
    enhetId: String,
    personIdent: String
) {
    val tidspunkt = Timestamp.from(Instant.now())
    connection.use { connection ->
        connection.prepareStatement(queryOppdaterPersonEnhetBatch).use {
            it.setString(1, enhetId)
            it.setTimestamp(2, tidspunkt)
            it.setString(3, personIdent)
            it.execute()
        }
        connection.commit()
    }
}
