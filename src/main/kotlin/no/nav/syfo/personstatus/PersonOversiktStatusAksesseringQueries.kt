package no.nav.syfo.personstatus

import no.nav.syfo.db.DatabaseInterface
import no.nav.syfo.db.toList
import no.nav.syfo.personstatus.domain.VeilederBrukerKnytning
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import java.util.*

const val KNYTNING_IKKE_FUNNET = 0L

fun DatabaseInterface.hentBrukereTilknyttetVeileder(veileder: String): List<VeilederBrukerKnytning> {
    return connection.use { connection ->
        connection.prepareStatement(
                """
                        SELECT *
                        FROM PERSON_OVERSIKT_STATUS
                        WHERE tildelt_veileder = ?
                """
        ).use {
            it.setString(1, veileder)
            it.executeQuery().toList { toVeilederBrukerKnytning() }
        }
    }
}

fun DatabaseInterface.lagreBrukerKnytningPaEnhet(veilederBrukerKnytning: VeilederBrukerKnytning) {
    val id = oppdaterEnhetDersomKnytningFinnes(veilederBrukerKnytning)

    if (id == KNYTNING_IKKE_FUNNET) {
        val uuid = UUID.randomUUID().toString()
        val tidspunkt = Timestamp.from(Instant.now())

        val query = """INSERT INTO PERSON_OVERSIKT_STATUS (
            id,
            uuid,
            fnr,
            tildelt_veileder,
            tildelt_enhet,
            opprettet,
            sist_endret) VALUES (DEFAULT, ?, ?, ?, ?, ?, ?)"""

        connection.use { connection ->
            connection.prepareStatement(query).use {
                it.setString(1, uuid)
                it.setString(2, veilederBrukerKnytning.fnr)
                it.setString(3, veilederBrukerKnytning.veilederIdent)
                it.setString(4, veilederBrukerKnytning.enhet)
                it.setTimestamp(5, tidspunkt)
                it.setTimestamp(6, tidspunkt)
                it.execute().toString()
            }
            connection.commit()
        }
    }
}

fun DatabaseInterface.oppdaterEnhetDersomKnytningFinnes(veilederBrukerKnytning: VeilederBrukerKnytning): Long {
    var id = KNYTNING_IKKE_FUNNET

    val knytningerPaVeileder = connection.use { connection ->
        connection.prepareStatement(
                """
                         SELECT id
                         FROM PERSON_OVERSIKT_STATUS
                         WHERE fnr=?
                """
        ).use {
            it.setString(1, veilederBrukerKnytning.fnr)
            it.executeQuery().toList { getLong("id") }
        }
    }

    if (knytningerPaVeileder.isNotEmpty()) {
        id = knytningerPaVeileder[0]
        connection.use { connection ->
            connection.prepareStatement(
                    """
                         UPDATE PERSON_OVERSIKT_STATUS
                         SET tildelt_veileder = ?, tildelt_enhet = ?
                         WHERE id = ?
                """
            ).use {
                it.setString(1, veilederBrukerKnytning.veilederIdent)
                it.setString(2, veilederBrukerKnytning.enhet)
                it.setLong(3, id)
                it.executeUpdate()
            }
            connection.commit()
        }
    }
    connection.commit()

    return id
}

fun ResultSet.toVeilederBrukerKnytning(): VeilederBrukerKnytning =
        VeilederBrukerKnytning(
                veilederIdent = getString("tildelt_veileder").trim(),
                fnr = getString("fnr"),
                enhet = getString("tildelt_enhet")
        )
