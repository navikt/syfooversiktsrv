package no.nav.syfo.testutil.mock

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import no.nav.syfo.personstatus.api.v2.auth.installContentNegotiation
import no.nav.syfo.personstatus.application.oppfolgingsoppgave.OppfolgingsoppgaveDTO
import no.nav.syfo.personstatus.application.oppfolgingsoppgave.OppfolgingsoppgaverResponseDTO
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_2_FNR
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_3_FNR
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testutil.UserConstants.VEILEDER_ID
import no.nav.syfo.testutil.getRandomPort
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class OppfolgingsoppgaveMock {
    private val port = getRandomPort()
    val url = "http://localhost:$port"

    val name = "oppfolgingsoppgave"
    val server = embeddedServer(
        factory = Netty,
        port = port,
    ) {
        installContentNegotiation()
        routing {
            post("/api/internad/v1/huskelapp/get-oppfolgingsoppgaver") {
                call.respond(HttpStatusCode.OK, oppfolgingsoppgaverResponseDTO)
            }
        }
    }
}

private val oppfolgingsoppgaverResponseDTO = OppfolgingsoppgaverResponseDTO(
    oppfolgingsoppgaver = mapOf(
        ARBEIDSTAKER_FNR to generateOppfolgingsoppgave("FOLG_OPP_ETTER_NESTE_SYKMELDING"),
        ARBEIDSTAKER_2_FNR to generateOppfolgingsoppgave("VURDER_ANNEN_YTELSE"),
        ARBEIDSTAKER_3_FNR to generateOppfolgingsoppgave("VURDER_14A"),
    )
)

private fun generateOppfolgingsoppgave(
    oppfolgingsgrunn: String,
): OppfolgingsoppgaveDTO = OppfolgingsoppgaveDTO(
    uuid = UUID.randomUUID().toString(),
    createdBy = VEILEDER_ID,
    updatedAt = LocalDateTime.now(),
    createdAt = LocalDateTime.now(),
    tekst = "En tekst",
    oppfolgingsgrunn = oppfolgingsgrunn,
    frist = LocalDate.now().plusDays(14),
)
