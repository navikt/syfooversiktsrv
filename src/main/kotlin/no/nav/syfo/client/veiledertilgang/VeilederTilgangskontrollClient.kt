package no.nav.syfo.client.veiledertilgang

import io.ktor.client.call.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.micrometer.core.instrument.Timer
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.client.httpClientDefault
import no.nav.syfo.metric.*
import no.nav.syfo.util.NAV_CALL_ID_HEADER
import no.nav.syfo.util.bearerHeader
import org.slf4j.LoggerFactory

class VeilederTilgangskontrollClient(
    private val endpointUrl: String,
    private val azureAdClient: AzureAdClient,
    private val syfotilgangskontrollClientId: String
) {
    private val httpClient = httpClientDefault()

    private val pathTilgangTilBrukereOBO = "/navident/brukere"
    private val pathTilgangTilEnhetOBO = "/navident/enhet"

    suspend fun veilederPersonAccessListMedOBO(
        personIdentNumberList: List<String>,
        token: String,
        callId: String
    ): List<String>? {
        val oboToken = azureAdClient.getOnBehalfOfToken(
            scopeClientId = syfotilgangskontrollClientId,
            token = token
        )?.accessToken
            ?: throw RuntimeException("Failed to request access to list of persons: Failed to get OBO token")

        try {
            val requestTimer: Timer.Sample = Timer.start()

            val url = getTilgangskontrollUrl(pathTilgangTilBrukereOBO)
            val response: HttpResponse = httpClient.post(url) {
                header(HttpHeaders.Authorization, bearerHeader(oboToken))
                header(NAV_CALL_ID_HEADER, callId)
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                body = personIdentNumberList
            }

            requestTimer.stop(HISTOGRAM_SYFOTILGANGSKONTROLL_PERSONER)
            COUNT_CALL_TILGANGSKONTROLL_PERSONS_SUCCESS.increment()
            return response.receive()
        } catch (e: ClientRequestException) {
            return if (e.response.status == HttpStatusCode.Forbidden) {
                log.warn("Forbidden to request access to list of person from syfo-tilgangskontroll")
                null
            } else {
                COUNT_CALL_TILGANGSKONTROLL_PERSONS_FAIL.increment()
                log.error("Error while requesting access to list of person from syfo-tilgangskontroll: ${e.message}", e)
                null
            }
        } catch (e: ServerResponseException) {
            COUNT_CALL_TILGANGSKONTROLL_PERSONS_FAIL.increment()
            log.error("Error while requesting access to list of person from syfo-tilgangskontroll: ${e.message}", e)
            return null
        }
    }

    suspend fun harVeilederTilgangTilEnhetMedOBO(
        enhet: String,
        token: String,
        callId: String
    ): Boolean {
        val oboToken = azureAdClient.getOnBehalfOfToken(
            scopeClientId = syfotilgangskontrollClientId,
            token = token
        )?.accessToken ?: throw RuntimeException("Failed to request access to Enhet: Failed to get OBO token")

        try {
            val requestTimer: Timer.Sample = Timer.start()
            val url = getTilgangskontrollUrl("$pathTilgangTilEnhetOBO/$enhet")
            val response: HttpResponse = httpClient.get(url) {
                header(HttpHeaders.Authorization, bearerHeader(oboToken))
                header(NAV_CALL_ID_HEADER, callId)
                accept(ContentType.Application.Json)
            }
            requestTimer.stop(HISTOGRAM_SYFOTILGANGSKONTROLL_ENHET)
            return response.receive<Tilgang>().harTilgang
        } catch (e: ClientRequestException) {
            return if (e.response.status == HttpStatusCode.Forbidden) {
                false
            } else {
                return false
            }
        } catch (e: ServerResponseException) {
            return false
        }
    }

    private fun getTilgangskontrollUrl(path: String): String {
        return "$endpointUrl/syfo-tilgangskontroll/api/tilgang$path"
    }

    companion object {
        private val log = LoggerFactory.getLogger(VeilederTilgangskontrollClient::class.java)
    }
}
