package no.nav.syfo.testutil.mock

import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import no.nav.syfo.personstatus.api.v2.auth.WellKnown
import no.nav.syfo.personstatus.api.v2.auth.getNAVIdentFromToken
import no.nav.syfo.personstatus.infrastructure.clients.azuread.AzureAdTokenResponse
import no.nav.syfo.testutil.UserConstants
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

fun MockRequestHandleScope.azureAdMockResponse(request: HttpRequestData): HttpResponseData {
    val token = (request.body as FormDataContent).formData["assertion"]
    val veilederIdent: String? = if (token != null) getNAVIdentFromToken(token) else null

    return when (veilederIdent) {
        UserConstants.VEILEDER_IDENT_NO_AZURE_AD_TOKEN -> respondError(status = HttpStatusCode.NotFound)
        else -> respondOk(
            AzureAdTokenResponse(
                access_token = "token",
                expires_in = 3600,
                token_type = "type",
            )
        )
    }
}
