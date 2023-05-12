package no.nav.syfo.testutil.mock

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.syfo.application.api.authentication.installContentNegotiation
import no.nav.syfo.client.veiledertilgang.Tilgang
import no.nav.syfo.testutil.UserConstants
import no.nav.syfo.testutil.getRandomPort

class VeilederTilgangskontrollMock {
    private val port = getRandomPort()
    val url = "http://localhost:$port"

    val responseAccessEnhet = Tilgang(harTilgang = true)
    val responseAccessPersons = listOf(
        UserConstants.ARBEIDSTAKER_FNR,
        UserConstants.ARBEIDSTAKER_2_FNR,
        UserConstants.ARBEIDSTAKER_NO_NAME_FNR,
    )

    val name = "veiledertilgangskontroll"
    val server = embeddedServer(
        factory = Netty,
        port = port,
    ) {
        installContentNegotiation()
        routing {
            get("/syfo-tilgangskontroll/api/tilgang/navident/enhet/${UserConstants.NAV_ENHET}") {
                call.respond(responseAccessEnhet)
            }
            post("/syfo-tilgangskontroll/api/tilgang/navident/brukere") {
                call.respond(responseAccessPersons)
            }
            post("/syfo-tilgangskontroll/api/tilgang/system/preloadbrukere") {
                val identer = call.receive<List<String>>()
                call.respond(
                    if (identer.contains(UserConstants.ARBEIDSTAKER_4_FNR_WITH_ERROR)) {
                        HttpStatusCode.InternalServerError
                    } else {
                        HttpStatusCode.OK
                    }
                )
            }
        }
    }
}
