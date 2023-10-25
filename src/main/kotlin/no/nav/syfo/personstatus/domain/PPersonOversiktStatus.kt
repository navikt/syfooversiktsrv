package no.nav.syfo.personstatus.domain

import no.nav.syfo.aktivitetskravvurdering.domain.AktivitetskravStatus
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

data class PPersonOversiktStatus(
    val veilederIdent: String?,
    val uuid: UUID,
    val fnr: String,
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
    val aktivitetskrav: String?,
    val aktivitetskravStoppunkt: LocalDate?,
    val aktivitetskravUpdatedAt: OffsetDateTime?,
    val aktivitetskravVurderingFrist: LocalDate?,
    val behandlerdialogSvarUbehandlet: Boolean,
    val behandlerdialogUbesvartUbehandlet: Boolean,
    val behandlerdialogAvvistUbehandlet: Boolean,
    val aktivitetskravVurderStansUbehandlet: Boolean,
    val huskelappActive: Boolean,
    val behandlerBerOmBistandUbehandlet: Boolean,
)

fun PPersonOversiktStatus.toPersonOversiktStatus(
    personOppfolgingstilfelleVirksomhetList: List<PersonOppfolgingstilfelleVirksomhet>,
) = PersonOversiktStatus(
    fnr = fnr,
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
    aktivitetskrav = aktivitetskrav?.let { AktivitetskravStatus.valueOf(aktivitetskrav) },
    aktivitetskravStoppunkt = aktivitetskravStoppunkt,
    aktivitetskravSistVurdert = aktivitetskravUpdatedAt,
    aktivitetskravVurderingFrist = aktivitetskravVurderingFrist,
    behandlerdialogSvarUbehandlet = behandlerdialogSvarUbehandlet,
    behandlerdialogUbesvartUbehandlet = behandlerdialogUbesvartUbehandlet,
    behandlerdialogAvvistUbehandlet = behandlerdialogAvvistUbehandlet,
    aktivitetskravVurderStansUbehandlet = aktivitetskravVurderStansUbehandlet,
    huskelappActive = huskelappActive,
    behandlerBerOmBistandUbehandlet = behandlerBerOmBistandUbehandlet,
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
        )
    } else {
        null
    }
}
