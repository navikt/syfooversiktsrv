package no.nav.syfo.infrastructure.clients.pdl.model

data class PdlIdentRequest(
    val query: String,
    val variables: PdlIdentVariables,
)

data class PdlIdentVariables(
    val ident: String,
    val navnHistorikk: Boolean = false,
)
