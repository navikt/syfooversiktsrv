package no.nav.syfo.personstatus.db

import no.nav.syfo.aktivitetskravvurdering.domain.Aktivitetskrav
import no.nav.syfo.dialogmotestatusendring.domain.DialogmoteStatusendring
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.personstatus.domain.PPersonOversiktStatus
import java.sql.Connection
import java.sql.Timestamp
import java.time.Instant
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
    fnr: PersonIdent,
) {
    val currentTime = Timestamp.from(Instant.now())

    this.prepareStatement(queryUpdatePersonOversiktStatusLPS).use {
        it.setBoolean(1, isLPSBistandUbehandlet)
        it.setObject(2, currentTime)
        it.setString(3, fnr.value)
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
    pPersonOversiktStatus: PPersonOversiktStatus,
    dialogmoteStatusendring: DialogmoteStatusendring,
) {
    this.prepareStatement(queryUpdatePersonOversiktStatusMotestatus).use {
        it.setString(1, dialogmoteStatusendring.type.name)
        it.setObject(2, dialogmoteStatusendring.endringTidspunkt)
        it.setObject(3, Timestamp.from(Instant.now()))
        it.setString(4, pPersonOversiktStatus.fnr)
        it.execute()
    }
}

const val queryUpdatePersonOversiktStatusAktivitetskrav =
    """
        UPDATE PERSON_OVERSIKT_STATUS
        SET aktivitetskrav = ?,
        aktivitetskrav_sist_vurdert = ?,
        aktivitetskrav_stoppunkt = ?,
        aktivitetskrav_vurdering_frist = ?,
        sist_endret = ?
        WHERE fnr = ?
    """

fun Connection.updatePersonOversiktStatusAktivitetskrav(
    pPersonOversiktStatus: PPersonOversiktStatus,
    aktivitetskrav: Aktivitetskrav,
) {
    this.prepareStatement(queryUpdatePersonOversiktStatusAktivitetskrav).use {
        it.setString(1, aktivitetskrav.status.name)
        it.setObject(2, aktivitetskrav.sistVurdert)
        it.setObject(3, aktivitetskrav.stoppunkt)
        it.setObject(4, aktivitetskrav.vurderingFrist)
        it.setObject(5, Timestamp.from(Instant.now()))
        it.setString(6, pPersonOversiktStatus.fnr)
        it.execute()
    }
}

fun Connection.updatePersonOversiktStatusKandidat(
    pPersonOversiktStatus: PPersonOversiktStatus,
    kandidat: Boolean,
    generatedAt: OffsetDateTime,
) {
    this.prepareStatement(queryUpdatePersonOversiktStatusKandidat).use {
        it.setBoolean(1, kandidat)
        it.setObject(2, generatedAt)
        it.setObject(3, Timestamp.from(Instant.now()))
        it.setString(4, pPersonOversiktStatus.fnr)
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

fun Connection.updateAktivitetskravForhandsvarselFristExpired(
    isAktivitetskravForhandsvarselFristExpiredUbehandlet: Boolean,
    personIdent: PersonIdent,
) {
    // TODO: Add query
}
