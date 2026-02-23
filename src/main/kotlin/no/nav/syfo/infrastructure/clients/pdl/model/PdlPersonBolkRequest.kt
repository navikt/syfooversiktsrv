package no.nav.syfo.infrastructure.clients.pdl.model

data class PdlPersonBolkRequest(
    val query: String,
    val variables: PdlPersonBolkVariables,
)

data class PdlPersonBolkVariables(
    val identer: List<String>,
)
