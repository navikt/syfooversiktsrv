package no.nav.syfo.personstatus.infrastructure.clients.pdl.model

data class PdlPersonBolkRequest(
    val query: String,
    val variables: PdlPersonBolkVariables,
)

data class PdlPersonBolkVariables(
    val identer: List<String>,
)
