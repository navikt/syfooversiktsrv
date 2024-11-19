package no.nav.syfo.personstatus.infrastructure.clients.pdl.model

import no.nav.syfo.util.lowerCapitalize
import java.time.LocalDate
import kotlin.collections.first
import kotlin.let
import kotlin.text.isNullOrBlank

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
    val foedselsdato: List<Foedselsdato>,
)

data class PdlPersonNavn(
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
)

data class Foedselsdato(
    val foedselsdato: LocalDate?,
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
