package no.nav.syfo.infrastructure.database.queries

import no.nav.syfo.infrastructure.database.DatabaseInterface
import no.nav.syfo.domain.PersonIdent
import java.sql.Timestamp
import java.time.Instant
import java.time.OffsetDateTime

const val queryUpdatePersonOversiktStatusFnr =
    """
        UPDATE PERSON_OVERSIKT_STATUS
        SET fnr = ?, sist_endret = ?
        WHERE fnr = ?
    """

fun DatabaseInterface.updatePersonOversiktStatusFnr(nyPersonident: PersonIdent, gammelPersonident: PersonIdent): Int {
    var updatedRows: Int
    val now = Timestamp.from(Instant.now())
    this.connection.use { connection ->
        updatedRows = connection.prepareStatement(queryUpdatePersonOversiktStatusFnr).use {
            it.setString(1, nyPersonident.value)
            it.setTimestamp(2, now)
            it.setString(3, gammelPersonident.value)
            it.executeUpdate()
        }.also {
            connection.commit()
        }
    }
    return updatedRows
}

const val queryUpdatePersonOversiktStatusVeileder =
    """
        UPDATE PERSON_OVERSIKT_STATUS
        SET tildelt_veileder = ?, sist_endret = ?
        WHERE fnr = ?
    """

fun DatabaseInterface.updatePersonOversiktStatusVeileder(veilederIdent: String, personident: PersonIdent): Int {
    var updatedRows: Int
    val now = Timestamp.from(Instant.now())
    this.connection.use { connection ->
        updatedRows = connection.prepareStatement(queryUpdatePersonOversiktStatusVeileder).use {
            it.setString(1, veilederIdent)
            it.setObject(2, now)
            it.setString(3, personident.value)
            it.executeUpdate()
        }.also {
            connection.commit()
        }
    }
    return updatedRows
}

fun DatabaseInterface.updatePersonOversiktStatusDialogmotekandidat(
    personident: PersonIdent,
    kandidat: Boolean?,
    generatedAt: OffsetDateTime,
): Int = executeSingleRowUpdate(queryUpdatePersonOversiktStatusKandidat, kandidat, generatedAt, personident)

fun DatabaseInterface.updatePersonOversiktStatusMotestatusAndGeneratedAt(
    personident: PersonIdent,
    motestatus: String?,
    generatedAt: OffsetDateTime,
): Int = executeSingleRowUpdate(queryUpdatePersonOversiktStatusMotestatus, motestatus, generatedAt, personident)

private fun DatabaseInterface.executeSingleRowUpdate(
    query: String,
    value: Any?,
    generatedAt: OffsetDateTime,
    personident: PersonIdent,
): Int {
    this.connection.use { connection ->
        val updatedRows = connection.prepareStatement(query).use {
            it.setObject(1, value)
            it.setObject(2, generatedAt)
            it.setObject(3, Timestamp.from(Instant.now()))
            it.setString(4, personident.value)
            it.executeUpdate()
        }
        if (updatedRows != 1) {
            connection.rollback()
            throw IllegalStateException("Expected single row to be updated, got $updatedRows")
        }
        connection.commit()
        return updatedRows
    }
}

const val queryDeletePersonOversiktStatusFnr =
    """
        DELETE FROM PERSON_OVERSIKT_STATUS
        WHERE fnr = ?
    """

fun DatabaseInterface.queryDeletePersonOversiktStatusFnr(personident: String): Int {
    var deletedRows: Int
    this.connection.use { connection ->
        deletedRows = connection.prepareStatement(queryDeletePersonOversiktStatusFnr).use {
            it.setString(1, personident)
            it.executeUpdate()
        }.also {
            connection.commit()
        }
    }
    return deletedRows
}
