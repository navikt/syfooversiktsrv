package no.nav.syfo.personstatus

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.application.database.toList
import no.nav.syfo.domain.Virksomhetsnummer
import no.nav.syfo.personstatus.domain.PPersonOppfolgingstilfelleVirksomhet
import no.nav.syfo.personstatus.domain.PersonOppfolgingstilfelleVirksomhet
import java.sql.Connection
import java.sql.ResultSet
import java.time.OffsetDateTime
import java.util.*

const val queryCreatePersonOppfolgingstilfelleVirksomhet =
    """
    INSERT INTO PERSON_OPPFOLGINGSTILFELLE_VIRKSOMHET (
        id,
        uuid,
        created_at,
        person_oversikt_status_id,
        virksomhetsnummer,
        virksomhetsnavn
    ) VALUES (DEFAULT, ?, ?, ?, ?, ?)
    """

fun Connection.createPersonOppfolgingstilfelleVirksomhet(
    commit: Boolean,
    personOversiktStatusId: Int,
    personOppfolgingstilfelleVirksomhet: PersonOppfolgingstilfelleVirksomhet,
) {
    this.prepareStatement(queryCreatePersonOppfolgingstilfelleVirksomhet).use {
        it.setString(1, personOppfolgingstilfelleVirksomhet.uuid.toString())
        it.setObject(2, personOppfolgingstilfelleVirksomhet.createdAt)
        it.setInt(3, personOversiktStatusId)
        it.setString(4, personOppfolgingstilfelleVirksomhet.virksomhetsnummer.value)
        it.setString(5, personOppfolgingstilfelleVirksomhet.virksomhetsnavn)
        it.execute()
    }
    if (commit) {
        this.commit()
    }
}

fun Connection.createPersonOppfolgingstilfelleVirksomhetList(
    commit: Boolean,
    personOversiktStatusId: Int,
    personOppfolgingstilfelleVirksomhetList: List<PersonOppfolgingstilfelleVirksomhet>,
) {
    personOppfolgingstilfelleVirksomhetList.forEach { personOppfolgingstilfelleVirksomhet ->
        this.createPersonOppfolgingstilfelleVirksomhet(
            commit = commit,
            personOversiktStatusId = personOversiktStatusId,
            personOppfolgingstilfelleVirksomhet = personOppfolgingstilfelleVirksomhet,
        )
    }
}

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
