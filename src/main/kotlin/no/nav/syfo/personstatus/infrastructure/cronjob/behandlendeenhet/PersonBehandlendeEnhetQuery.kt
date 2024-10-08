package no.nav.syfo.personstatus.infrastructure.cronjob.behandlendeenhet

import no.nav.syfo.personstatus.infrastructure.database.DatabaseInterface
import no.nav.syfo.personstatus.infrastructure.database.toList
import no.nav.syfo.personstatus.domain.PersonIdent
import no.nav.syfo.util.nowUTC
import java.sql.ResultSet
import java.sql.Types

const val queryUpdatePersonTildeltEnhetUpdatedAt =
    """
    UPDATE PERSON_OVERSIKT_STATUS
    SET tildelt_enhet_updated_at = ?, sist_endret = ?
    WHERE fnr = ?
    """

fun DatabaseInterface.updatePersonTildeltEnhetUpdatedAt(
    personIdent: PersonIdent,
) {
    val now = nowUTC()
    this.connection.use { connection ->
        connection.prepareStatement(queryUpdatePersonTildeltEnhetUpdatedAt).use {
            it.setObject(1, now)
            it.setObject(2, now)
            it.setString(3, personIdent.value)
            it.execute()
        }
        connection.commit()
    }
}

const val queryUpdatePersonTildeltEnhetAndRemoveTildeltVeileder =
    """
    UPDATE PERSON_OVERSIKT_STATUS
    SET tildelt_veileder = ?, tildelt_enhet = ?, tildelt_enhet_updated_at = ?, sist_endret = ?
    WHERE fnr = ?
    """

fun DatabaseInterface.updatePersonTildeltEnhetAndRemoveTildeltVeileder(
    personIdent: PersonIdent,
    enhetId: String,
) {
    val now = nowUTC()
    this.connection.use { connection ->
        connection.prepareStatement(queryUpdatePersonTildeltEnhetAndRemoveTildeltVeileder).use {
            it.setNull(1, Types.NULL)
            it.setString(2, enhetId)
            it.setObject(3, now)
            it.setObject(4, nowUTC().toLocalDateTime())
            it.setString(5, personIdent.value)
            it.execute()
        }
        connection.commit()
    }
}

const val queryGetPersonerWithOppgaveAndOldEnhet =
    """
    SELECT fnr, tildelt_enhet
    FROM PERSON_OVERSIKT_STATUS
    WHERE (motebehov_ubehandlet = 't' 
        OR oppfolgingsplan_lps_bistand_ubehandlet = 't' 
        OR dialogmotekandidat = 't' 
        OR dialogmotesvar_ubehandlet = 't'
        OR behandlerdialog_svar_ubehandlet = 't'
        OR behandlerdialog_ubesvart_ubehandlet = 't'
        OR behandlerdialog_avvist_ubehandlet = 't'
        OR trenger_oppfolging = 't'
        OR behandler_bistand_ubehandlet = 't'
        OR friskmelding_til_arbeidsformidling_fom IS NOT NULL
        OR arbeidsuforhet_aktiv_vurdering = 't'
        OR is_aktiv_sen_oppfolging_kandidat = 't'
        OR is_aktiv_aktivitetskrav_vurdering = 't'
        OR is_aktiv_manglende_medvirkning_vurdering = 't'
        )
    AND (tildelt_enhet_updated_at IS NULL OR tildelt_enhet_updated_at <= NOW() - INTERVAL '24 HOURS')
    ORDER BY tildelt_enhet_updated_at ASC
    LIMIT 2000
    """

fun DatabaseInterface.getPersonerWithOppgaveAndOldEnhet(): List<Pair<PersonIdent, String?>> =
    this.connection.use { connection ->
        connection.prepareStatement(queryGetPersonerWithOppgaveAndOldEnhet).use {
            it.executeQuery().toList { toPersonIdentTildeltEnhetPair() }
        }
    }

fun ResultSet.toPersonIdentTildeltEnhetPair(): Pair<PersonIdent, String?> =
    Pair(
        PersonIdent(getString("fnr")),
        getString("tildelt_enhet"),
    )
