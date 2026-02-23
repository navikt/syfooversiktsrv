package no.nav.syfo.infrastructure.clients.manglendemedvirkning

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.micrometer.core.instrument.Counter
import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.infrastructure.clients.ClientEnvironment
import no.nav.syfo.infrastructure.clients.azuread.AzureAdClient
import no.nav.syfo.infrastructure.clients.httpClientDefault
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.infrastructure.METRICS_NS
import no.nav.syfo.infrastructure.METRICS_REGISTRY
import no.nav.syfo.application.manglendemedvirkning.IManglendeMedvirkningClient
import no.nav.syfo.application.manglendemedvirkning.ManglendeMedvirkningRequestDTO
import no.nav.syfo.application.manglendemedvirkning.ManglendeMedvirkningResponseDTO
import no.nav.syfo.util.NAV_CALL_ID_HEADER
import no.nav.syfo.util.bearerHeader
import no.nav.syfo.util.callIdArgument
import org.slf4j.LoggerFactory

class ManglendeMedvirkningClient(
    private val azureAdClient: AzureAdClient,
    private val clientEnvironment: ClientEnvironment,
    private val httpClient: HttpClient = httpClientDefault(),
) : IManglendeMedvirkningClient {

    private val manglendeMedvirkningUrl = "${clientEnvironment.baseUrl}$MANGLENDE_MEDVIRKNING_API_PATH"

    override suspend fun getLatestVurderinger(
        callId: String,
        token: String,
        personidenter: List<PersonIdent>,
    ): ManglendeMedvirkningResponseDTO? {
        val oboToken = azureAdClient.getOnBehalfOfToken(
            scopeClientId = clientEnvironment.clientId,
            token,
        )?.accessToken ?: throw RuntimeException("Failed to get OBO-token for manglende medvirkning vurdering")
        val requestDTO = ManglendeMedvirkningRequestDTO(personidenter.map { it.value })
        return try {
            val response = httpClient.post(manglendeMedvirkningUrl) {
                header(HttpHeaders.Authorization, bearerHeader(oboToken))
                header(NAV_CALL_ID_HEADER, callId)
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                setBody(requestDTO)
            }
            when (response.status) {
                HttpStatusCode.OK -> {
                    COUNT_CALL_MANGLENDE_MEDVIRKNING_SUCCESS.increment()
                    response.body<ManglendeMedvirkningResponseDTO>()
                }
                HttpStatusCode.NotFound -> {
                    log.error("Resource not found")
                    COUNT_CALL_MANGLENDE_MEDVIRKNING_FAIL.increment()
                    null
                }
                else -> {
                    log.error("Unhandled status code: ${response.status}")
                    COUNT_CALL_MANGLENDE_MEDVIRKNING_FAIL.increment()
                    null
                }
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
            "Error while requesting from ismanglendemedvirkning with {}, {}",
            StructuredArguments.keyValue("statusCode", response.status.value.toString()),
            callIdArgument(callId)
        )
        COUNT_CALL_MANGLENDE_MEDVIRKNING_FAIL.increment()
    }

    companion object {
        const val MANGLENDE_MEDVIRKNING_API_PATH = "/api/internad/v1/manglende-medvirkning/get-vurderinger"
        private val log = LoggerFactory.getLogger(ManglendeMedvirkningClient::class.java)

        const val CALL_MANGLENDE_MEDVIRKNING_BASE = "${METRICS_NS}_call_ismanglendemedvirkning"
        const val CALL_MANGLENDE_MEDVIRKNING_SUCCESS = "${CALL_MANGLENDE_MEDVIRKNING_BASE}_success_count"
        const val CALL_MANGLENDE_MEDVIRKNING_FAIL = "${CALL_MANGLENDE_MEDVIRKNING_BASE}_fail_count"

        val COUNT_CALL_MANGLENDE_MEDVIRKNING_SUCCESS: Counter = Counter
            .builder(CALL_MANGLENDE_MEDVIRKNING_SUCCESS)
            .description("Counts the number of successful calls to ismanglendemedvirkning")
            .register(METRICS_REGISTRY)
        val COUNT_CALL_MANGLENDE_MEDVIRKNING_FAIL: Counter = Counter
            .builder(CALL_MANGLENDE_MEDVIRKNING_FAIL)
            .description("Counts the number of failed calls to ismanglendemedvirkning")
            .register(METRICS_REGISTRY)
    }
}
