package no.nav.syfo.testutil.mock

import io.ktor.server.application.call
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import no.nav.syfo.personstatus.api.v2.auth.installContentNegotiation
import no.nav.syfo.personstatus.application.manglendemedvirkning.ManglendeMedvirkningDTO
import no.nav.syfo.personstatus.application.manglendemedvirkning.ManglendeMedvirkningResponseDTO
import no.nav.syfo.personstatus.application.manglendemedvirkning.ManglendeMedvirkningVarselDTO
import no.nav.syfo.personstatus.application.manglendemedvirkning.ManglendeMedvirkningVurderingType
import no.nav.syfo.personstatus.infrastructure.clients.arbeidsuforhet.ManglendeMedvirkningClient
import no.nav.syfo.testutil.UserConstants
import no.nav.syfo.testutil.getRandomPort
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class ManglendeMedvirkningMock {
    private val port = getRandomPort()
    val url = "http://localhost:$port"

    val name = "ismanglendemedvirkning"

    val server = embeddedServer(
        factory = Netty,
        port = port,
    ) {
        installContentNegotiation()
        routing {
            post(ManglendeMedvirkningClient.MANGLENDE_MEDVIRKNING_API_PATH) {
                call.respond(
                    ManglendeMedvirkningResponseDTO(
                        mapOf(
                            Pair(
                                UserConstants.ARBEIDSTAKER_FNR,
                                ManglendeMedvirkningDTO(
                                    uuid = UUID.randomUUID(),
                                    personident = UserConstants.ARBEIDSTAKER_FNR,
                                    createdAt = LocalDateTime.now().minusDays(1),
                                    type = ManglendeMedvirkningVurderingType.FORHANDSVARSEL,
                                    begrunnelse = "begrunnelse",
                                    varsel = ManglendeMedvirkningVarselDTO(
                                        uuid = UUID.randomUUID(),
                                        createdAt = LocalDateTime.now().minusDays(1),
                                        svarfrist = LocalDate.now().plusDays(1),
                                    )
                                ),
                            ),
                        ),
                    ),
                )
            }
        }
    }
}
