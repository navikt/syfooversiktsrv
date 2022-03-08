package no.nav.syfo.personstatus.domain

data class PPersonOversiktStatus(
    val veilederIdent: String?,
    val fnr: String,
    val navn: String?,
    val id: Int,
    val enhet: String,
    val motebehovUbehandlet: Boolean?,
    val moteplanleggerUbehandlet: Boolean?,
    val oppfolgingsplanLPSBistandUbehandlet: Boolean?,
)

fun PPersonOversiktStatus.toPersonOversiktStatus(
    oppfolgingstilfeller: List<Oppfolgingstilfelle>,
) = PersonOversiktStatus(
    fnr = this.fnr,
    navn = this.navn,
    enhet = this.enhet,
    veilederIdent = this.veilederIdent,
    motebehovUbehandlet = this.motebehovUbehandlet,
    moteplanleggerUbehandlet = this.moteplanleggerUbehandlet,
    oppfolgingsplanLPSBistandUbehandlet = this.oppfolgingsplanLPSBistandUbehandlet,
    oppfolgingstilfeller = oppfolgingstilfeller,
)
