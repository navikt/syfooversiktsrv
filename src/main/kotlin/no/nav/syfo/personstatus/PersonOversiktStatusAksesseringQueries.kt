package no.nav.syfo.personstatus

import no.nav.syfo.db.DatabaseInterface
import no.nav.syfo.db.toList
import no.nav.syfo.personstatus.domain.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import java.util.*

const val KNYTNING_IKKE_FUNNET = 0L

val DatabaseInterface.LOG: Logger
    get() = LoggerFactory.getLogger("no.nav.syfo.DatabaseInterface")

const val queryHentPersonResultatInternal = """
                         SELECT *
                         FROM PERSON_OVERSIKT_STATUS
                         WHERE fnr=?
                """

fun DatabaseInterface.hentPersonResultatInternal(fnr: String): List<PPersonOversiktStatus> {
    return connection.use { connection ->
        connection.prepareStatement(queryHentPersonResultatInternal).use {
            it.setString(1, fnr)
            it.executeQuery().toList { toPPersonOversiktStatus() }
        }
    }
}

fun DatabaseInterface.hentPersonResultat(fnr: String): List<PPersonOversiktStatus> {
    val query = """
                         SELECT *
                         FROM PERSON_OVERSIKT_STATUS
                         WHERE fnr=?
                """
    return connection.use { connection ->
        connection.prepareStatement(query).use {
            it.setString(1, fnr)
            it.executeQuery().toList { toPPersonOversiktStatus() }
        }
    }
}

const val queryHentUbehandledePersonerTilknyttetEnhet = """
                        SELECT *
                        FROM PERSON_OVERSIKT_STATUS
                        WHERE ((tildelt_enhet = ?)
                        AND (motebehov_ubehandlet = 't' OR moteplanlegger_ubehandlet = 't' OR oppfolgingsplan_lps_bistand_ubehandlet = 't'))
                """

fun DatabaseInterface.hentUbehandledePersonerTilknyttetEnhet(enhet: String): List<PPersonOversiktStatus> {
    return connection.use { connection ->
        connection.prepareStatement(queryHentUbehandledePersonerTilknyttetEnhet).use {
            it.setString(1, enhet)
            it.executeQuery().toList { toPPersonOversiktStatus() }
        }
    }
}

const val queryHentPersonerTilknyttetEnhet = """
                        SELECT *
                        FROM PERSON_OVERSIKT_STATUS
                        WHERE tildelt_enhet = ?
                """

fun DatabaseInterface.hentPersonerTilknyttetEnhet(enhet: String): List<PPersonOversiktStatus> {
    return connection.use { connection ->
        connection.prepareStatement(queryHentPersonerTilknyttetEnhet).use {
            it.setString(1, enhet)
            it.executeQuery().toList { toPPersonOversiktStatus() }
        }
    }
}

fun DatabaseInterface.hentBrukereTilknyttetVeileder(veileder: String): List<VeilederBrukerKnytning> {
    val query = """
                        SELECT *
                        FROM PERSON_OVERSIKT_STATUS
                        WHERE tildelt_veileder = ?
                """
    return connection.use { connection ->
        connection.prepareStatement(query).use {
            it.setString(1, veileder)
            it.executeQuery().toList { toVeilederBrukerKnytning() }
        }
    }
}

const val queryLagreBrukerKnytningPaEnhet = """INSERT INTO PERSON_OVERSIKT_STATUS (
            id,
            uuid,
            fnr,
            tildelt_veileder,
            tildelt_enhet,
            opprettet,
            sist_endret) VALUES (DEFAULT, ?, ?, ?, ?, ?, ?)"""

fun DatabaseInterface.lagreBrukerKnytningPaEnhet(veilederBrukerKnytning: VeilederBrukerKnytning) {
    val id = oppdaterEnhetDersomKnytningFinnes(veilederBrukerKnytning)

    if (id == KNYTNING_IKKE_FUNNET) {
        val uuid = UUID.randomUUID().toString()
        val tidspunkt = Timestamp.from(Instant.now())

        connection.use { connection ->

            connection.prepareStatement(queryLagreBrukerKnytningPaEnhet).use {
                it.setString(1, uuid)
                it.setString(2, veilederBrukerKnytning.fnr)
                it.setString(3, veilederBrukerKnytning.veilederIdent.trim())
                it.setString(4, veilederBrukerKnytning.enhet)
                it.setTimestamp(5, tidspunkt)
                it.setTimestamp(6, tidspunkt)

                it.execute()
            }
            connection.commit()
        }
    }
}

fun DatabaseInterface.oppdaterEnhetDersomKnytningFinnes(veilederBrukerKnytning: VeilederBrukerKnytning): Long {
    var id = KNYTNING_IKKE_FUNNET

    val selectQuery = """
                         SELECT id
                         FROM PERSON_OVERSIKT_STATUS
                         WHERE fnr=?
                """

    val knytningerPaVeileder = connection.use { connection ->
        connection.prepareStatement(selectQuery).use {
            it.setString(1, veilederBrukerKnytning.fnr)
            it.executeQuery().toList { getLong("id") }
        }
    }

    if (knytningerPaVeileder.isNotEmpty()) {
        id = knytningerPaVeileder[0]
        val updateQuery = """
                         UPDATE PERSON_OVERSIKT_STATUS
                         SET tildelt_veileder = ?
                         WHERE id = ?
                """
        connection.use { connection ->
            connection.prepareStatement(updateQuery).use {
                it.setString(1, veilederBrukerKnytning.veilederIdent)
                it.setLong(2, id)
                it.executeUpdate()
            }
            connection.commit()
        }
    }
    return id
}

fun ResultSet.toPPersonOversiktStatus(): PPersonOversiktStatus =
    PPersonOversiktStatus(
        id = getInt("id"),
        veilederIdent = getString("tildelt_veileder"),
        fnr = getString("fnr"),
        navn = getString("navn"),
        enhet = getString("tildelt_enhet"),
        motebehovUbehandlet = getObject("motebehov_ubehandlet") as Boolean?,
        moteplanleggerUbehandlet = getObject("moteplanlegger_ubehandlet") as Boolean?,
        oppfolgingsplanLPSBistandUbehandlet = getObject("oppfolgingsplan_lps_bistand_ubehandlet") as Boolean?
    )

fun ResultSet.toVeilederBrukerKnytning(): VeilederBrukerKnytning =
    VeilederBrukerKnytning(
        veilederIdent = getString("tildelt_veileder"),
        fnr = getString("fnr"),
        enhet = getString("tildelt_enhet")
    )
