package no.nav.syfo.infrastructure.database.queries

import no.nav.syfo.domain.PPersonOversiktStatus
import no.nav.syfo.domain.PersonOversiktStatus
import no.nav.syfo.domain.toPersonOversiktStatus
import no.nav.syfo.infrastructure.database.toList
import no.nav.syfo.util.nowUTC
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Timestamp
import java.sql.Types
import java.sql.Types.NULL
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
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
        is_aktiv_manglende_medvirkning_vurdering,
        is_aktiv_kartleggingssporsmal_vurdering
    ) VALUES (DEFAULT, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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
        it.setBoolean(parameterIndex++, personOversiktStatus.isAktivKartleggingssporsmalVurdering)
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

fun ResultSet.toPPersonOversiktStatus(): PPersonOversiktStatus =
    PPersonOversiktStatus(
        id = getInt("id"),
        uuid = getString("uuid").let { UUID.fromString(it) },
        veilederIdent = getString("tildelt_veileder"),
        fnr = getString("fnr"),
        fodselsdato = getObject("fodselsdato", LocalDate::class.java),
        navn = getString("name"),
        enhet = getString("tildelt_enhet"),
        tildeltEnhetUpdatedAt = getObject("tildelt_enhet_updated_at", OffsetDateTime::class.java),
        motebehovUbehandlet = getObject("motebehov_ubehandlet") as Boolean?,
        oppfolgingsplanLPSBistandUbehandlet = getObject("oppfolgingsplan_lps_bistand_ubehandlet") as Boolean?,
        dialogmotesvarUbehandlet = getObject("dialogmotesvar_ubehandlet") as Boolean,
        dialogmotekandidat = getObject("dialogmotekandidat") as Boolean?,
        dialogmotekandidatGeneratedAt = getObject("dialogmotekandidat_generated_at", OffsetDateTime::class.java),
        motestatus = getString("motestatus"),
        motestatusGeneratedAt = getObject("motestatus_generated_at", OffsetDateTime::class.java),
        oppfolgingstilfelleUpdatedAt = getObject("oppfolgingstilfelle_updated_at", OffsetDateTime::class.java),
        oppfolgingstilfelleGeneratedAt = getObject("oppfolgingstilfelle_generated_at", OffsetDateTime::class.java),
        oppfolgingstilfelleStart = getObject("oppfolgingstilfelle_start", LocalDate::class.java),
        oppfolgingstilfelleEnd = getObject("oppfolgingstilfelle_end", LocalDate::class.java),
        oppfolgingstilfelleBitReferanseUuid = getString("oppfolgingstilfelle_bit_referanse_uuid")?.let {
            UUID.fromString(
                it
            )
        },
        oppfolgingstilfelleBitReferanseInntruffet = getObject(
            "oppfolgingstilfelle_bit_referanse_inntruffet",
            OffsetDateTime::class.java
        ),
        behandlerdialogSvarUbehandlet = getBoolean("behandlerdialog_svar_ubehandlet"),
        behandlerdialogUbesvartUbehandlet = getBoolean("behandlerdialog_ubesvart_ubehandlet"),
        behandlerdialogAvvistUbehandlet = getBoolean("behandlerdialog_avvist_ubehandlet"),
        isAktivOppfolgingsoppgave = getBoolean("trenger_oppfolging"),
        behandlerBerOmBistandUbehandlet = getBoolean("behandler_bistand_ubehandlet"),
        antallSykedager = getObject("antall_sykedager") as Int?,
        friskmeldingTilArbeidsformidlingFom = getObject("friskmelding_til_arbeidsformidling_fom", LocalDate::class.java),
        isAktivArbeidsuforhetvurdering = getBoolean("arbeidsuforhet_aktiv_vurdering"),
        isAktivSenOppfolgingKandidat = getBoolean("is_aktiv_sen_oppfolging_kandidat"),
        isAktivAktivitetskravvurdering = getBoolean("is_aktiv_aktivitetskrav_vurdering"),
        isAktivManglendeMedvirkningVurdering = getBoolean("is_aktiv_manglende_medvirkning_vurdering"),
        isAktivKartleggingssporsmalVurdering = getBoolean("is_aktiv_kartleggingssporsmal_vurdering"),
    )
