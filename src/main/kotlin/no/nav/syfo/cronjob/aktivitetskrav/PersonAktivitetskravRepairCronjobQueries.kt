package no.nav.syfo.cronjob.aktivitetskrav

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.application.database.toList
import no.nav.syfo.personstatus.db.toPPersonOversiktStatus
import no.nav.syfo.personstatus.domain.PPersonOversiktStatus

const val queryGetPersonOversiktAktivitetskravList =
    """
    SELECT *
    FROM PERSON_OVERSIKT_STATUS
    WHERE aktivitetskrav_stoppunkt IS NOT NULL AND aktivitetskrav_stoppunkt > '2023-03-10' AND aktivitetskrav='NY'
          AND (oppfolgingstilfelle_start IS NULL OR oppfolgingstilfelle_end < aktivitetskrav_stoppunkt)
    """

fun DatabaseInterface.getPersonOversiktStatusAktivitetskravList(): List<PPersonOversiktStatus> =
    this.connection.use { connection ->
        connection.prepareStatement(queryGetPersonOversiktAktivitetskravList).use {
            it.executeQuery().toList { toPPersonOversiktStatus() }
        }
    }
