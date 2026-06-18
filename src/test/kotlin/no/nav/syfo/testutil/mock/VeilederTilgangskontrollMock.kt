package no.nav.syfo.testutil.mock

import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.syfo.api.auth.getNAVIdentFromToken
import no.nav.syfo.infrastructure.clients.veiledertilgang.Tilgang
import no.nav.syfo.testutil.UserConstants
import no.nav.syfo.util.NAV_PERSONIDENT_HEADER

private fun HttpRequestData.navIdentFromBearer(): String? =
    headers[HttpHeaders.Authorization]
        ?.removePrefix("Bearer ")
        ?.let { token ->
            runCatching { getNAVIdentFromToken(token) }.getOrNull()
        }

suspend fun MockRequestHandleScope.tilgangskontrollResponse(request: HttpRequestData): HttpResponseData {
    val responseAccessPersons = listOf(
        UserConstants.ARBEIDSTAKER_FNR,
        UserConstants.ARBEIDSTAKER_2_FNR,
        UserConstants.ARBEIDSTAKER_NO_NAME_FNR,
    )

    val requestUrl = request.url.encodedPath

    return when {
        requestUrl.endsWith("tilgang/navident/syfo") -> {
            val fullTilgang = request.navIdentFromBearer() != UserConstants.VEILEDER_ID_NO_WRITE_ACCESS
            respondOk(Tilgang(erGodkjent = true, fullTilgang = fullTilgang))
        }
        requestUrl.endsWith("tilgang/navident/person") -> {
            val personident = request.headers[NAV_PERSONIDENT_HEADER]
            val erGodkjent = personident != UserConstants.ARBEIDSTAKER_NO_ACCESS
            val fullTilgang = request.navIdentFromBearer() != UserConstants.VEILEDER_ID_NO_WRITE_ACCESS
            respondOk(Tilgang(erGodkjent = erGodkjent, fullTilgang = fullTilgang))
        }
        requestUrl.endsWith("tilgang/navident/brukere") -> {
            respondOk(responseAccessPersons)
        }
        requestUrl.endsWith("tilgang/system/preloadbrukere") -> {
            val identer = request.receiveBody<List<String>>()
            if (identer.contains(UserConstants.ARBEIDSTAKER_4_FNR_WITH_ERROR)) {
                respondError(status = HttpStatusCode.InternalServerError)
            } else {
                respondOk("")
            }
        }
        requestUrl.endsWith("tilgang/navident/enhet/${UserConstants.NAV_ENHET}") -> {
            respondOk(Tilgang(erGodkjent = true, fullTilgang = true))
        }
        else -> error("Unhandled path $requestUrl")
    }
}
