package no.nav.syfo.personstatus.domain

data class VeilederBrukerKnytning(
    val veilederIdent: String,
    val fnr: String,
    val enhet: String
)
