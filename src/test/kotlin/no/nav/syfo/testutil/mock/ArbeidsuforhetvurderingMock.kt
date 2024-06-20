package no.nav.syfo.testutil.mock

import io.ktor.server.application.call
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import no.nav.syfo.personstatus.api.v2.auth.installContentNegotiation
import no.nav.syfo.personstatus.application.arbeidsuforhet.*
import no.nav.syfo.testutil.UserConstants
import no.nav.syfo.testutil.getRandomPort
import java.time.LocalDate
import java.time.LocalDateTime

val latestVurdering = LocalDateTime.now().minusDays(1)

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
            post("/api/internad/v1/arbeidsuforhet/get-vurderinger") {
                call.respond(
                    ArbeidsuforhetvurderingerResponseDTO(
                        mapOf(
                            Pair(
                                UserConstants.ARBEIDSTAKER_FNR,
                                ArbeidsuforhetvurderingDTO(
                                    createdAt = latestVurdering,
                                    type = VurderingType.FORHANDSVARSEL,
                                    varsel = VarselDTO(LocalDate.now().plusDays(1))
                                ),
                            ),
                        ),
                    ),
                )
            }
        }
    }
}
