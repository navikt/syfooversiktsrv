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
)

fun PPersonOversiktStatus.toPersonOversiktStatus(
    personOppfolgingstilfelleVirksomhetList: List<PersonOppfolgingstilfelleVirksomhet>,
) = PersonOversiktStatus(
    fnr = this.fnr,
    navn = this.navn,
    enhet = this.enhet,
    veilederIdent = this.veilederIdent,
    motebehovUbehandlet = this.motebehovUbehandlet,
    moteplanleggerUbehandlet = this.moteplanleggerUbehandlet,
    oppfolgingsplanLPSBistandUbehandlet = this.oppfolgingsplanLPSBistandUbehandlet,
    dialogmotesvarUbehandlet = this.dialogmotesvarUbehandlet,
    dialogmotekandidat = this.dialogmotekandidat,
    dialogmotekandidatGeneratedAt = this.dialogmotekandidatGeneratedAt,
    motestatus = this.motestatus,
    motestatusGeneratedAt = this.motestatusGeneratedAt,
    latestOppfolgingstilfelle = this.toPersonOppfolgingstilfelle(
        personOppfolgingstilfelleVirksomhetList = personOppfolgingstilfelleVirksomhetList,
    ),
)

fun PPersonOversiktStatus.toPersonOppfolgingstilfelle(
    personOppfolgingstilfelleVirksomhetList: List<PersonOppfolgingstilfelleVirksomhet>,
): PersonOppfolgingstilfelle? {
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
            virksomhetList = personOppfolgingstilfelleVirksomhetList,
        )
    } else {
        null
    }
}
