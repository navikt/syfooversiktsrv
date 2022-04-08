package no.nav.syfo.cronjob.behandlendeenhet

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.application.database.toList
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.util.nowUTC
import java.sql.ResultSet

const val queryUpdatePersonTildeltEnhetUpdatedAt =
    """
    UPDATE PERSON_OVERSIKT_STATUS
    SET tildelt_enhet_updated_at = ?
    WHERE fnr = ?
    """

fun DatabaseInterface.updatePersonTildeltEnhetUpdatedAt(
    personIdent: PersonIdent,
) {
    val now = nowUTC()
    this.connection.use { connection ->
        connection.prepareStatement(queryUpdatePersonTildeltEnhetUpdatedAt).use {
            it.setObject(1, now)
            it.setString(2, personIdent.value)
            it.execute()
        }
        connection.commit()
    }
}

const val queryupdatePersonTildeltEnhet =
    """
    UPDATE PERSON_OVERSIKT_STATUS
    SET tildelt_enhet = ?, tildelt_enhet_updated_at = ?, sist_endret = ?
    WHERE fnr = ?
    """

fun DatabaseInterface.updatePersonTildeltEnhet(
    personIdent: PersonIdent,
    enhetId: String,
) {
    val now = nowUTC()
    this.connection.use { connection ->
        connection.prepareStatement(queryupdatePersonTildeltEnhet).use {
            it.setString(1, enhetId)
            it.setObject(2, now)
            it.setObject(3, nowUTC().toLocalDateTime())
            it.setString(4, personIdent.value)
            it.execute()
        }
        connection.commit()
    }
}

const val queryGetPersonIdentToUpdateTildeltEnhetList =
    """
    SELECT fnr
    FROM PERSON_OVERSIKT_STATUS
    WHERE (motebehov_ubehandlet = 't' OR moteplanlegger_ubehandlet = 't' OR oppfolgingsplan_lps_bistand_ubehandlet = 't')
    AND (tildelt_enhet_updated_at IS NULL OR tildelt_enhet_updated_at < oppfolgingstilfelle_updated_at)
    ORDER BY tildelt_enhet_updated_at ASC
    LIMIT 1000
    """

fun DatabaseInterface.getPersonIdentToUpdateTildeltEnhetList(): List<PersonIdent> =
    this.connection.use { connection ->
        connection.prepareStatement(queryGetPersonIdentToUpdateTildeltEnhetList).use {
            it.executeQuery().toList { toPersonIdent() }
        }
    }

fun ResultSet.toPersonIdent(): PersonIdent =
    PersonIdent(getString("fnr"))
