package no.nav.syfo.testutil.mock

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.jackson.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
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
        install(ContentNegotiation) {
            jackson {
                registerKotlinModule()
                registerModule(JavaTimeModule())
                configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            }
        }
        routing {
            get("/syfo-tilgangskontroll/api/tilgang/navident/enhet/${UserConstants.NAV_ENHET}") {
                call.respond(responseAccessEnhet)
            }
            post("/syfo-tilgangskontroll/api/tilgang/navident/brukere") {
                call.respond(responseAccessPersons)
            }
        }
    }
}
