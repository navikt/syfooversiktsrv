package no.nav.syfo.testutil.mock

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import no.nav.syfo.application.api.authentication.installContentNegotiation
import no.nav.syfo.personstatus.application.arbeidsuforhet.ArbeidsuforhetvurderingDTO
import no.nav.syfo.personstatus.application.arbeidsuforhet.VarselDTO
import no.nav.syfo.personstatus.application.arbeidsuforhet.VurderingType
import no.nav.syfo.testutil.UserConstants
import no.nav.syfo.testutil.getRandomPort
import java.time.LocalDate
import java.time.LocalDateTime

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
                when (getPersonIdentHeader()) {
                    UserConstants.ARBEIDSTAKER_FNR ->
                        call.respond(HttpStatusCode.OK, arbeidsuforhetvurdering)
                    else -> call.respond(HttpStatusCode.NotFound, "")
                }
            }
        }
    }
}

val latestVurdering = LocalDateTime.now().minusDays(1)

private val arbeidsuforhetvurdering: List<ArbeidsuforhetvurderingDTO> =
    listOf(
        ArbeidsuforhetvurderingDTO(
            createdAt = LocalDateTime.now().minusDays(3),
            type = VurderingType.FORHANDSVARSEL,
            varsel = VarselDTO(
                svarfrist = LocalDate.now().plusDays(1),
            ),
        ),
        ArbeidsuforhetvurderingDTO(
            createdAt = latestVurdering,
            type = VurderingType.FORHANDSVARSEL,
            varsel = VarselDTO(
                svarfrist = LocalDate.now().plusDays(1),
            ),
        ),
        ArbeidsuforhetvurderingDTO(
            createdAt = LocalDateTime.now().minusDays(5),
            type = VurderingType.FORHANDSVARSEL,
            varsel = VarselDTO(
                svarfrist = LocalDate.now().plusDays(1),
            ),
        )
    )
