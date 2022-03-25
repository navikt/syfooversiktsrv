package no.nav.syfo.personstatus.domain

import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

data class PPersonOversiktStatus(
    val veilederIdent: String?,
    val fnr: String,
    val navn: String?,
    val id: Int,
    val enhet: String?,
    val tildeltEnhetUpdatedAt: OffsetDateTime?,
    val motebehovUbehandlet: Boolean?,
    val moteplanleggerUbehandlet: Boolean?,
    val oppfolgingsplanLPSBistandUbehandlet: Boolean?,
    val oppfolgingstilfelleUpdatedAt: OffsetDateTime?,
    val oppfolgingstilfelleGeneratedAt: OffsetDateTime?,
    val oppfolgingstilfelleStart: LocalDate?,
    val oppfolgingstilfelleEnd: LocalDate?,
    val oppfolgingstilfelleBitReferanseUuid: UUID?,
    val oppfolgingstilfelleBitReferanseInntruffet: OffsetDateTime?,
)

fun PPersonOversiktStatus.toPersonOversiktStatus(
    oppfolgingstilfeller: List<Oppfolgingstilfelle>,
): PersonOversiktStatus {
    return PersonOversiktStatus(
        fnr = this.fnr,
        navn = this.navn,
        enhet = this.enhet,
        veilederIdent = this.veilederIdent,
        motebehovUbehandlet = this.motebehovUbehandlet,
        moteplanleggerUbehandlet = this.moteplanleggerUbehandlet,
        oppfolgingsplanLPSBistandUbehandlet = this.oppfolgingsplanLPSBistandUbehandlet,
        latestOppfolgingstilfelle = this.toPersonOppfolgingstilfelle(),
        oppfolgingstilfeller = oppfolgingstilfeller,
    )
}

fun PPersonOversiktStatus.toPersonOppfolgingstilfelle(): PersonOppfolgingstilfelle? {
    return if (
        this.oppfolgingstilfelleUpdatedAt != null &&
        this.oppfolgingstilfelleGeneratedAt != null &&
        this.oppfolgingstilfelleStart != null &&
        this.oppfolgingstilfelleEnd != null &&
        this.oppfolgingstilfelleBitReferanseInntruffet != null &&
        this.oppfolgingstilfelleBitReferanseUuid != null
    ) {
        PersonOppfolgingstilfelle(
            oppfolgingstilfelleUpdatedAt = this.oppfolgingstilfelleUpdatedAt,
            oppfolgingstilfelleGeneratedAt = this.oppfolgingstilfelleGeneratedAt,
            oppfolgingstilfelleStart = this.oppfolgingstilfelleStart,
            oppfolgingstilfelleEnd = this.oppfolgingstilfelleEnd,
            oppfolgingstilfelleBitReferanseInntruffet = this.oppfolgingstilfelleBitReferanseInntruffet,
            oppfolgingstilfelleBitReferanseUuid = this.oppfolgingstilfelleBitReferanseUuid,
            virksomhetList = emptyList()
        )
    } else {
        null
    }
}
