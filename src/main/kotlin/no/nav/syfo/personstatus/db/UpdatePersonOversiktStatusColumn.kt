package no.nav.syfo.personstatus.db

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.application.database.toList
import no.nav.syfo.dialogmotestatusendring.domain.DialogmoteStatusendring
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.personstatus.domain.PPersonOversiktStatus
import no.nav.syfo.personstatus.domain.VeilederBrukerKnytning
import java.sql.Connection
import java.sql.Timestamp
import java.time.Instant
import java.time.OffsetDateTime

const val queryUpdatePersonOversiktStatusLPS =
    """
        UPDATE PERSON_OVERSIKT_STATUS
        SET oppfolgingsplan_lps_bistand_ubehandlet = ?,
        sist_endret = ?
        WHERE fnr = ?
    """

fun Connection.updatePersonOversiktStatusLPS(
    isLPSBistandUbehandlet: Boolean,
    fnr: PersonIdent,
) {
    val currentTime = Timestamp.from(Instant.now())

    this.prepareStatement(queryUpdatePersonOversiktStatusLPS).use {
        it.setBoolean(1, isLPSBistandUbehandlet)
        it.setObject(2, currentTime)
        it.setString(3, fnr.value)
        it.execute()
    }
}

const val queryUpdatePersonOversiktStatusMotestatus =
    """
        UPDATE PERSON_OVERSIKT_STATUS
        SET motestatus = ?,
        motestatus_generated_at = ?
        WHERE fnr = ?
    """

fun Connection.updatePersonOversiktStatusMotestatus(
    pPersonOversiktStatus: PPersonOversiktStatus,
    dialogmoteStatusendring: DialogmoteStatusendring,
) {
    this.prepareStatement(queryUpdatePersonOversiktStatusMotestatus).use {
        it.setString(1, dialogmoteStatusendring.type.name)
        it.setObject(2, dialogmoteStatusendring.endringTidspunkt)
        it.setString(3, pPersonOversiktStatus.fnr)
        it.execute()
    }
}

fun Connection.updatePersonOversiktStatusKandidat(
    pPersonOversiktStatus: PPersonOversiktStatus,
    kandidat: Boolean,
    generatedAt: OffsetDateTime,
) {
    this.prepareStatement(queryUpdatePersonOversiktStatusKandidat).use {
        it.setBoolean(1, kandidat)
        it.setObject(2, generatedAt)
        it.setString(3, pPersonOversiktStatus.fnr)
        it.execute()
    }
}

const val queryUpdatePersonOversiktStatusKandidat =
    """
        UPDATE PERSON_OVERSIKT_STATUS
        SET dialogmotekandidat = ?,
        dialogmotekandidat_generated_at = ?
        WHERE fnr = ?
    """

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
