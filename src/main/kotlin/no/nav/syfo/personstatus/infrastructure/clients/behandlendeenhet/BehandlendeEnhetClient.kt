package no.nav.syfo.personstatus.infrastructure.clients.behandlendeenhet

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.personstatus.infrastructure.clients.ClientEnvironment
import no.nav.syfo.personstatus.infrastructure.clients.azuread.AzureAdClient
import no.nav.syfo.personstatus.infrastructure.clients.httpClientDefault
import no.nav.syfo.personstatus.domain.PersonIdent
import no.nav.syfo.util.*
import org.slf4j.LoggerFactory

class BehandlendeEnhetClient(
    private val azureAdClient: AzureAdClient,
    private val clientEnvironment: ClientEnvironment,
    private val httpClient: HttpClient = httpClientDefault(),
) {
    private val behandlendeEnhetUrl = "${clientEnvironment.baseUrl}$BEHANDLENDEENHET_PATH"

    suspend fun getEnhet(
        callId: String,
        personIdent: PersonIdent,
    ): BehandlendeEnhetResponseDTO? {
        val url = behandlendeEnhetUrl
        val systemToken = azureAdClient.getSystemToken(
            scopeClientId = clientEnvironment.clientId,
        )?.accessToken ?: throw RuntimeException("Failed to request access to Enhet: Failed to get token")
        return try {
            val response: HttpResponse = httpClient.get(url) {
                header(HttpHeaders.Authorization, bearerHeader(systemToken))
                header(NAV_CALL_ID_HEADER, callId)
                header(NAV_PERSONIDENT_HEADER, personIdent.value)
                accept(ContentType.Application.Json)
            }
            if (response.status == HttpStatusCode.NoContent) {
                return null
            } else {
                COUNT_CALL_BEHANDLENDEENHET_SUCCESS.increment()
                response.body()
            }
        } catch (e: ClientRequestException) {
            handleUnexpectedResponseException(e.response, callId)
            throw e
        } catch (e: ServerResponseException) {
            handleUnexpectedResponseException(e.response, callId)
            throw e
        }
    }

    suspend fun unsetOppfolgingsenhet(
        callId: String,
        personIdent: PersonIdent,
    ) {
        val url = behandlendeEnhetUrl
        val systemToken = azureAdClient.getSystemToken(
            scopeClientId = clientEnvironment.clientId,
        )?.accessToken ?: throw RuntimeException("Failed to unset Enhet: Failed to get token")
        try {
            httpClient.post(url) {
                header(HttpHeaders.Authorization, bearerHeader(systemToken))
                header(NAV_CALL_ID_HEADER, callId)
                header(NAV_PERSONIDENT_HEADER, personIdent.value)
                accept(ContentType.Application.Json)
            }
        } catch (e: ClientRequestException) {
            handleUnexpectedResponseException(e.response, callId)
            throw e
        } catch (e: ServerResponseException) {
            handleUnexpectedResponseException(e.response, callId)
            throw e
        }
    }

    private fun handleUnexpectedResponseException(
        response: HttpResponse,
        callId: String,
    ) {
        log.error(
            "Error while requesting BehandlendeEnhet of person from Syfobehandlendeenhet with {}, {}",
            StructuredArguments.keyValue("statusCode", response.status.value.toString()),
            callIdArgument(callId)
        )
        COUNT_CALL_BEHANDLENDEENHET_FAIL.increment()
    }

    companion object {
        const val BEHANDLENDEENHET_PATH = "/api/system/v2/personident"
        private val log = LoggerFactory.getLogger(BehandlendeEnhetClient::class.java)
    }
}
