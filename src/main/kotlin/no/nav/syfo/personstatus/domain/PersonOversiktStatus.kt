package no.nav.syfo.personstatus.domain

import no.nav.syfo.dialogmotestatusendring.domain.DialogmoteStatusendringType
import no.nav.syfo.oppfolgingstilfelle.domain.Oppfolgingstilfelle
import no.nav.syfo.oppfolgingstilfelle.domain.toPersonOppfolgingstilfelleDTO
import no.nav.syfo.personstatus.api.v2.model.PersonOversiktStatusDTO
import no.nav.syfo.personstatus.application.aktivitetskrav.AktivitetskravDTO
import no.nav.syfo.personstatus.application.arbeidsuforhet.ArbeidsuforhetvurderingDTO
import no.nav.syfo.personstatus.application.manglendemedvirkning.ManglendeMedvirkningDTO
import no.nav.syfo.personstatus.application.meroppfolging.SenOppfolgingKandidatDTO
import no.nav.syfo.personstatus.application.oppfolgingsoppgave.OppfolgingsoppgaveDTO
import no.nav.syfo.util.isBeforeOrEqual
import no.nav.syfo.util.toLocalDateOslo
import java.time.LocalDate
import java.time.OffsetDateTime

data class PersonOversiktStatus(
    val veilederIdent: String? = null,
    val fnr: String,
    val fodselsdato: LocalDate? = null,
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
    val behandlerdialogSvarUbehandlet: Boolean = false,
    val behandlerdialogUbesvartUbehandlet: Boolean = false,
    val behandlerdialogAvvistUbehandlet: Boolean = false,
    val trengerOppfolging: Boolean = false,
    val behandlerBerOmBistandUbehandlet: Boolean = false,
    val isAktivArbeidsuforhetvurdering: Boolean = false,
    val friskmeldingTilArbeidsformidlingFom: LocalDate? = null,
    val isAktivSenOppfolgingKandidat: Boolean = false,
    val isAktivAktivitetskravvurdering: Boolean = false,
    val isAktivManglendeMedvirkningVurdering: Boolean = false,
) {
    fun updatePersonDetails(navn: String? = null, fodselsdato: LocalDate? = null): PersonOversiktStatus =
        if (navn != null && fodselsdato != null) {
            this.copy(navn = navn, fodselsdato = fodselsdato)
        } else if (navn != null) {
            this.copy(navn = navn)
        } else if (fodselsdato != null) {
            this.copy(fodselsdato = fodselsdato)
        } else {
            this
        }
}

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

fun PersonOversiktStatus.hasActiveOppgave(): Boolean {
    return this.oppfolgingsplanLPSBistandUbehandlet == true ||
        this.dialogmotesvarUbehandlet ||
        this.isDialogmotekandidat() ||
        (this.motebehovUbehandlet == true && this.latestOppfolgingstilfelle != null) ||
        this.isAktivAktivitetskravvurdering ||
        this.hasActiveBehandlerdialogOppgave() ||
        this.friskmeldingTilArbeidsformidlingFom != null ||
        this.trengerOppfolging ||
        this.behandlerBerOmBistandUbehandlet ||
        this.isAktivArbeidsuforhetvurdering ||
        this.isAktivSenOppfolgingKandidat ||
        this.isAktivManglendeMedvirkningVurdering
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
    arbeidsuforhetvurdering: ArbeidsuforhetvurderingDTO?,
    oppfolgingsoppgave: OppfolgingsoppgaveDTO?,
    aktivitetskravvurdering: AktivitetskravDTO?,
    manglendeMedvirkning: ManglendeMedvirkningDTO?,
    senOppfolgingKandidat: SenOppfolgingKandidatDTO?,
) =
    PersonOversiktStatusDTO(
        veilederIdent = veilederIdent,
        fnr = fnr,
        fodselsdato = fodselsdato,
        navn = navn ?: "",
        enhet = enhet ?: "",
        motebehovUbehandlet = motebehovUbehandlet,
        oppfolgingsplanLPSBistandUbehandlet = oppfolgingsplanLPSBistandUbehandlet,
        dialogmotesvarUbehandlet = dialogmotesvarUbehandlet,
        dialogmotekandidat = dialogmotekandidat?.let { isDialogmotekandidat() },
        motestatus = motestatus,
        latestOppfolgingstilfelle = latestOppfolgingstilfelle?.toPersonOppfolgingstilfelleDTO(),
        behandlerdialogUbehandlet = hasActiveBehandlerdialogOppgave(),
        behandlerBerOmBistandUbehandlet = behandlerBerOmBistandUbehandlet,
        arbeidsuforhetvurdering = arbeidsuforhetvurdering,
        friskmeldingTilArbeidsformidlingFom = friskmeldingTilArbeidsformidlingFom,
        oppfolgingsoppgave = oppfolgingsoppgave,
        aktivitetskravvurdering = aktivitetskravvurdering,
        manglendeMedvirkning = manglendeMedvirkning,
        senOppfolgingKandidat = senOppfolgingKandidat,
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
        OversikthendelseType.BEHANDLER_BER_OM_BISTAND_MOTTATT -> this.copy(
            behandlerBerOmBistandUbehandlet = true
        )
        OversikthendelseType.BEHANDLER_BER_OM_BISTAND_BEHANDLET -> this.copy(
            behandlerBerOmBistandUbehandlet = false
        )
    }
