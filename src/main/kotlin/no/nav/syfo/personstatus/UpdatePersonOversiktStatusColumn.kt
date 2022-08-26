package no.nav.syfo.personstatus.queries

import no.nav.syfo.domain.PersonIdent
import java.sql.Connection
import java.sql.Timestamp
import java.time.Instant

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
