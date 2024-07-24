package no.nav.syfo.personstatus.infrastructure.clients.veiledertilgang

import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.micrometer.core.instrument.Timer
import no.nav.syfo.personstatus.domain.PersonIdent
import no.nav.syfo.personstatus.infrastructure.COUNT_CALL_TILGANGSKONTROLL_PERSONS_FAIL
import no.nav.syfo.personstatus.infrastructure.COUNT_CALL_TILGANGSKONTROLL_PERSONS_SUCCESS
import no.nav.syfo.personstatus.infrastructure.HISTOGRAM_ISTILGANGSKONTROLL_ENHET
import no.nav.syfo.personstatus.infrastructure.HISTOGRAM_ISTILGANGSKONTROLL_PERSONER
import no.nav.syfo.personstatus.infrastructure.clients.ClientEnvironment
import no.nav.syfo.personstatus.infrastructure.clients.azuread.AzureAdClient
import no.nav.syfo.personstatus.infrastructure.clients.httpClientDefault
import no.nav.syfo.util.NAV_CALL_ID_HEADER
import no.nav.syfo.util.NAV_PERSONIDENT_HEADER
import no.nav.syfo.util.bearerHeader
import org.slf4j.LoggerFactory
import java.util.UUID

class VeilederTilgangskontrollClient(
    private val azureAdClient: AzureAdClient,
    private val istilgangskontrollEnv: ClientEnvironment,
) {
    private val httpClient = httpClientDefault()

    private val pathTilgangTilBrukereOBO = "/navident/brukere"
    private val pathPreloadCache = "/system/preloadbrukere"
    private val pathTilgangTilEnhetOBO = "/navident/enhet"

    suspend fun getVeilederAccessToPerson(
        personident: PersonIdent,
        token: String,
        callId: String,
    ): Tilgang? {
        val oboToken = azureAdClient.getOnBehalfOfToken(
            scopeClientId = istilgangskontrollEnv.clientId,
            token = token
        )?.accessToken ?: throw RuntimeException("Failed to request access to person: Failed to get OBO token")
        try {
            val url = getTilgangskontrollUrl("/navident/person")
            val response: HttpResponse = httpClient.get(url) {
                header(HttpHeaders.Authorization, bearerHeader(oboToken))
                header(NAV_CALL_ID_HEADER, callId)
                header(NAV_PERSONIDENT_HEADER, personident.value)
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
            }

            return response.body<Tilgang>()
        } catch (e: ClientRequestException) {
            return if (e.response.status == HttpStatusCode.Forbidden) {
                log.warn("Forbidden to request access to person from istilgangskontroll")
                null
            } else {
                log.error("Error while requesting access to person from istilgangskontroll: ${e.message}", e)
                null
            }
        } catch (e: ServerResponseException) {
            log.error("Error while requesting access to person from istilgangskontroll: ${e.message}", e)
            return null
        }
    }

    suspend fun veilederPersonAccessListMedOBO(
        personIdentNumberList: List<String>,
        token: String,
        callId: String,
    ): List<String>? {
        val oboToken = azureAdClient.getOnBehalfOfToken(
            scopeClientId = istilgangskontrollEnv.clientId,
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
                setBody(personIdentNumberList)
            }

            requestTimer.stop(HISTOGRAM_ISTILGANGSKONTROLL_PERSONER)
            COUNT_CALL_TILGANGSKONTROLL_PERSONS_SUCCESS.increment()

            return response.body<List<String>>()
        } catch (e: ClientRequestException) {
            return if (e.response.status == HttpStatusCode.Forbidden) {
                log.warn("Forbidden to request access to list of person from istilgangskontroll")
                null
            } else {
                COUNT_CALL_TILGANGSKONTROLL_PERSONS_FAIL.increment()
                log.error("Error while requesting access to list of person from istilgangskontroll: ${e.message}", e)
                null
            }
        } catch (e: ServerResponseException) {
            COUNT_CALL_TILGANGSKONTROLL_PERSONS_FAIL.increment()
            log.error("Error while requesting access to list of person from istilgangskontroll: ${e.message}", e)
            return null
        }
    }

    suspend fun preloadCache(
        personIdentNumberList: List<String>,
    ): Boolean {
        val systemToken = azureAdClient.getSystemToken(
            scopeClientId = istilgangskontrollEnv.clientId,
        )?.accessToken
            ?: throw RuntimeException("Failed to request preload of list of persons: Failed to get system token")

        return try {
            val response = httpClient.post(getTilgangskontrollUrl(pathPreloadCache)) {
                header(HttpHeaders.Authorization, bearerHeader(systemToken))
                header(NAV_CALL_ID_HEADER, UUID.randomUUID().toString())
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                setBody(personIdentNumberList)
            }
            HttpStatusCode.OK == response.status
        } catch (e: ClientRequestException) {
            if (e.response.status == HttpStatusCode.Forbidden) {
                log.warn("Forbidden to request preload of list of person from istilgangskontroll")
            } else {
                log.error("Error while requesting preload of list of person from istilgangskontroll: ${e.message}", e)
            }
            false
        } catch (e: ServerResponseException) {
            log.error("Error while requesting preload of list of person from istilgangskontroll: ${e.message}", e)
            false
        }
    }

    suspend fun harVeilederTilgangTilEnhetMedOBO(
        enhet: String,
        token: String,
        callId: String,
    ): Boolean {
        val oboToken = azureAdClient.getOnBehalfOfToken(
            scopeClientId = istilgangskontrollEnv.clientId,
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
            requestTimer.stop(HISTOGRAM_ISTILGANGSKONTROLL_ENHET)
            return response.body<Tilgang>().erGodkjent
        } catch (e: ClientRequestException) {
            return if (e.response.status == HttpStatusCode.Forbidden) {
                false
            } else {
                return false
            }
        } catch (e: ServerResponseException) {
            log.error("Failed to get access to enhet from istilgangskontroll. requested enhet: $enhet", e)
            return false
        }
    }

    private fun getTilgangskontrollUrl(path: String): String {
        return "${istilgangskontrollEnv.baseUrl}/api/tilgang$path"
    }

    companion object {
        private val log = LoggerFactory.getLogger(VeilederTilgangskontrollClient::class.java)
    }
}
