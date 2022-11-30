package no.nav.syfo.identhendelse.database

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.domain.PersonIdent

const val queryUpdatePersonOversiktStatusFnr =
    """
        UPDATE PERSON_OVERSIKT_STATUS
        SET fnr = ?
        WHERE fnr = ?
    """

fun DatabaseInterface.updatePersonOversiktStatusFnr(nyPersonident: PersonIdent, gammelPersonident: PersonIdent): Int {
    var updatedRows: Int
    this.connection.use { connection ->
        updatedRows = connection.prepareStatement(queryUpdatePersonOversiktStatusFnr).use {
            it.setString(1, nyPersonident.value)
            it.setString(2, gammelPersonident.value)
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
        SET tildelt_veileder = ?
        WHERE fnr = ?
    """

fun DatabaseInterface.updatePersonOversiktStatusVeileder(veilederIdent: String, personident: PersonIdent): Int {
    var updatedRows: Int
    this.connection.use { connection ->
        updatedRows = connection.prepareStatement(queryUpdatePersonOversiktStatusVeileder).use {
            it.setString(1, veilederIdent)
            it.setString(2, personident.value)
            it.executeUpdate()
        }.also {
            connection.commit()
        }
    }
    return updatedRows
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
