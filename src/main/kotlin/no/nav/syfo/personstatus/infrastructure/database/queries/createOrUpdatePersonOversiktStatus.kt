package no.nav.syfo.personstatus.infrastructure.database.queries

import no.nav.syfo.personstatus.infrastructure.database.toList
import no.nav.syfo.personstatus.domain.*
import no.nav.syfo.util.nowUTC
import java.sql.*
import java.sql.Types.NULL
import java.time.Instant
import java.util.*

const val queryCreatePersonOversiktStatus =
    """
    INSERT INTO PERSON_OVERSIKT_STATUS (
        id,
        uuid,
        fnr,
        fodselsdato,
        name,
        tildelt_veileder,
        tildelt_enhet,
        tildelt_enhet_updated_at,
        opprettet,
        sist_endret,
        motebehov_ubehandlet,
        moteplanlegger_ubehandlet,
        oppfolgingsplan_lps_bistand_ubehandlet,
        dialogmotesvar_ubehandlet,
        oppfolgingstilfelle_updated_at,
        oppfolgingstilfelle_generated_at,
        oppfolgingstilfelle_start,
        oppfolgingstilfelle_end,
        oppfolgingstilfelle_bit_referanse_uuid,
        oppfolgingstilfelle_bit_referanse_inntruffet,
        dialogmotekandidat,
        dialogmotekandidat_generated_at,
        motestatus,
        motestatus_generated_at,
        behandlerdialog_svar_ubehandlet,
        behandlerdialog_ubesvart_ubehandlet,
        behandlerdialog_avvist_ubehandlet,
        trenger_oppfolging,
        behandler_bistand_ubehandlet,
        arbeidsuforhet_aktiv_vurdering,
        antall_sykedager,
        friskmelding_til_arbeidsformidling_fom,
        is_aktiv_sen_oppfolging_kandidat,
        is_aktiv_aktivitetskrav_vurdering,
        is_aktiv_manglende_medvirkning_vurdering
    ) VALUES (DEFAULT, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    RETURNING *
    """

fun Connection.createPersonOversiktStatus(
    commit: Boolean,
    personOversiktStatus: PersonOversiktStatus,
): PersonOversiktStatus {
    val tidspunkt = Timestamp.from(Instant.now())
    val now = nowUTC()
    var parameterIndex = 1

    val pPersonStatus: PPersonOversiktStatus = this.prepareStatement(queryCreatePersonOversiktStatus).use {
        it.setString(parameterIndex++, UUID.randomUUID().toString())
        it.setString(parameterIndex++, personOversiktStatus.fnr)
        it.setObject(parameterIndex++, personOversiktStatus.fodselsdato)
        it.setString(parameterIndex++, personOversiktStatus.navn)
        it.setString(parameterIndex++, personOversiktStatus.veilederIdent)
        it.setString(parameterIndex++, personOversiktStatus.enhet)
        if (personOversiktStatus.enhet != null) {
            it.setObject(parameterIndex++, now)
        } else {
            it.setNull(parameterIndex++, Types.TIMESTAMP_WITH_TIMEZONE)
        }
        it.setTimestamp(parameterIndex++, tidspunkt)
        it.setTimestamp(parameterIndex++, tidspunkt)
        if (personOversiktStatus.motebehovUbehandlet != null) {
            it.setBoolean(parameterIndex++, personOversiktStatus.motebehovUbehandlet)
        } else {
            it.setNull(parameterIndex++, NULL)
        }
        it.setNull(parameterIndex++, NULL)
        if (personOversiktStatus.oppfolgingsplanLPSBistandUbehandlet != null) {
            it.setBoolean(parameterIndex++, personOversiktStatus.oppfolgingsplanLPSBistandUbehandlet)
        } else {
            it.setNull(parameterIndex++, NULL)
        }
        it.setBoolean(parameterIndex++, personOversiktStatus.dialogmotesvarUbehandlet)
        it.setObject(parameterIndex++, personOversiktStatus.latestOppfolgingstilfelle?.updatedAt)
        it.setObject(parameterIndex++, personOversiktStatus.latestOppfolgingstilfelle?.generatedAt)
        it.setObject(parameterIndex++, personOversiktStatus.latestOppfolgingstilfelle?.oppfolgingstilfelleStart)
        it.setObject(parameterIndex++, personOversiktStatus.latestOppfolgingstilfelle?.oppfolgingstilfelleEnd)
        if (personOversiktStatus.latestOppfolgingstilfelle != null) {
            it.setString(
                parameterIndex++,
                personOversiktStatus.latestOppfolgingstilfelle.oppfolgingstilfelleBitReferanseUuid.toString()
            )
        } else {
            it.setNull(parameterIndex++, Types.CHAR)
        }
        it.setObject(parameterIndex++, personOversiktStatus.latestOppfolgingstilfelle?.oppfolgingstilfelleBitReferanseInntruffet)
        it.setObject(parameterIndex++, personOversiktStatus.dialogmotekandidat)
        it.setObject(parameterIndex++, personOversiktStatus.dialogmotekandidatGeneratedAt)
        it.setString(parameterIndex++, personOversiktStatus.motestatus)
        it.setObject(parameterIndex++, personOversiktStatus.motestatusGeneratedAt)
        it.setBoolean(parameterIndex++, personOversiktStatus.behandlerdialogSvarUbehandlet)
        it.setBoolean(parameterIndex++, personOversiktStatus.behandlerdialogUbesvartUbehandlet)
        it.setBoolean(parameterIndex++, personOversiktStatus.behandlerdialogAvvistUbehandlet)
        it.setBoolean(parameterIndex++, personOversiktStatus.isAktivOppfolgingsoppgave)
        it.setBoolean(parameterIndex++, personOversiktStatus.behandlerBerOmBistandUbehandlet)
        it.setBoolean(parameterIndex++, personOversiktStatus.isAktivArbeidsuforhetvurdering)
        if (personOversiktStatus.latestOppfolgingstilfelle?.antallSykedager != null) {
            it.setInt(parameterIndex++, personOversiktStatus.latestOppfolgingstilfelle.antallSykedager)
        } else it.setNull(parameterIndex++, Types.INTEGER)
        it.setObject(parameterIndex++, personOversiktStatus.friskmeldingTilArbeidsformidlingFom)
        it.setBoolean(parameterIndex++, personOversiktStatus.isAktivSenOppfolgingKandidat)
        it.setBoolean(parameterIndex++, personOversiktStatus.isAktivAktivitetskravvurdering)
        it.setBoolean(parameterIndex++, personOversiktStatus.isAktivManglendeMedvirkningVurdering)
        it.executeQuery().toList { toPPersonOversiktStatus() }.firstOrNull()
    } ?: throw SQLException("Creating PersonOversikStatus failed, no rows affected.")

    personOversiktStatus.latestOppfolgingstilfelle?.let { personOppfolgingstilfelle ->
        createPersonOppfolgingstilfelleVirksomhetList(
            commit = commit,
            personOversiktStatusId = pPersonStatus.id,
            personOppfolgingstilfelleVirksomhetList = personOppfolgingstilfelle.virksomhetList,
        )
    }
    if (commit) {
        this.commit()
    }
    return pPersonStatus.toPersonOversiktStatus()
}
