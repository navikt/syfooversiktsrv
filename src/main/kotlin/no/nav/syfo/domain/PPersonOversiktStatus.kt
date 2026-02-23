package no.nav.syfo.domain

import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

data class PPersonOversiktStatus(
    val veilederIdent: String?,
    val uuid: UUID,
    val fnr: String,
    val fodselsdato: LocalDate?,
    val navn: String?,
    val id: Int,
    val enhet: String?,
    val tildeltEnhetUpdatedAt: OffsetDateTime?,
    val motebehovUbehandlet: Boolean?,
    val oppfolgingsplanLPSBistandUbehandlet: Boolean?,
    val dialogmotesvarUbehandlet: Boolean,
    val dialogmotekandidat: Boolean?,
    val dialogmotekandidatGeneratedAt: OffsetDateTime?,
    val motestatus: String?,
    val motestatusGeneratedAt: OffsetDateTime?,
    val oppfolgingstilfelleUpdatedAt: OffsetDateTime?,
    val oppfolgingstilfelleGeneratedAt: OffsetDateTime?,
    val oppfolgingstilfelleStart: LocalDate?,
    val oppfolgingstilfelleEnd: LocalDate?,
    val oppfolgingstilfelleBitReferanseUuid: UUID?,
    val oppfolgingstilfelleBitReferanseInntruffet: OffsetDateTime?,
    val behandlerdialogSvarUbehandlet: Boolean,
    val behandlerdialogUbesvartUbehandlet: Boolean,
    val behandlerdialogAvvistUbehandlet: Boolean,
    val isAktivOppfolgingsoppgave: Boolean,
    val behandlerBerOmBistandUbehandlet: Boolean,
    val antallSykedager: Int?,
    val friskmeldingTilArbeidsformidlingFom: LocalDate?,
    val isAktivArbeidsuforhetvurdering: Boolean,
    val isAktivSenOppfolgingKandidat: Boolean,
    val isAktivAktivitetskravvurdering: Boolean,
    val isAktivManglendeMedvirkningVurdering: Boolean,
    val isAktivKartleggingssporsmalVurdering: Boolean,
)

fun PPersonOversiktStatus.toPersonOversiktStatus(
    personOppfolgingstilfelleVirksomhetList: List<PersonOppfolgingstilfelleVirksomhet> = emptyList(),
) = PersonOversiktStatus(
    fnr = fnr,
    uuid = uuid,
    fodselsdato = fodselsdato,
    navn = navn,
    enhet = enhet,
    veilederIdent = veilederIdent,
    motebehovUbehandlet = motebehovUbehandlet,
    oppfolgingsplanLPSBistandUbehandlet = oppfolgingsplanLPSBistandUbehandlet,
    dialogmotesvarUbehandlet = dialogmotesvarUbehandlet,
    dialogmotekandidat = dialogmotekandidat,
    dialogmotekandidatGeneratedAt = dialogmotekandidatGeneratedAt,
    motestatus = motestatus,
    motestatusGeneratedAt = motestatusGeneratedAt,
    latestOppfolgingstilfelle = toPersonOppfolgingstilfelle(
        personOppfolgingstilfelleVirksomhetList = personOppfolgingstilfelleVirksomhetList,
    ),
    behandlerdialogSvarUbehandlet = behandlerdialogSvarUbehandlet,
    behandlerdialogUbesvartUbehandlet = behandlerdialogUbesvartUbehandlet,
    behandlerdialogAvvistUbehandlet = behandlerdialogAvvistUbehandlet,
    isAktivOppfolgingsoppgave = isAktivOppfolgingsoppgave,
    behandlerBerOmBistandUbehandlet = behandlerBerOmBistandUbehandlet,
    friskmeldingTilArbeidsformidlingFom = friskmeldingTilArbeidsformidlingFom,
    isAktivArbeidsuforhetvurdering = isAktivArbeidsuforhetvurdering,
    isAktivSenOppfolgingKandidat = isAktivSenOppfolgingKandidat,
    isAktivAktivitetskravvurdering = isAktivAktivitetskravvurdering,
    isAktivManglendeMedvirkningVurdering = isAktivManglendeMedvirkningVurdering,
    isAktivKartleggingssporsmalVurdering = isAktivKartleggingssporsmalVurdering
)

fun PPersonOversiktStatus.toPersonOppfolgingstilfelle(
    personOppfolgingstilfelleVirksomhetList: List<PersonOppfolgingstilfelleVirksomhet>,
): Oppfolgingstilfelle? {
    return if (
        this.oppfolgingstilfelleUpdatedAt != null &&
        this.oppfolgingstilfelleGeneratedAt != null &&
        this.oppfolgingstilfelleStart != null &&
        this.oppfolgingstilfelleEnd != null &&
        this.oppfolgingstilfelleBitReferanseInntruffet != null &&
        this.oppfolgingstilfelleBitReferanseUuid != null
    ) {
        Oppfolgingstilfelle(
            updatedAt = this.oppfolgingstilfelleUpdatedAt,
            generatedAt = this.oppfolgingstilfelleGeneratedAt,
            oppfolgingstilfelleStart = this.oppfolgingstilfelleStart,
            oppfolgingstilfelleEnd = this.oppfolgingstilfelleEnd,
            oppfolgingstilfelleBitReferanseInntruffet = this.oppfolgingstilfelleBitReferanseInntruffet,
            oppfolgingstilfelleBitReferanseUuid = this.oppfolgingstilfelleBitReferanseUuid,
            virksomhetList = personOppfolgingstilfelleVirksomhetList,
            antallSykedager = this.antallSykedager,
        )
    } else {
        null
    }
}
