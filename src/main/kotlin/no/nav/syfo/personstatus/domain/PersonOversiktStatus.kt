package no.nav.syfo.personstatus.domain

data class PersonOversiktStatus(
        val veilederIdent: String?,
        var veileder: Veileder?,
        val fnr: String,
        val enhet: String,
        val motebehovUbehandlet: Boolean?,
        val moteplanleggerUbehandlet: Boolean?
)
