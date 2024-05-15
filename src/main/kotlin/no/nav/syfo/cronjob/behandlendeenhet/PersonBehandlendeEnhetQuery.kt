package no.nav.syfo.cronjob.behandlendeenhet

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.application.database.toList
import no.nav.syfo.domain.PersonIdent
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
        OR ((aktivitetskrav = 'NY' OR aktivitetskrav = 'AVVENT') AND aktivitetskrav_stoppunkt > '2023-03-10')
        OR behandlerdialog_svar_ubehandlet = 't'
        OR behandlerdialog_ubesvart_ubehandlet = 't'
        OR behandlerdialog_avvist_ubehandlet = 't'
        OR aktivitetskrav_vurder_stans_ubehandlet = 't'
        OR trenger_oppfolging = 't'
        OR behandler_bistand_ubehandlet = 't'
        OR arbeidsuforhet_vurder_avslag_ubehandlet = 't'
        OR (friskmelding_til_arbeidsformidling_fom IS NOT NULL AND friskmelding_til_arbeidsformidling_fom >= CURRENT_DATE) 
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
