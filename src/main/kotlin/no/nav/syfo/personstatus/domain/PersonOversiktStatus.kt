package no.nav.syfo.personstatus.domain

data class PersonOversiktStatus(
        val veilederIdent: String?,
        val fnr: String,
        val navn: String,
        val enhet: String,
        val motebehovUbehandlet: Boolean?,
        val moteplanleggerUbehandlet: Boolean?,
        val oppfolgingstilfeller: List<Oppfolgingstilfelle>
)
