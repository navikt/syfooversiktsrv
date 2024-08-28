package no.nav.syfo.testutil.mock

import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.syfo.personstatus.infrastructure.clients.veiledertilgang.Tilgang
import no.nav.syfo.testutil.UserConstants

suspend fun MockRequestHandleScope.tilgangskontrollResponse(request: HttpRequestData): HttpResponseData {
    val responseAccess = Tilgang(erGodkjent = true)
    val responseAccessPersons = listOf(
        UserConstants.ARBEIDSTAKER_FNR,
        UserConstants.ARBEIDSTAKER_2_FNR,
        UserConstants.ARBEIDSTAKER_NO_NAME_FNR,
    )

    val requestUrl = request.url.encodedPath

    return when {
        requestUrl.endsWith("tilgang/navident/person") -> {
            respondOk(responseAccess)
        }
        requestUrl.endsWith("tilgang/navident/brukere") -> {
            respondOk(responseAccessPersons)
        }
        requestUrl.endsWith("tilgang/system/preloadbrukere") -> {
            val identer = request.receiveBody<List<String>>()
            if (identer.contains(UserConstants.ARBEIDSTAKER_4_FNR_WITH_ERROR)) {
                return respondError(status = HttpStatusCode.InternalServerError)
            } else {
                return respondOk("")
            }
        }
        requestUrl.endsWith("tilgang/navident/enhet/${UserConstants.NAV_ENHET}") -> {
            respondOk(responseAccess)
        }
        else -> error("Unhandled path $requestUrl")
    }
}
