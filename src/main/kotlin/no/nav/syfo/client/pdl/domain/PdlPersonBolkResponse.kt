package no.nav.syfo.client.pdl.domain

import no.nav.syfo.util.lowerCapitalize

data class PdlPersonBolkResponse(
    val data: PdlHentPersonBolkData,
    val errors: List<PdlError>?,
)

data class PdlHentPersonBolkData(
    val hentPersonBolk: List<PdlHentPerson>?,
)

data class PdlHentPerson(
    val ident: String,
    val person: PdlPerson?,
    val code: String,
)

data class PdlPerson(
    val navn: List<PdlPersonNavn>,
)

data class PdlPersonNavn(
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
)

fun PdlPerson.fullName(): String? {
    val nameList = this.navn
    if (nameList.isEmpty()) {
        return null
    }
    nameList.first().let {
        val fornavn = it.fornavn.lowerCapitalize()
        val mellomnavn = it.mellomnavn
        val etternavn = it.etternavn.lowerCapitalize()

        return if (mellomnavn.isNullOrBlank()) {
            "$fornavn $etternavn"
        } else {
            "$fornavn ${mellomnavn.lowerCapitalize()} $etternavn"
        }
    }
}
