package no.nav.syfo.testutil.mock

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.syfo.application.api.authentication.installContentNegotiation
import no.nav.syfo.client.pdl.domain.*
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_NO_NAME_FNR
import no.nav.syfo.testutil.getRandomPort

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

class PdlMock {
    private val port = getRandomPort()
    val url = "http://localhost:$port"
    val name = "pdl"

    val server = embeddedServer(
        factory = Netty,
        port = port,
    ) {
        installContentNegotiation()
        routing {
            post {
                val pdlRequest = call.receive<PdlPersonBolkRequest>()
                call.respond(
                    generatePdlPersonResponse(
                        identList = pdlRequest.variables.identer,
                    )
                )
            }
        }
    }
}
