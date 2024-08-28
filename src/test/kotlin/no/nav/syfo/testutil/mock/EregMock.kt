package no.nav.syfo.testutil.mock

import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.syfo.personstatus.infrastructure.clients.ereg.EregOrganisasjonNavn
import no.nav.syfo.personstatus.infrastructure.clients.ereg.EregOrganisasjonResponse
import no.nav.syfo.testutil.UserConstants.VIRKSOMHETSNUMMER_2
import no.nav.syfo.testutil.UserConstants.VIRKSOMHETSNUMMER_DEFAULT
import no.nav.syfo.testutil.UserConstants.VIRKSOMHETSNUMMER_NO_VIRKSOMHETSNAVN

val eregOrganisasjonResponse = EregOrganisasjonResponse(
    navn = EregOrganisasjonNavn(
        navnelinje1 = "Virksom Bedrift AS",
        redigertnavn = "Virksom Bedrift AS, Norge",
    )
)

fun MockRequestHandleScope.eregMockResponse(request: HttpRequestData): HttpResponseData {
    val requestUrl = request.url.encodedPath

    return when {
        requestUrl.endsWith(VIRKSOMHETSNUMMER_DEFAULT.value) -> respondOk(eregOrganisasjonResponse)
        requestUrl.endsWith(VIRKSOMHETSNUMMER_2) -> respondOk(eregOrganisasjonResponse)
        requestUrl.endsWith(VIRKSOMHETSNUMMER_NO_VIRKSOMHETSNAVN.value) -> respondError(status = HttpStatusCode.InternalServerError)
        else -> error("Unhandled path $requestUrl")
    }
}
