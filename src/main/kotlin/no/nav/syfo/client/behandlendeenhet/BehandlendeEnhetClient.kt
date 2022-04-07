package no.nav.syfo.client.behandlendeenhet

import io.ktor.client.call.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.client.httpClientDefault
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.util.*
import org.slf4j.LoggerFactory

class BehandlendeEnhetClient(
    private val azureAdClient: AzureAdClient,
    private val syfobehandlendeenhetClientId: String,
    baseUrl: String
) {
    private val behandlendeEnhetUrl = "$baseUrl$BEHANDLENDEENHET_PATH"

    private val httpClient = httpClientDefault()

    suspend fun getEnhet(
        callId: String,
        personIdent: PersonIdent,
    ): BehandlendeEnhetDTO? {
        val url = behandlendeEnhetUrl
        val oboToken = azureAdClient.getSystemToken(
            scopeClientId = syfobehandlendeenhetClientId,
        )?.accessToken ?: throw RuntimeException("Failed to request access to Enhet: Failed to get OBO token")
        return try {
            val response: HttpResponse = httpClient.get(url) {
                header(HttpHeaders.Authorization, bearerHeader(oboToken))
                header(NAV_CALL_ID_HEADER, callId)
                header(NAV_PERSONIDENT_HEADER, personIdent.value)
                accept(ContentType.Application.Json)
            }
            COUNT_CALL_BEHANDLENDEENHET_SUCCESS.increment()
            response.receive()
        } catch (e: ClientRequestException) {
            handleUnexpectedResponseException(e.response, callId)
        } catch (e: ServerResponseException) {
            handleUnexpectedResponseException(e.response, callId)
        }
    }

    private fun handleUnexpectedResponseException(
        response: HttpResponse,
        callId: String,
    ): BehandlendeEnhetDTO? {
        log.error(
            "Error while requesting BehandlendeEnhet of person from Syfobehandlendeenhet with {}, {}",
            StructuredArguments.keyValue("statusCode", response.status.value.toString()),
            callIdArgument(callId)
        )
        COUNT_CALL_BEHANDLENDEENHET_FAIL.increment()
        return null
    }

    companion object {
        const val BEHANDLENDEENHET_PATH = "/api/system/v2/personident"
        private val log = LoggerFactory.getLogger(BehandlendeEnhetClient::class.java)
    }
}
