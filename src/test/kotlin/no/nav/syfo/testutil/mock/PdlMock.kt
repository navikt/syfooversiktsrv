package no.nav.syfo.testutil.mock

import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import no.nav.syfo.personstatus.infrastructure.clients.pdl.model.*
import no.nav.syfo.testutil.UserConstants
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_NO_NAME_FNR

fun generatePdlPerson(
    pdlPersonNavn: PdlPersonNavn,
) = PdlPerson(
    navn = listOf(
        pdlPersonNavn,
    ),
    foedselsdato = listOf(
        Foedselsdato(foedselsdato = null),
    ),
)

fun generatePdlPersonNavn(
    ident: String,
) = PdlPersonNavn(
    fornavn = "Fornavn$ident",
    mellomnavn = "Mellomnavn$ident",
    etternavn = "Etternavn$ident",
)

fun generatePdlHentPerson(
    ident: String = ARBEIDSTAKER_FNR,
    pdlPersonNavn: PdlPersonNavn = generatePdlPersonNavn(ident = ident),
) = PdlHentPerson(
    ident = ident,
    person = generatePdlPerson(
        pdlPersonNavn = pdlPersonNavn
    ),
    code = "ok",
)

fun generatePdlHentPersonBolkData(
    identList: List<String>,
) = PdlHentPersonBolkData(
    hentPersonBolk = identList.filter { ident ->
        ident != ARBEIDSTAKER_NO_NAME_FNR
    }.map { ident ->
        generatePdlHentPerson(ident = ident)
    }
)

fun generatePdlPersonResponse(
    identList: List<String>,
) = PdlPersonBolkResponse(
    errors = null,
    data = generatePdlHentPersonBolkData(
        identList = identList,
    ),
)

fun generatePdlIdenter(
    personident: String,
) = PdlIdentResponse(
    data = PdlHentIdenter(
        hentIdenter = PdlIdenter(
            identer = listOf(
                PdlIdent(
                    ident = personident,
                    historisk = false,
                    gruppe = IdentGruppe.FOLKEREGISTERIDENT,
                ),
                PdlIdent(
                    ident = personident.toFakeOldIdent(),
                    historisk = true,
                    gruppe = IdentGruppe.FOLKEREGISTERIDENT,
                ),
            ),
        ),
    ),
    errors = null,
)

fun generatePdlError(code: String? = null) = listOf(
    PdlError(
        message = "Error",
        locations = emptyList(),
        path = emptyList(),
        extensions = PdlErrorExtension(
            code = code,
            classification = "Classification",
        )
    )
)

private fun String.toFakeOldIdent(): String {
    val substring = this.drop(1)
    return "9$substring"
}

suspend fun MockRequestHandleScope.pdlMockResponse(request: HttpRequestData): HttpResponseData {
    val isHentIdenterRequest = request.receiveBody<Any>().toString().contains("hentIdenter")
    return if (isHentIdenterRequest) {
        val pdlRequest = request.receiveBody<PdlIdentRequest>()
        when (val personIdent = pdlRequest.variables.ident) {
            UserConstants.ARBEIDSTAKER_3_FNR -> {
                respondOk(generatePdlIdenter("enAnnenIdent"))
            }
            UserConstants.ARBEIDSTAKER_4_FNR_WITH_ERROR -> {
                respondOk(
                    generatePdlIdenter(personIdent)
                        .copy(errors = generatePdlError(code = "not_found"))
                )
            }
            else -> {
                respondOk(generatePdlIdenter(personIdent))
            }
        }
    } else {
        val pdlRequest = request.receiveBody<PdlPersonBolkRequest>()
        respondOk(generatePdlPersonResponse(identList = pdlRequest.variables.identer))
    }
}
