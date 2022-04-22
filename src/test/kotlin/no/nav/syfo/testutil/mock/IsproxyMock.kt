package no.nav.syfo.testutil.mock

import io.ktor.server.application.*
import io.ktor.http.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.syfo.application.api.authentication.installContentNegotiation
import no.nav.syfo.client.ereg.EregClient.Companion.EREG_PATH
import no.nav.syfo.client.ereg.EregOrganisasjonNavn
import no.nav.syfo.client.ereg.EregOrganisasjonResponse
import no.nav.syfo.testutil.UserConstants.VIRKSOMHETSNUMMER_2
import no.nav.syfo.testutil.UserConstants.VIRKSOMHETSNUMMER_DEFAULT
import no.nav.syfo.testutil.UserConstants.VIRKSOMHETSNUMMER_NO_VIRKSOMHETSNAVN
import no.nav.syfo.testutil.getRandomPort

val eregOrganisasjonResponse = EregOrganisasjonResponse(
    navn = EregOrganisasjonNavn(
        navnelinje1 = "Virksom Bedrift AS",
        redigertnavn = "Virksom Bedrift AS, Norge",
    )
)

class IsproxyMock {
    private val port = getRandomPort()
    val url = "http://localhost:$port"

    val name = "isproxy"
    val server = embeddedServer(
        factory = Netty,
        port = port,
    ) {
        installContentNegotiation()
        routing {
            get("$EREG_PATH/${VIRKSOMHETSNUMMER_DEFAULT.value}") {
                call.respond(eregOrganisasjonResponse)
            }
            get("$EREG_PATH/$VIRKSOMHETSNUMMER_2") {
                call.respond(eregOrganisasjonResponse)
            }
            get("$EREG_PATH/${VIRKSOMHETSNUMMER_NO_VIRKSOMHETSNAVN.value}") {
                call.respond(HttpStatusCode.InternalServerError, "")
            }
        }
    }
}
