package no.nav.syfo.testutil.mock

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.syfo.personstatus.api.v2.auth.installContentNegotiation
import no.nav.syfo.personstatus.infrastructure.clients.pdl.model.*
import no.nav.syfo.testutil.UserConstants
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_NO_NAME_FNR
import no.nav.syfo.testutil.getRandomPort
import no.nav.syfo.util.configuredJacksonMapper

fun generatePdlPerson(
    pdlPersonNavn: PdlPersonNavn,
) = PdlPerson(
    navn = listOf(
        pdlPersonNavn,
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

class PdlMock {
    private val port = getRandomPort()
    val url = "http://localhost:$port"
    val name = "pdl"

    private val objectMapper: ObjectMapper = configuredJacksonMapper()

    val server = embeddedServer(
        factory = Netty,
        port = port,
    ) {
        installContentNegotiation()
        routing {
            post {
                val pdlRequest = call.receiveText()
                val isHentIdenter = pdlRequest.contains("hentIdenter")
                if (isHentIdenter) {
                    val request: PdlIdentRequest = objectMapper.readValue(pdlRequest)
                    if (request.variables.ident == UserConstants.ARBEIDSTAKER_3_FNR) {
                        call.respond(generatePdlIdenter("enAnnenIdent"))
                    } else if (request.variables.ident == UserConstants.ARBEIDSTAKER_4_FNR_WITH_ERROR) {
                        call.respond(
                            generatePdlIdenter(request.variables.ident)
                                .copy(errors = generatePdlError(code = "not_found"))
                        )
                    } else {
                        call.respond(generatePdlIdenter(request.variables.ident))
                    }
                } else {
                    val request: PdlPersonBolkRequest = objectMapper.readValue(pdlRequest)
                    call.respond(
                        generatePdlPersonResponse(
                            identList = request.variables.identer,
                        )
                    )
                }
            }
        }
    }
}
