package no.nav.syfo.client.veiledertilgang

import io.ktor.client.call.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.syfo.client.httpClientDefault
import no.nav.syfo.metric.*
import no.nav.syfo.util.NAV_CALL_ID_HEADER
import no.nav.syfo.util.bearerHeader
import org.slf4j.LoggerFactory

class VeilederTilgangskontrollClient(
    private val endpointUrl: String
) {
    private val httpClient = httpClientDefault()

    private val paramEnhet = "enhet"
    private val pathTilgangTilBrukere = "/brukere"
    private val pathTilgangTilEnhet = "/enhet"

    suspend fun veilederPersonAccessList(
        personIdentNumberList: List<String>,
        token: String,
        callId: String
    ): List<String>? {
        try {
            val requestTimer = HISTOGRAM_SYFOTILGANGSKONTROLL_PERSONER.startTimer()

            val response: HttpResponse = httpClient.post(getTilgangskontrollUrl(pathTilgangTilBrukere)) {
                header(HttpHeaders.Authorization, bearerHeader(token))
                header(NAV_CALL_ID_HEADER, callId)
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                body = personIdentNumberList
            }

            requestTimer.observeDuration()
            COUNT_CALL_TILGANGSKONTROLL_PERSONS_SUCCESS.inc()
            return response.receive()
        } catch (e: ClientRequestException) {
            return if (e.response.status == HttpStatusCode.Forbidden) {
                log.warn("Forbidden to request access to list of person from syfo-tilgangskontroll")
                null
            } else {
                COUNT_CALL_TILGANGSKONTROLL_PERSONS_FAIL.inc()
                log.error("Error while requesting access to list of person from syfo-tilgangskontroll: ${e.message}", e)
                null
            }
        } catch (e: ServerResponseException) {
            COUNT_CALL_TILGANGSKONTROLL_PERSONS_FAIL.inc()
            log.error("Error while requesting access to list of person from syfo-tilgangskontroll: ${e.message}", e)
            return null
        }
    }

    suspend fun harVeilederTilgangTilEnhet(
        enhet: String,
        token: String,
        callId: String
    ): Boolean {
        try {
            val requestTimer = HISTOGRAM_SYFOTILGANGSKONTROLL_ENHET.startTimer()
            val url = getTilgangskontrollUrl("$pathTilgangTilEnhet?$paramEnhet=$enhet")
            val response: HttpResponse = httpClient.get(url) {
                header(HttpHeaders.Authorization, bearerHeader(token))
                header(NAV_CALL_ID_HEADER, callId)
                accept(ContentType.Application.Json)
            }
            requestTimer.observeDuration()
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
