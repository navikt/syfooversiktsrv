package no.nav.syfo.personstatus.domain

data class PersonOversiktStatus(
        val veilederIdent: String?,
        val fnr: String,
        val enhet: String,
        val motebehovUbehandlet: Boolean?
)