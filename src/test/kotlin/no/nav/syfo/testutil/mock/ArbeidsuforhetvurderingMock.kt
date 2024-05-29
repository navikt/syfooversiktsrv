package no.nav.syfo.testutil.mock

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import no.nav.syfo.application.api.authentication.installContentNegotiation
import no.nav.syfo.testutil.getRandomPort

class ArbeidsuforhetvurderingMock {
    private val port = getRandomPort()
    val url = "http://localhost:$port"

    val name = "arbeidsuforhetvurdering"
    val server = embeddedServer(
        factory = Netty,
        port = port,
    ) {
        installContentNegotiation()
        routing {
            get("/api/internad/v1/arbeidsuforhet/vurderinger") {
            }
        }
    }
}
