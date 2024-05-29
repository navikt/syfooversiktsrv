package no.nav.syfo.personstatus.infrastructure

import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.micrometer.core.instrument.Counter
import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.client.ClientEnvironment
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.client.httpClientDefault
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.metric.METRICS_NS
import no.nav.syfo.metric.METRICS_REGISTRY
import no.nav.syfo.personstatus.application.arbeidsuforhet.ArbeidsuforhetvurderingDTO
import no.nav.syfo.personstatus.application.arbeidsuforhet.IArbeidsuforhetvurderingClient
import no.nav.syfo.util.NAV_CALL_ID_HEADER
import no.nav.syfo.util.NAV_PERSONIDENT_HEADER
import no.nav.syfo.util.bearerHeader
import no.nav.syfo.util.callIdArgument
import org.slf4j.LoggerFactory

class ArbeidsuforhetvurderingClient(
    private val azureAdClient: AzureAdClient,
    private val clientEnvironment: ClientEnvironment,
) : IArbeidsuforhetvurderingClient {

    private val httpClient = httpClientDefault()
    private val isarbeidsuforhetUrl = "${clientEnvironment.baseUrl}$ARBEIDSUFORHET_API_PATH"

    override suspend fun getVurdering(
        callId: String,
        token: String,
        personIdent: PersonIdent,
    ): ArbeidsuforhetvurderingDTO? {
        val oboToken = azureAdClient.getOnBehalfOfToken(
            scopeClientId = clientEnvironment.clientId,
            token,
        )?.accessToken ?: throw RuntimeException("Failed to get OBO-token for arbeidsbeidsuforhet vurdering")
        return try {
            val response = httpClient.get(isarbeidsuforhetUrl) {
                header(HttpHeaders.Authorization, bearerHeader(oboToken))
                header(NAV_CALL_ID_HEADER, callId)
                header(NAV_PERSONIDENT_HEADER, personIdent.value)
                accept(ContentType.Application.Json)
            }
            when (response.status) {
                HttpStatusCode.OK -> {
                    COUNT_CALL_ISARBEIDSUFORHET_SUCCESS.increment()
                    response.body<ArbeidsuforhetvurderingDTO>()
                }
                HttpStatusCode.NotFound -> {
                    log.error("Resource not found")
                    COUNT_CALL_ISARBEIDSUFORHET_FAIL.increment()
                    null
                }
                else -> {
                    log.error("Unhandled status code: ${response.status}")
                    COUNT_CALL_ISARBEIDSUFORHET_FAIL.increment()
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
            "Error while requesting from isarbeidsuforhet with {}, {}",
            StructuredArguments.keyValue("statusCode", response.status.value.toString()),
            callIdArgument(callId)
        )
        COUNT_CALL_ISARBEIDSUFORHET_FAIL.increment()
    }

    companion object {
        private const val ARBEIDSUFORHET_API_PATH = "/api/internad/v1/arbeidsuforhet/vurderinger"
        private val log = LoggerFactory.getLogger(ArbeidsuforhetvurderingClient::class.java)

        const val CALL_ISARBEIDSUFORHET_BASE = "${METRICS_NS}_call_isarbeidsuforhet"
        const val CALL_ISARBEIDSUFORHET_SUCCESS = "${CALL_ISARBEIDSUFORHET_BASE}_success_count"
        const val CALL_ISARBEIDSUFORHET_FAIL = "${CALL_ISARBEIDSUFORHET_BASE}_fail_count"

        val COUNT_CALL_ISARBEIDSUFORHET_SUCCESS: Counter = Counter
            .builder(CALL_ISARBEIDSUFORHET_SUCCESS)
            .description("Counts the number of successful calls to isarbeidsuforhet")
            .register(METRICS_REGISTRY)
        val COUNT_CALL_ISARBEIDSUFORHET_FAIL: Counter = Counter
            .builder(CALL_ISARBEIDSUFORHET_FAIL)
            .description("Counts the number of failed calls to isarbeidsuforhet")
            .register(METRICS_REGISTRY)
    }
}
