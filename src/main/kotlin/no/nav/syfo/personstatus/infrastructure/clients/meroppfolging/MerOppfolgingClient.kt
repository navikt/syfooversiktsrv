package no.nav.syfo.personstatus.infrastructure.clients.meroppfolging

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.micrometer.core.instrument.Counter
import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.personstatus.application.meroppfolging.IMeroppfolgingClient
import no.nav.syfo.personstatus.application.meroppfolging.SenOppfolgingKandidaterRequestDTO
import no.nav.syfo.personstatus.application.meroppfolging.SenOppfolgingKandidaterResponseDTO
import no.nav.syfo.personstatus.domain.PersonIdent
import no.nav.syfo.personstatus.infrastructure.METRICS_NS
import no.nav.syfo.personstatus.infrastructure.METRICS_REGISTRY
import no.nav.syfo.personstatus.infrastructure.clients.ClientEnvironment
import no.nav.syfo.personstatus.infrastructure.clients.azuread.AzureAdClient
import no.nav.syfo.personstatus.infrastructure.clients.httpClientDefault
import no.nav.syfo.util.NAV_CALL_ID_HEADER
import no.nav.syfo.util.bearerHeader
import no.nav.syfo.util.callIdArgument
import org.slf4j.LoggerFactory

class MerOppfolgingClient(
    private val azureAdClient: AzureAdClient,
    private val clientEnvironment: ClientEnvironment,
    private val httpClient: HttpClient = httpClientDefault(),
) : IMeroppfolgingClient {

    override suspend fun getSenOppfolgingKandidater(
        callId: String,
        token: String,
        personidenter: List<PersonIdent>
    ): SenOppfolgingKandidaterResponseDTO? {
        val oboToken = azureAdClient.getOnBehalfOfToken(
            scopeClientId = clientEnvironment.clientId,
            token,
        )?.accessToken ?: throw RuntimeException("Failed to get OBO-token for sen oppfolging kandidater")
        val requestDTO = SenOppfolgingKandidaterRequestDTO(personidenter.map { it.value })

        return try {
            val response = httpClient.post("${clientEnvironment.baseUrl}$MEROPPFOLGING_SENOPPFOLGING_KANDIDATER_API_PATH") {
                header(HttpHeaders.Authorization, bearerHeader(oboToken))
                header(NAV_CALL_ID_HEADER, callId)
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                setBody(requestDTO)
            }
            when (response.status) {
                HttpStatusCode.OK -> {
                    COUNT_CALL_MEROPPFOLGING_SUCCESS.increment()
                    response.body<SenOppfolgingKandidaterResponseDTO>()
                }
                HttpStatusCode.NotFound -> {
                    log.error("Resource not found")
                    COUNT_CALL_MEROPPFOLGING_FAIL.increment()
                    null
                }
                else -> {
                    log.error("Unhandled status code: ${response.status}")
                    COUNT_CALL_MEROPPFOLGING_FAIL.increment()
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
            "Error while requesting from ismeroppfolging with {}, {}",
            StructuredArguments.keyValue("statusCode", response.status.value.toString()),
            callIdArgument(callId)
        )
        COUNT_CALL_MEROPPFOLGING_FAIL.increment()
    }

    companion object {
        const val MEROPPFOLGING_SENOPPFOLGING_KANDIDATER_API_PATH = "/api/internad/v1/senoppfolging/get-kandidater"
        private val log = LoggerFactory.getLogger(this::class.java)

        private const val CALL_MEROPPFOLGING_BASE = "${METRICS_NS}_call_ismeroppfolging"
        private const val CALL_MEROPPFOLGING_SUCCESS = "${CALL_MEROPPFOLGING_BASE}_success_count"
        private const val CALL_MEROPPFOLGING_FAIL = "${CALL_MEROPPFOLGING_BASE}_fail_count"

        val COUNT_CALL_MEROPPFOLGING_SUCCESS: Counter = Counter
            .builder(CALL_MEROPPFOLGING_SUCCESS)
            .description("Counts the number of successful calls to ismeroppfolging")
            .register(METRICS_REGISTRY)
        val COUNT_CALL_MEROPPFOLGING_FAIL: Counter = Counter
            .builder(CALL_MEROPPFOLGING_FAIL)
            .description("Counts the number of failed calls to ismeroppfolging")
            .register(METRICS_REGISTRY)
    }
}
