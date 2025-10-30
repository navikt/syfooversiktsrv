package no.nav.syfo.personstatus.infrastructure.database.queries

import no.nav.syfo.personstatus.infrastructure.database.toList
import no.nav.syfo.personstatus.domain.PersonOppfolgingstilfelleVirksomhet
import no.nav.syfo.personstatus.domain.PPersonOppfolgingstilfelleVirksomhet
import no.nav.syfo.personstatus.domain.Virksomhetsnummer
import java.sql.Connection
import java.sql.ResultSet
import java.time.OffsetDateTime
import java.util.*
import kotlin.collections.filter
import kotlin.collections.forEach
import kotlin.collections.groupBy
import kotlin.collections.intersect
import kotlin.collections.map
import kotlin.collections.toTypedArray
import kotlin.jvm.java
import kotlin.use

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

const val queryDeletePersonOppfolgingstilfelleVirksomhetList =
    """
    DELETE FROM PERSON_OPPFOLGINGSTILFELLE_VIRKSOMHET
    WHERE uuid = ?
    """

fun Connection.deletePersonOppfolgingstilfelleVirksomhetList(
    personOppfolgingstilfelleVirksomhetToDeleteList: List<PPersonOppfolgingstilfelleVirksomhet>,
) {
    personOppfolgingstilfelleVirksomhetToDeleteList.forEach { virksomhet ->
        this.prepareStatement(queryDeletePersonOppfolgingstilfelleVirksomhetList).use {
            it.setString(1, virksomhet.uuid.toString())
            it.execute()
        }
    }
}

fun Connection.updatePersonOppfolgingstilfelleVirksomhetList(
    personOversiktStatusId: Int,
    personOppfolgingstilfelleVirksomhetList: List<PersonOppfolgingstilfelleVirksomhet>,
) {
    val virksomhetInDatabaseList = getPersonOppfolgingstilfelleVirksomhetList(
        pPersonOversikStatusId = personOversiktStatusId
    )
    val virksomhetnummerToKeepList = personOppfolgingstilfelleVirksomhetList.map { virksomhet ->
        virksomhet.virksomhetsnummer.value
    }.intersect(
        virksomhetInDatabaseList.map { virksomhet ->
            virksomhet.virksomhetsnummer.value
        }
    )

    val virksomhetToDeleteList = virksomhetInDatabaseList.filter { virksomhet ->
        !virksomhetnummerToKeepList.contains(virksomhet.virksomhetsnummer.value)
    }
    deletePersonOppfolgingstilfelleVirksomhetList(
        personOppfolgingstilfelleVirksomhetToDeleteList = virksomhetToDeleteList,
    )

    val virksomhetToCreateList = personOppfolgingstilfelleVirksomhetList.filter { virksomhet ->
        !virksomhetnummerToKeepList.contains(virksomhet.virksomhetsnummer.value)
    }
    createPersonOppfolgingstilfelleVirksomhetList(
        commit = false,
        personOversiktStatusId = personOversiktStatusId,
        personOppfolgingstilfelleVirksomhetList = virksomhetToCreateList,
    )
}

const val queryHentOppfolgingstilfelleForPerson =
    """
    SELECT *
    FROM PERSON_OPPFOLGINGSTILFELLE_VIRKSOMHET
    WHERE person_oversikt_status_id = ?
    """

fun Connection.getPersonOppfolgingstilfelleVirksomhetList(
    pPersonOversikStatusId: Int,
): List<PPersonOppfolgingstilfelleVirksomhet> {
    return this.prepareStatement(queryHentOppfolgingstilfelleForPerson).use {
        it.setInt(1, pPersonOversikStatusId)
        it.executeQuery().toList { toPPersonOppfolgingstilfelleVirksomhet() }
    }
}

const val queryHentOppfolgingstilfelleForIds =
    """
    SELECT *
    FROM PERSON_OPPFOLGINGSTILFELLE_VIRKSOMHET
    WHERE person_oversikt_status_id = ANY(?)
    """

fun Connection.getPersonOppfolgingstilfelleVirksomhetMap(
    pPersonOversikStatusIds: List<Int>,
): Map<Int, List<PPersonOppfolgingstilfelleVirksomhet>> {
    return this.prepareStatement(queryHentOppfolgingstilfelleForIds).use { preparedStatement ->
        preparedStatement.setArray(1, createArrayOf("INTEGER", pPersonOversikStatusIds.toTypedArray()))
        preparedStatement.executeQuery().toList { toPPersonOppfolgingstilfelleVirksomhet() }
            .groupBy { it.personOversiktStatusId }
    }
}

fun ResultSet.toPPersonOppfolgingstilfelleVirksomhet(): PPersonOppfolgingstilfelleVirksomhet =
    PPersonOppfolgingstilfelleVirksomhet(
        id = getInt("id"),
        uuid = UUID.fromString(getString("uuid")),
        createdAt = getObject("created_at", OffsetDateTime::class.java),
        virksomhetsnummer = Virksomhetsnummer(getString("virksomhetsnummer")),
        virksomhetsnavn = getString("virksomhetsnavn"),
        personOversiktStatusId = getInt("person_oversikt_status_id")
    )
