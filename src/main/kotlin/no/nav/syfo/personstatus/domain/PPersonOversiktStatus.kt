package no.nav.syfo.personstatus.domain

data class PPersonOversiktStatus(
    val veilederIdent: String?,
    val fnr: String,
    val navn: String?,
    val id: Int,
    val enhet: String,
    val motebehovUbehandlet: Boolean?,
    val moteplanleggerUbehandlet: Boolean?
)
