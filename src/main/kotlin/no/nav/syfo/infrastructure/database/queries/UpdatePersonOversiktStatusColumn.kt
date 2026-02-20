package no.nav.syfo.infrastructure.database.queries

import no.nav.syfo.application.oppfolgingsoppgave.OppfolgingsoppgaveRecord
import no.nav.syfo.domain.DialogmoteStatusendring
import no.nav.syfo.domain.PersonIdent
import java.sql.Connection
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime

const val queryUpdatePersonOversiktStatusLPS =
    """
        UPDATE PERSON_OVERSIKT_STATUS
        SET oppfolgingsplan_lps_bistand_ubehandlet = ?,
        sist_endret = ?
        WHERE fnr = ?
    """

fun Connection.updatePersonOversiktStatusLPS(
    isLPSBistandUbehandlet: Boolean,
    personident: PersonIdent,
) {
    val currentTime = Timestamp.from(Instant.now())

    this.prepareStatement(queryUpdatePersonOversiktStatusLPS).use {
        it.setBoolean(1, isLPSBistandUbehandlet)
        it.setObject(2, currentTime)
        it.setString(3, personident.value)
        it.execute()
    }
}

const val queryUpdatePersonOversiktMotebehov =
    """
        UPDATE PERSON_OVERSIKT_STATUS
        SET motebehov_ubehandlet = ?,
        sist_endret = ?
        WHERE fnr = ?
    """

fun Connection.updatePersonOversiktMotebehov(
    hasMotebehov: Boolean,
    fnr: PersonIdent,
) {
    val currentTime = Timestamp.from(Instant.now())

    this.prepareStatement(queryUpdatePersonOversiktMotebehov).use {
        it.setBoolean(1, hasMotebehov)
        it.setObject(2, currentTime)
        it.setString(3, fnr.value)
        it.execute()
    }
}

const val queryUpdatePersonOversiktStatusDialogmotesvar =
    """
        UPDATE PERSON_OVERSIKT_STATUS
        SET dialogmotesvar_ubehandlet = ?,
        sist_endret = ?
        WHERE fnr = ?
    """

fun Connection.updatePersonOversiktStatusDialogmotesvar(
    isDialogmotesvarUbehandlet: Boolean,
    fnr: PersonIdent,
) {
    val currentTime = Timestamp.from(Instant.now())

    this.prepareStatement(queryUpdatePersonOversiktStatusDialogmotesvar).use {
        it.setBoolean(1, isDialogmotesvarUbehandlet)
        it.setObject(2, currentTime)
        it.setString(3, fnr.value)
        it.execute()
    }
}

const val queryUpdatePersonOversiktStatusMotestatus =
    """
        UPDATE PERSON_OVERSIKT_STATUS
        SET motestatus = ?,
        motestatus_generated_at = ?,
        sist_endret = ?
        WHERE fnr = ?
    """

fun Connection.updatePersonOversiktStatusMotestatus(
    personident: PersonIdent,
    dialogmoteStatusendring: DialogmoteStatusendring,
) {
    this.prepareStatement(queryUpdatePersonOversiktStatusMotestatus).use {
        it.setString(1, dialogmoteStatusendring.type.name)
        it.setObject(2, dialogmoteStatusendring.endringTidspunkt)
        it.setObject(3, Timestamp.from(Instant.now()))
        it.setString(4, personident.value)
        it.execute()
    }
}

fun Connection.updatePersonOversiktStatusKandidat(
    personident: PersonIdent,
    kandidat: Boolean,
    generatedAt: OffsetDateTime,
) {
    this.prepareStatement(queryUpdatePersonOversiktStatusKandidat).use {
        it.setBoolean(1, kandidat)
        it.setObject(2, generatedAt)
        it.setObject(3, Timestamp.from(Instant.now()))
        it.setString(4, personident.value)
        it.execute()
    }
}

const val queryUpdatePersonOversiktStatusKandidat =
    """
        UPDATE PERSON_OVERSIKT_STATUS
        SET dialogmotekandidat = ?,
        dialogmotekandidat_generated_at = ?,
        sist_endret = ?
        WHERE fnr = ?
    """

fun Connection.updatePersonOversiktStatusFriskmeldtTilArbeid(
    personident: PersonIdent,
    friskTilArbeidFom: LocalDate?,
) {
    this.prepareStatement(queryUpdatePersonOversiktStatusFriskmeldtTilArbeid).use {
        it.setObject(1, friskTilArbeidFom)
        it.setObject(2, Timestamp.from(Instant.now()))
        it.setString(3, personident.value)
        it.execute()
    }
}

const val queryUpdatePersonOversiktStatusFriskmeldtTilArbeid =
    """
        UPDATE PERSON_OVERSIKT_STATUS
        SET friskmelding_til_arbeidsformidling_fom = ?,
        sist_endret = ?
        WHERE fnr = ?
    """

const val queryUpdatePersonOversiktStatusBehandlerdialogSvar =
    """
        UPDATE PERSON_OVERSIKT_STATUS
        SET behandlerdialog_svar_ubehandlet = ?,
        sist_endret = ?
        WHERE fnr = ?
    """

fun Connection.updatePersonOversiktStatusBehandlerdialogSvar(
    isBehandlerdialogSvarUbehandlet: Boolean,
    personIdent: PersonIdent,
) {
    this.prepareStatement(queryUpdatePersonOversiktStatusBehandlerdialogSvar).use {
        it.setBoolean(1, isBehandlerdialogSvarUbehandlet)
        it.setObject(2, Timestamp.from(Instant.now()))
        it.setString(3, personIdent.value)
        it.execute()
    }
}

const val queryUpdatePersonOversiktStatusBehandlerdialogUbesvart =
    """
        UPDATE PERSON_OVERSIKT_STATUS
        SET behandlerdialog_ubesvart_ubehandlet = ?,
        sist_endret = ?
        WHERE fnr = ?
    """

fun Connection.updatePersonOversiktStatusBehandlerdialogUbesvart(
    isBehandlerdialogUbesvartUbehandlet: Boolean,
    personIdent: PersonIdent,
) {
    this.prepareStatement(queryUpdatePersonOversiktStatusBehandlerdialogUbesvart).use {
        it.setBoolean(1, isBehandlerdialogUbesvartUbehandlet)
        it.setObject(2, Timestamp.from(Instant.now()))
        it.setString(3, personIdent.value)
        it.execute()
    }
}

const val queryUpdatePersonOversiktStatusBehandlerdialogAvvist =
    """
        UPDATE PERSON_OVERSIKT_STATUS
        SET behandlerdialog_avvist_ubehandlet = ?, sist_endret = ?
        WHERE fnr = ?
    """

fun Connection.updatePersonOversiktStatusBehandlerdialogAvvist(
    isBehandlerdialogAvvistUbehandlet: Boolean,
    personIdent: PersonIdent,
) {
    this.prepareStatement(queryUpdatePersonOversiktStatusBehandlerdialogAvvist).use {
        it.setBoolean(1, isBehandlerdialogAvvistUbehandlet)
        it.setObject(2, Timestamp.from(Instant.now()))
        it.setString(3, personIdent.value)
        it.execute()
    }
}

const val queryUpdatePersonOversiktStatusOppfolgingsoppgave =
    """
        UPDATE PERSON_OVERSIKT_STATUS
        SET trenger_oppfolging = ?, sist_endret = ?
        WHERE fnr = ?
    """

fun Connection.updateOppfolgingsoppgave(
    oppfolgingsoppgave: OppfolgingsoppgaveRecord,
) {
    this.prepareStatement(queryUpdatePersonOversiktStatusOppfolgingsoppgave).use {
        it.setBoolean(1, oppfolgingsoppgave.isActive)
        it.setObject(2, Timestamp.from(Instant.now()))
        it.setString(3, oppfolgingsoppgave.personIdent)
        it.execute()
    }
}

const val queryUpdatePersonOversiktStatusBehandlerBerOmBistand =
    """
        UPDATE PERSON_OVERSIKT_STATUS
        SET behandler_bistand_ubehandlet = ?, sist_endret = ?
        WHERE fnr = ?
    """

fun Connection.updateBehandlerBerOmBistand(
    isBehandlerBerOmBistandUbehandlet: Boolean,
    personIdent: PersonIdent,
) {
    this.prepareStatement(queryUpdatePersonOversiktStatusBehandlerBerOmBistand).use {
        it.setBoolean(1, isBehandlerBerOmBistandUbehandlet)
        it.setObject(2, Timestamp.from(Instant.now()))
        it.setString(3, personIdent.value)
        it.execute()
    }
}
