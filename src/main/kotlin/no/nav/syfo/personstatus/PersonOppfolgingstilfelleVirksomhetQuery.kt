package no.nav.syfo.personstatus

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.application.database.toList
import no.nav.syfo.domain.Virksomhetsnummer
import no.nav.syfo.personstatus.domain.PPersonOppfolgingstilfelleVirksomhet
import java.sql.ResultSet
import java.time.OffsetDateTime
import java.util.*

const val queryHentOppfolgingstilfelleForPerson =
    """
    SELECT *
    FROM PERSON_OPPFOLGINGSTILFELLE_VIRKSOMHET
    WHERE person_oversikt_status_id = ?
    """

fun DatabaseInterface.getPersonOppfolgingstilfelleVirksomhetList(
    pPersonOversikStatusId: Int,
): List<PPersonOppfolgingstilfelleVirksomhet> {
    return connection.use { connection ->
        connection.prepareStatement(queryHentOppfolgingstilfelleForPerson).use {
            it.setInt(1, pPersonOversikStatusId)
            it.executeQuery().toList { toPPersonOppfolgingstilfelleVirksomhet() }
        }
    }
}

fun ResultSet.toPPersonOppfolgingstilfelleVirksomhet(): PPersonOppfolgingstilfelleVirksomhet =
    PPersonOppfolgingstilfelleVirksomhet(
        id = getInt("id"),
        uuid = UUID.fromString(getString("uuid")),
        createdAt = getObject("created_at", OffsetDateTime::class.java),
        virksomhetsnummer = Virksomhetsnummer(getString("virksomhetsnummer")),
        virksomhetsnavn = getString("virksomhetsnavn")
    )
