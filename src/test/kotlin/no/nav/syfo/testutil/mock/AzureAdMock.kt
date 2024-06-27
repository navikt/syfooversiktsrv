package no.nav.syfo.testutil.mock

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.syfo.personstatus.api.v2.auth.WellKnown
import no.nav.syfo.personstatus.api.v2.auth.getNAVIdentFromToken
import no.nav.syfo.personstatus.api.v2.auth.installContentNegotiation
import no.nav.syfo.personstatus.infrastructure.clients.azuread.AzureAdTokenResponse
import no.nav.syfo.testutil.UserConstants.VEILEDER_IDENT_NO_AZURE_AD_TOKEN
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
                val parameters = call.receive<Parameters>()
                val token = parameters["assertion"]?.takeIf { it.isNotEmpty() }
                val veilederIdent = token?.let { getNAVIdentFromToken(it) }
                when (veilederIdent) {
                    VEILEDER_IDENT_NO_AZURE_AD_TOKEN -> call.respond(HttpStatusCode.NotFound)
                    else -> call.respond(azureAdTokenResponse)
                }
            }
        }
    }
}
