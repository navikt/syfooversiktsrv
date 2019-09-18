package no.nav.syfo.personstatus.domain

data class PersonOversiktStatusInternal(
        val veilederIdent: String?,
        val fnr: String,
        val id: Int,
        val enhet: String,
        val motebehovUbehandlet: Boolean?,
        val moteplanleggerUbehandlet: Boolean?
)
