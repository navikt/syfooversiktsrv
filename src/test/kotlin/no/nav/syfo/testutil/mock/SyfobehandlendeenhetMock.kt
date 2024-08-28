package no.nav.syfo.testutil.mock

import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.syfo.personstatus.infrastructure.clients.behandlendeenhet.BehandlendeEnhetDTO
import no.nav.syfo.testutil.UserConstants
import no.nav.syfo.util.NAV_PERSONIDENT_HEADER

fun behandlendeEnhetDTO() =
    BehandlendeEnhetDTO(
        enhetId = UserConstants.NAV_ENHET,
        navn = "Navkontor",
    )

fun MockRequestHandleScope.getBehandlendeEnhetResponse(request: HttpRequestData): HttpResponseData {
    val personident = request.headers[NAV_PERSONIDENT_HEADER]
    return when (personident) {
        UserConstants.ARBEIDSTAKER_ENHET_ERROR_PERSONIDENT.value -> respondError(status = HttpStatusCode.InternalServerError)
        UserConstants.ARBEIDSTAKER_ENHET_NOT_FOUND_PERSONIDENT.value -> {
            respond(
                status = HttpStatusCode.NoContent,
                content = "",
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        else -> {
            respondOk(behandlendeEnhetDTO())
        }
    }
}
