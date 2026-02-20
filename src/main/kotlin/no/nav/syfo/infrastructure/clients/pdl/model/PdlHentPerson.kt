package no.nav.syfo.infrastructure.clients.pdl.model

data class PdlHentPersonRequest(
    val query: String,
    val variables: PdlHentPersonRequestVariables,
)

data class PdlHentPersonRequestVariables(
    val ident: String,
)

data class PdlHentPersonResponse(
    val errors: List<PdlError>?,
    val data: PdlHentPerson?,
)

data class PdlHentPerson(
    val hentPerson: PdlPerson?,
)
