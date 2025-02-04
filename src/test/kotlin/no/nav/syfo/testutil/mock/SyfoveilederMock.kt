package no.nav.syfo.testutil.mock

import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.syfo.personstatus.infrastructure.clients.veileder.VeilederDTO
import no.nav.syfo.testutil.UserConstants

fun MockRequestHandleScope.veilederMockResponse(requestData: HttpRequestData): HttpResponseData {
    val requestUrl = requestData.url.encodedPath

    return when {
        requestUrl.endsWith(UserConstants.VEILEDER_ID) -> respondOk(
            VeilederDTO(enabled = true, ident = UserConstants.VEILEDER_ID)
        )
        requestUrl.endsWith(UserConstants.VEILEDER_ID_2) -> respondError(status = HttpStatusCode.NotFound)
        requestUrl.endsWith(UserConstants.VEILEDER_ID_WITH_ERROR) -> respondError(status = HttpStatusCode.InternalServerError)
        else -> error("Unhandled path $requestUrl")
    }
}
