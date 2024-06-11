package no.nav.syfo.personstatus.domain

import no.nav.syfo.aktivitetskravvurdering.domain.AktivitetskravStatus
import no.nav.syfo.dialogmotestatusendring.domain.DialogmoteStatusendringType
import no.nav.syfo.oppfolgingstilfelle.domain.Oppfolgingstilfelle
import no.nav.syfo.oppfolgingstilfelle.domain.toPersonOppfolgingstilfelleDTO
import no.nav.syfo.personstatus.api.v2.model.PersonOversiktStatusDTO
import no.nav.syfo.personstatus.application.arbeidsuforhet.ArbeidsuforhetvurderingDTO
import no.nav.syfo.util.isBeforeOrEqual
import no.nav.syfo.util.toLocalDateOslo
import no.nav.syfo.util.toLocalDateTimeOslo
import java.time.LocalDate
import java.time.OffsetDateTime

data class PersonOversiktStatus(
    val veilederIdent: String? = null,
    val fnr: String,
    val navn: String? = null,
    val enhet: String? = null,
    val motebehovUbehandlet: Boolean? = null,
    val oppfolgingsplanLPSBistandUbehandlet: Boolean? = null,
    val dialogmotesvarUbehandlet: Boolean = false,
    val dialogmotekandidat: Boolean? = null,
    val dialogmotekandidatGeneratedAt: OffsetDateTime? = null,
    val motestatus: String? = null,
    val motestatusGeneratedAt: OffsetDateTime? = null,
    val latestOppfolgingstilfelle: Oppfolgingstilfelle? = null,
    val aktivitetskrav: AktivitetskravStatus? = null,
    val aktivitetskravStoppunkt: LocalDate? = null,
    val aktivitetskravSistVurdert: OffsetDateTime? = null,
    val aktivitetskravVurderingFrist: LocalDate? = null,
    val behandlerdialogSvarUbehandlet: Boolean = false,
    val behandlerdialogUbesvartUbehandlet: Boolean = false,
    val behandlerdialogAvvistUbehandlet: Boolean = false,
    val aktivitetskravVurderStansUbehandlet: Boolean = false,
    val trengerOppfolging: Boolean = false,
    val trengerOppfolgingFrist: LocalDate? = null,
    val behandlerBerOmBistandUbehandlet: Boolean = false,
    val arbeidsuforhetVurderAvslagUbehandlet: Boolean = false,
    val isAktivArbeidsuforhetvurdering: Boolean = false,
    val friskmeldingTilArbeidsformidlingFom: LocalDate? = null,
)

fun PersonOversiktStatus.isDialogmotekandidat() =
    dialogmotekandidat == true &&
        latestOppfolgingstilfelle != null &&
        dialogmotekandidatGeneratedAt != null &&
        dialogmotekandidatGeneratedAt.toLocalDateOslo()
            .isAfter(latestOppfolgingstilfelle.oppfolgingstilfelleStart) &&
        dialogmotekandidatGeneratedAt.toLocalDateOslo().isBeforeOrEqual(LocalDate.now().minusDays(7)) &&
        noOpenDialogmoteInvitation()

fun PersonOversiktStatus.hasOpenDialogmoteInvitation() =
    motestatus == DialogmoteStatusendringType.INNKALT.name ||
        motestatus == DialogmoteStatusendringType.NYTT_TID_STED.name

fun PersonOversiktStatus.noOpenDialogmoteInvitation() = !hasOpenDialogmoteInvitation()

fun PersonOversiktStatus.isActiveAktivitetskrav(arenaCutoff: LocalDate) =
    (aktivitetskrav == AktivitetskravStatus.NY || aktivitetskrav == AktivitetskravStatus.AVVENT) &&
        aktivitetskravStoppunkt?.isAfter(arenaCutoff) ?: false

fun PersonOversiktStatus.hasActiveOppgave(arenaCutoff: LocalDate): Boolean {
    return this.oppfolgingsplanLPSBistandUbehandlet == true ||
        this.dialogmotesvarUbehandlet ||
        this.isDialogmotekandidat() ||
        (this.motebehovUbehandlet == true && this.latestOppfolgingstilfelle != null) ||
        this.isActiveAktivitetskrav(arenaCutoff = arenaCutoff) ||
        this.hasActiveBehandlerdialogOppgave() ||
        this.friskmeldingTilArbeidsformidlingFom != null ||
        this.aktivitetskravVurderStansUbehandlet ||
        this.trengerOppfolging ||
        this.behandlerBerOmBistandUbehandlet || this.arbeidsuforhetVurderAvslagUbehandlet || this.isAktivArbeidsuforhetvurdering
}

fun List<PersonOversiktStatus>.addPersonName(
    personIdentNameMap: Map<String, String>,
): List<PersonOversiktStatus> {
    return if (personIdentNameMap.isEmpty()) {
        this
    } else {
        this.map { personOversiktStatus ->
            if (personOversiktStatus.navn.isNullOrEmpty()) {
                personOversiktStatus.copy(
                    navn = personIdentNameMap[personOversiktStatus.fnr]
                )
            } else {
                personOversiktStatus
            }
        }
    }
}

fun PersonOversiktStatus.toPersonOversiktStatusDTO(
    arenaCutoff: LocalDate,
    arbeidsuforhetvurdering: ArbeidsuforhetvurderingDTO?
) =
    PersonOversiktStatusDTO(
        veilederIdent = veilederIdent,
        fnr = fnr,
        navn = navn ?: "",
        enhet = enhet ?: "",
        motebehovUbehandlet = motebehovUbehandlet,
        oppfolgingsplanLPSBistandUbehandlet = oppfolgingsplanLPSBistandUbehandlet,
        dialogmotesvarUbehandlet = dialogmotesvarUbehandlet,
        dialogmotekandidat = dialogmotekandidat?.let { isDialogmotekandidat() },
        motestatus = motestatus,
        latestOppfolgingstilfelle = latestOppfolgingstilfelle?.toPersonOppfolgingstilfelleDTO(),
        aktivitetskrav = aktivitetskrav?.name,
        aktivitetskravStoppunkt = aktivitetskravStoppunkt,
        aktivitetskravSistVurdert = aktivitetskravSistVurdert?.toLocalDateTimeOslo(),
        aktivitetskravActive = isActiveAktivitetskrav(arenaCutoff = arenaCutoff),
        aktivitetskravVurderingFrist = aktivitetskravVurderingFrist,
        behandlerdialogUbehandlet = hasActiveBehandlerdialogOppgave(),
        aktivitetskravVurderStansUbehandlet = aktivitetskravVurderStansUbehandlet,
        trengerOppfolging = trengerOppfolging,
        trengerOppfolgingFrist = trengerOppfolgingFrist,
        behandlerBerOmBistandUbehandlet = behandlerBerOmBistandUbehandlet,
        arbeidsuforhetVurderAvslagUbehandlet = arbeidsuforhetVurderAvslagUbehandlet,
        arbeidsuforhetvurdering = arbeidsuforhetvurdering,
        friskmeldingTilArbeidsformidlingFom = friskmeldingTilArbeidsformidlingFom,
    )

fun PersonOversiktStatus.hasActiveBehandlerdialogOppgave(): Boolean {
    return this.behandlerdialogSvarUbehandlet ||
        this.behandlerdialogUbesvartUbehandlet ||
        this.behandlerdialogAvvistUbehandlet
}

fun PersonOversiktStatus.applyHendelse(
    oversikthendelseType: OversikthendelseType,
): PersonOversiktStatus =
    when (oversikthendelseType) {
        OversikthendelseType.MOTEBEHOV_SVAR_MOTTATT -> this.copy(
            motebehovUbehandlet = true,
        )
        OversikthendelseType.MOTEBEHOV_SVAR_BEHANDLET -> this.copy(
            motebehovUbehandlet = false,
        )
        OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_MOTTATT -> this.copy(
            oppfolgingsplanLPSBistandUbehandlet = true,
        )
        OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_BEHANDLET -> this.copy(
            oppfolgingsplanLPSBistandUbehandlet = false,
        )
        OversikthendelseType.DIALOGMOTESVAR_MOTTATT -> this.copy(
            dialogmotesvarUbehandlet = true,
        )
        OversikthendelseType.DIALOGMOTESVAR_BEHANDLET -> this.copy(
            dialogmotesvarUbehandlet = false,
        )
        OversikthendelseType.BEHANDLERDIALOG_SVAR_MOTTATT -> this.copy(
            behandlerdialogSvarUbehandlet = true,
        )
        OversikthendelseType.BEHANDLERDIALOG_SVAR_BEHANDLET -> this.copy(
            behandlerdialogSvarUbehandlet = false,
        )
        OversikthendelseType.BEHANDLERDIALOG_MELDING_UBESVART_MOTTATT -> this.copy(
            behandlerdialogUbesvartUbehandlet = true,
        )
        OversikthendelseType.BEHANDLERDIALOG_MELDING_UBESVART_BEHANDLET -> this.copy(
            behandlerdialogUbesvartUbehandlet = false,
        )
        OversikthendelseType.BEHANDLERDIALOG_MELDING_AVVIST_MOTTATT -> this.copy(
            behandlerdialogAvvistUbehandlet = true,
        )
        OversikthendelseType.BEHANDLERDIALOG_MELDING_AVVIST_BEHANDLET -> this.copy(
            behandlerdialogAvvistUbehandlet = false,
        )
        OversikthendelseType.AKTIVITETSKRAV_VURDER_STANS_MOTTATT -> this.copy(
            aktivitetskravVurderStansUbehandlet = true,
        )
        OversikthendelseType.AKTIVITETSKRAV_VURDER_STANS_BEHANDLET -> this.copy(
            aktivitetskravVurderStansUbehandlet = false,
        )
        OversikthendelseType.BEHANDLER_BER_OM_BISTAND_MOTTATT -> this.copy(
            behandlerBerOmBistandUbehandlet = true
        )
        OversikthendelseType.BEHANDLER_BER_OM_BISTAND_BEHANDLET -> this.copy(
            behandlerBerOmBistandUbehandlet = false
        )
        OversikthendelseType.ARBEIDSUFORHET_VURDER_AVSLAG_MOTTATT -> this.copy(
            arbeidsuforhetVurderAvslagUbehandlet = true,
        )
        OversikthendelseType.ARBEIDSUFORHET_VURDER_AVSLAG_BEHANDLET -> this.copy(
            arbeidsuforhetVurderAvslagUbehandlet = false,
        )
    }
