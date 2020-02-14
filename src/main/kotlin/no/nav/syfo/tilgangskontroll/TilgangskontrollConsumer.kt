package no.nav.syfo.tilgangskontroll

import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.HttpStatement
import io.ktor.http.ContentType
import no.nav.syfo.metric.HISTOGRAM_SYFOTILGANGSKONTROLL_ENHET
import no.nav.syfo.metric.HISTOGRAM_SYFOTILGANGSKONTROLL_PERSON
import no.nav.syfo.util.NAV_CALL_ID_HEADER
import org.slf4j.LoggerFactory

val log = LoggerFactory.getLogger("no.nav.syfo.oversikt.tilgangskontroll")

class TilgangskontrollConsumer(
        private val endpointUrl: String,
        private val client: HttpClient
) {
    private val paramFnr = "fnr"
    private val paramEnhet = "enhet"
    private val pathTilgangTilBruker = "/bruker"
    private val pathTilgangTilEnhet = "/enhet"

    suspend fun harVeilederTilgangTilPerson(fnr: String, token: String, callId: String): Boolean {
        var requestTimer = HISTOGRAM_SYFOTILGANGSKONTROLL_PERSON.startTimer()
        val response = client.get<HttpStatement>(getTilgangskontrollUrl(pathTilgangTilBruker)) {
            accept(ContentType.Application.Json)
            headers {
                append("Authorization", "Bearer $token")
                append(NAV_CALL_ID_HEADER, callId)
            }
            parameter(paramFnr, fnr)
        }.receive<HttpResponse>()
        requestTimer.observeDuration()
        return response.status.value in 200..299
    }

    suspend fun harVeilederTilgangTilEnhet(enhet: String, token: String, callId: String): Boolean {
        var requestTimer = HISTOGRAM_SYFOTILGANGSKONTROLL_ENHET.startTimer()
        val response = client.get<HttpResponse>(getTilgangskontrollUrl(pathTilgangTilEnhet)) {
            accept(ContentType.Application.Json)
            headers {
                append("Authorization", "Bearer $token")
                append(NAV_CALL_ID_HEADER, callId)
            }
            parameter(paramEnhet, enhet)

        }
        requestTimer.observeDuration()
        return response.status.value in 200..299
    }

    private fun getTilgangskontrollUrl(path: String): String {
        return "$endpointUrl/syfo-tilgangskontroll/api/tilgang$path"
    }
}
