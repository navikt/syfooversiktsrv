package no.nav.syfo.testutil.mock

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.util.pipeline.*
import no.nav.syfo.application.api.authentication.installContentNegotiation
import no.nav.syfo.client.behandlendeenhet.BehandlendeEnhetClient.Companion.BEHANDLENDEENHET_PATH
import no.nav.syfo.client.behandlendeenhet.BehandlendeEnhetDTO
import no.nav.syfo.testutil.UserConstants
import no.nav.syfo.testutil.getRandomPort
import no.nav.syfo.util.NAV_PERSONIDENT_HEADER

fun behandlendeEnhetDTO() =
    BehandlendeEnhetDTO(
        enhetId = UserConstants.NAV_ENHET,
        navn = "Navkontor",
    )

fun PipelineContext<out Unit, ApplicationCall>.getPersonIdentHeader(): String? {
    return this.call.request.headers[NAV_PERSONIDENT_HEADER]
}

class SyfobehandlendeenhetMock {
    private val port = getRandomPort()
    val url = "http://localhost:$port"

    val name = "behandlendeenhet"
    val server = embeddedServer(
        factory = Netty,
        port = port,
    ) {
        installContentNegotiation()
        routing {
            get(BEHANDLENDEENHET_PATH) {
                if (
                    getPersonIdentHeader() == UserConstants.ARBEIDSTAKER_ENHET_ERROR_PERSONIDENT.value
                ) {
                    call.respond(HttpStatusCode.InternalServerError, "")
                } else if (
                    getPersonIdentHeader() == UserConstants.ARBEIDSTAKER_ENHET_NOT_FOUND_PERSONIDENT.value
                ) {
                    call.respond(HttpStatusCode.NoContent)
                } else {
                    call.respond(behandlendeEnhetDTO())
                }
            }
        }
    }
}
