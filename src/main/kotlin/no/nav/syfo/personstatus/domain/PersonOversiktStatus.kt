package no.nav.syfo.personstatus.domain

import no.nav.syfo.aktivitetskravvurdering.domain.AktivitetskravStatus
import no.nav.syfo.dialogmotestatusendring.domain.DialogmoteStatusendringType
import no.nav.syfo.oppfolgingstilfelle.domain.Oppfolgingstilfelle
import no.nav.syfo.oppfolgingstilfelle.domain.toPersonOppfolgingstilfelleDTO
import no.nav.syfo.personstatus.api.v2.model.PersonOversiktStatusDTO
import no.nav.syfo.personstatus.application.AktivitetskravDTO
import no.nav.syfo.personstatus.application.arbeidsuforhet.ArbeidsuforhetvurderingDTO
import no.nav.syfo.personstatus.application.oppfolgingsoppgave.OppfolgingsoppgaveDTO
import no.nav.syfo.util.isBeforeOrEqual
import no.nav.syfo.util.toLocalDateOslo
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
    val aktivitetskravVurderingFrist: LocalDate? = null,
    val behandlerdialogSvarUbehandlet: Boolean = false,
    val behandlerdialogUbesvartUbehandlet: Boolean = false,
    val behandlerdialogAvvistUbehandlet: Boolean = false,
    val aktivitetskravVurderStansUbehandlet: Boolean = false,
    val trengerOppfolging: Boolean = false,
    val behandlerBerOmBistandUbehandlet: Boolean = false,
    val isAktivArbeidsuforhetvurdering: Boolean = false,
    val friskmeldingTilArbeidsformidlingFom: LocalDate? = null,
    val isAktivSenOppfolgingKandidat: Boolean = false,
    val isAktivAktivitetskravvurdering: Boolean = false,
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
    (aktivitetskrav == AktivitetskravStatus.NY || aktivitetskrav == AktivitetskravStatus.AVVENT || aktivitetskrav == AktivitetskravStatus.NY_VURDERING) &&
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
        this.behandlerBerOmBistandUbehandlet ||
        this.isAktivArbeidsuforhetvurdering ||
        this.isAktivSenOppfolgingKandidat
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
    arbeidsuforhetvurdering: ArbeidsuforhetvurderingDTO?,
    oppfolgingsoppgave: OppfolgingsoppgaveDTO?,
    aktivitetskravvurdering: AktivitetskravDTO?,
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
        aktivitetskravActive = isActiveAktivitetskrav(arenaCutoff = arenaCutoff),
        aktivitetskravVurderingFrist = aktivitetskravVurderingFrist,
        behandlerdialogUbehandlet = hasActiveBehandlerdialogOppgave(),
        aktivitetskravVurderStansUbehandlet = aktivitetskravVurderStansUbehandlet,
        behandlerBerOmBistandUbehandlet = behandlerBerOmBistandUbehandlet,
        arbeidsuforhetvurdering = arbeidsuforhetvurdering,
        friskmeldingTilArbeidsformidlingFom = friskmeldingTilArbeidsformidlingFom,
        oppfolgingsoppgave = oppfolgingsoppgave,
        isAktivSenOppfolgingKandidat = isAktivSenOppfolgingKandidat,
        aktivitetskravvurdering = aktivitetskravvurdering,
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
    }
