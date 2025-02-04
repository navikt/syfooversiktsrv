package no.nav.syfo.personstatus.infrastructure.clients.veileder

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.micrometer.core.instrument.Counter
import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.personstatus.infrastructure.clients.ClientEnvironment
import no.nav.syfo.personstatus.infrastructure.clients.azuread.AzureAdClient
import no.nav.syfo.personstatus.infrastructure.clients.httpClientDefault
import no.nav.syfo.personstatus.infrastructure.METRICS_NS
import no.nav.syfo.personstatus.infrastructure.METRICS_REGISTRY
import no.nav.syfo.util.*
import org.slf4j.LoggerFactory

class VeilederClient(
    private val azureAdClient: AzureAdClient,
    private val clientEnvironment: ClientEnvironment,
    private val httpClient: HttpClient = httpClientDefault(),
) {
    private val veiledereUrl = "${clientEnvironment.baseUrl}$VEILEDERE_PATH"

    suspend fun getVeileder(
        callId: String,
        veilederIdent: String,
    ): Result<VeilederDTO?> {
        val systemToken = azureAdClient.getSystemToken(
            scopeClientId = clientEnvironment.clientId,
        )?.accessToken ?: return Result.failure(RuntimeException("Failed to request access to veileder: Failed to get system token"))
        return try {
            val response: HttpResponse = httpClient.get("$veiledereUrl/$veilederIdent") {
                header(HttpHeaders.Authorization, bearerHeader(systemToken))
                header(NAV_CALL_ID_HEADER, callId)
                accept(ContentType.Application.Json)
            }
            COUNT_CALL_SYFOVEILEDER_SUCCESS.increment()
            Result.success(response.body())
        } catch (e: ResponseException) {
            if (e.response.status == HttpStatusCode.NotFound) {
                log.warn("Veileder $veilederIdent not found in syfoveileder")
                COUNT_CALL_SYFOVEILEDER_NOT_FOUND.increment()
                Result.success(null)
            } else {
                log.error(
                    "Error while requesting veileder from syfoveileder with {}, {}",
                    StructuredArguments.keyValue("statusCode", e.response.status.value.toString()),
                    callIdArgument(callId)
                )
                COUNT_CALL_SYFOVEILEDER_FAIL.increment()
                Result.failure(e)
            }
        }
    }

    companion object {
        const val VEILEDERE_PATH = "/syfoveileder/api/system/veiledere"
        private val log = LoggerFactory.getLogger(this::class.java)

        private const val CALL_SYFOVEILEDER_BASE = "${METRICS_NS}_call_syfoveileder"
        private const val CALL_SYFOVEILEDER_SUCCESS = "${CALL_SYFOVEILEDER_BASE}_success_count"
        private const val CALL_SYFOVEILEDER_NOT_FOUND = "${CALL_SYFOVEILEDER_BASE}_not_found_count"
        private const val CALL_SYFOVEILEDER_FAIL = "${CALL_SYFOVEILEDER_BASE}_fail_count"

        val COUNT_CALL_SYFOVEILEDER_SUCCESS: Counter = Counter
            .builder(CALL_SYFOVEILEDER_SUCCESS)
            .description("Counts the number of successful calls to syfoveileder")
            .register(METRICS_REGISTRY)
        val COUNT_CALL_SYFOVEILEDER_NOT_FOUND: Counter = Counter
            .builder(CALL_SYFOVEILEDER_NOT_FOUND)
            .description("Counts the number of calls to syfoveileder where veileder was not found")
            .register(METRICS_REGISTRY)
        val COUNT_CALL_SYFOVEILEDER_FAIL: Counter = Counter
            .builder(CALL_SYFOVEILEDER_FAIL)
            .description("Counts the number of failed calls to syfoveileder")
            .register(METRICS_REGISTRY)
    }
}
