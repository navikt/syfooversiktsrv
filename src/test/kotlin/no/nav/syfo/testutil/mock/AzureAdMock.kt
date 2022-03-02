package no.nav.syfo.testutil.mock

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.syfo.application.api.authentication.WellKnown
import no.nav.syfo.application.api.authentication.installContentNegotiation
import no.nav.syfo.client.azuread.AzureAdTokenResponse
import no.nav.syfo.testutil.getRandomPort
import java.nio.file.Paths

fun wellKnownVeilederV2Mock(): WellKnown {
    val path = "src/test/resources/jwkset.json"
    val uri = Paths.get(path).toUri().toURL()
    return WellKnown(
        authorization_endpoint = "authorizationendpoint",
        token_endpoint = "tokenendpoint",
        jwks_uri = uri.toString(),
        issuer = "https://sts.issuer.net/v2"
    )
}

class AzureAdMock {
    private val port = getRandomPort()
    val url = "http://localhost:$port"

    val azureAdTokenResponse = AzureAdTokenResponse(
        access_token = "token",
        expires_in = 3600,
        token_type = "type"
    )

    val name = "azuread"
    val server = embeddedServer(
        factory = Netty,
        port = port,
    ) {
        installContentNegotiation()
        routing {
            post {
                call.respond(azureAdTokenResponse)
            }
        }
    }
}
