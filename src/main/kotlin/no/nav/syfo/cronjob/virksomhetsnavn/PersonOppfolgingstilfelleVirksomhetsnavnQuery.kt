package no.nav.syfo.cronjob.virksomhetsnavn

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.application.database.toList
import no.nav.syfo.domain.Virksomhetsnummer
import java.sql.ResultSet

const val queryUpdatePersonOppfolgingstilfelleVirksomhet =
    """
    UPDATE PERSON_OPPFOLGINGSTILFELLE_VIRKSOMHET
    SET virksomhetsnavn = ?
    WHERE id = ?
    """

fun DatabaseInterface.updatePersonOppfolgingstilfelleVirksomhetVirksomhetsnavn(
    personOppfolgingstilfelleVirksomhetId: Int,
    virksomhetsnavn: String,
) {
    this.connection.use { connection ->
        connection.prepareStatement(queryUpdatePersonOppfolgingstilfelleVirksomhet).use {
            it.setString(1, virksomhetsnavn)
            it.setInt(2, personOppfolgingstilfelleVirksomhetId)
            it.execute()
        }
        connection.commit()
    }
}

const val queryPersonOppfolgingstilfelleVirksomhetNoVirksomhetsnavnList =
    """
    SELECT PERSON_OPPFOLGINGSTILFELLE_VIRKSOMHET.id,virksomhetsnummer
    FROM PERSON_OPPFOLGINGSTILFELLE_VIRKSOMHET INNER JOIN PERSON_OVERSIKT_STATUS ON PERSON_OPPFOLGINGSTILFELLE_VIRKSOMHET.person_oversikt_status_id = PERSON_OVERSIKT_STATUS.id
    WHERE virksomhetsnavn IS NULL
    AND (motebehov_ubehandlet = 't' OR oppfolgingsplan_lps_bistand_ubehandlet = 't' OR dialogmotekandidat = 't' OR dialogmotesvar_ubehandlet = 't')
    ORDER BY PERSON_OPPFOLGINGSTILFELLE_VIRKSOMHET.created_at ASC
    LIMIT 1000
    """

fun DatabaseInterface.getPersonOppfolgingstilfelleVirksomhetMissingVirksomhetsnavnList(): List<Pair<Int, Virksomhetsnummer>> =
    this.connection.use { connection ->
        connection.prepareStatement(queryPersonOppfolgingstilfelleVirksomhetNoVirksomhetsnavnList).use {
            it.executeQuery().toList { toIdVirksomhetsnummerPair() }
        }
    }

fun ResultSet.toIdVirksomhetsnummerPair(): Pair<Int, Virksomhetsnummer> =
    Pair(
        getInt("id"),
        Virksomhetsnummer(getString("virksomhetsnummer")),
    )
