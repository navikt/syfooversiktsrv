package no.nav.syfo.cronjob.reaper

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.application.database.toList
import no.nav.syfo.personstatus.db.toPPersonOversiktStatus
import no.nav.syfo.personstatus.domain.PPersonOversiktStatus
import java.util.UUID

const val queryGetPersonerWithOutdatedVeiledertildeling =
    """
    SELECT *
    FROM person_oversikt_status
    WHERE tildelt_veileder IS NOT NULL
    AND oppfolgingstilfelle_end + INTERVAL '3 MONTH' < now()
    LIMIT 2000;
    """

fun DatabaseInterface.getPersonerWithOutdatedVeiledertildeling(): List<PPersonOversiktStatus> =
    this.connection.use { connection ->
        connection.prepareStatement(queryGetPersonerWithOutdatedVeiledertildeling).use {
            it.executeQuery().toList { toPPersonOversiktStatus() }
        }
    }

const val queryResetTildeltVeileder =
    """
    UPDATE person_oversikt_status
    SET tildelt_veileder = NULL
    WHERE uuid = ?
    """

fun DatabaseInterface.resetTildeltVeileder(
    uuid: UUID,
) {
    this.connection.use { connection ->
        connection.prepareStatement(queryResetTildeltVeileder).use {
            it.setString(1, uuid.toString())
            it.execute()
        }
        connection.commit()
    }
}
