package no.nav.syfo.personstatus.api.v2

data class PersonOversiktStatusDTO(
    val veilederIdent: String?,
    val fnr: String,
    val navn: String,
    val enhet: String,
    val motebehovUbehandlet: Boolean?,
    val moteplanleggerUbehandlet: Boolean?,
    val oppfolgingsplanLPSBistandUbehandlet: Boolean?,
    val oppfolgingstilfeller: List<OppfolgingstilfelleDTO>,
)
