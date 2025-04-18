package no.nav.syfo.testutil.mock

import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import no.nav.syfo.personstatus.infrastructure.clients.pdl.model.*
import no.nav.syfo.testutil.UserConstants
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_NO_FODSELSDATO
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_NO_NAME_FNR
import java.time.LocalDate

fun generatePdlPerson(
    pdlPersonNavn: PdlPersonNavn,
    fodselsdato: LocalDate?,
) = PdlPerson(
    navn = listOf(
        pdlPersonNavn,
    ),
    foedselsdato = fodselsdato?.let { listOf(Foedselsdato(it)) } ?: emptyList(),
)

fun generatePdlPersonNavn(
    ident: String,
) = PdlPersonNavn(
    fornavn = "Fornavn$ident",
    mellomnavn = "Mellomnavn$ident",
    etternavn = "Etternavn$ident",
)

fun generatePdlHentPersonBolk(
    ident: String = ARBEIDSTAKER_FNR,
    pdlPersonNavn: PdlPersonNavn = generatePdlPersonNavn(ident = ident),
) = PdlHentPersonBolkResult(
    ident = ident,
    person = generatePdlPerson(
        pdlPersonNavn = pdlPersonNavn,
        fodselsdato = if (ident == ARBEIDSTAKER_NO_FODSELSDATO) null else LocalDate.now().minusYears(30),
    ),
    code = "ok",
)

fun generatePdlHentPerson(
    ident: String = ARBEIDSTAKER_FNR,
    pdlPersonNavn: PdlPersonNavn = generatePdlPersonNavn(ident = ident),
) = PdlHentPerson(
    hentPerson = generatePdlPerson(
        pdlPersonNavn = pdlPersonNavn,
        fodselsdato = if (ident == ARBEIDSTAKER_NO_FODSELSDATO) null else LocalDate.now().minusYears(30),
    ),
)

fun generatePdlHentPersonBolkData(
    identList: List<String>,
) = PdlHentPersonBolkData(
    hentPersonBolk = identList.filter { ident ->
        ident != ARBEIDSTAKER_NO_NAME_FNR
    }.map { ident ->
        generatePdlHentPersonBolk(ident = ident)
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

fun generatePdlHentPersonResponse(ident: String) = PdlHentPersonResponse(
    errors = null,
    data = generatePdlHentPerson(ident = ident),
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
    val isHentPersonBolkRequest = request.receiveBody<Any>().toString().contains("hentPersonBolk")
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
    } else if (isHentPersonBolkRequest) {
        val pdlRequest = request.receiveBody<PdlPersonBolkRequest>()
        respondOk(generatePdlPersonResponse(identList = pdlRequest.variables.identer))
    } else {
        val pdlRequest = request.receiveBody<PdlHentPersonRequest>()
        respondOk(generatePdlHentPersonResponse(ident = pdlRequest.variables.ident))
    }
}
