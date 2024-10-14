package no.nav.syfo.personstatus.infrastructure.clients.aktivitetskrav

import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.*
import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.personstatus.domain.PersonIdent
import no.nav.syfo.personstatus.application.aktivitetskrav.AktivitetskravRequestDTO
import no.nav.syfo.personstatus.application.aktivitetskrav.GetAktivitetskravForPersonsResponseDTO
import no.nav.syfo.personstatus.application.aktivitetskrav.IAktivitetskravClient
import no.nav.syfo.personstatus.infrastructure.clients.ClientEnvironment
import no.nav.syfo.personstatus.infrastructure.clients.azuread.AzureAdClient
import no.nav.syfo.personstatus.infrastructure.clients.httpClientDefault
import no.nav.syfo.util.NAV_CALL_ID_HEADER
import no.nav.syfo.util.bearerHeader
import no.nav.syfo.util.callIdArgument
import org.slf4j.LoggerFactory

class AktivitetskravClient(
    private val azureAdClient: AzureAdClient,
    private val clientEnvironment: ClientEnvironment,
    private val httpClient: HttpClient = httpClientDefault(),
) : IAktivitetskravClient {

    private val baseUrl = "${clientEnvironment.baseUrl}$API_BASE_PATH"

    override suspend fun getAktivitetskravForPersons(
        callId: String,
        token: String,
        personidenter: List<PersonIdent>,
    ): GetAktivitetskravForPersonsResponseDTO? {
        val oboToken = azureAdClient.getOnBehalfOfToken(
            scopeClientId = clientEnvironment.clientId,
            token,
        )?.accessToken ?: throw RuntimeException("Failed to get OBO-token for aktivitetskrav vurdering")
        val requestDTO = AktivitetskravRequestDTO(personidenter.map { it.value })
        return try {
            val response = httpClient.post("$baseUrl/get-vurderinger") {
                header(HttpHeaders.Authorization, bearerHeader(oboToken))
                header(NAV_CALL_ID_HEADER, callId)
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                setBody(requestDTO)
            }
            when (response.status) {
                HttpStatusCode.OK -> {
                    response.body<GetAktivitetskravForPersonsResponseDTO>()
                }
                HttpStatusCode.NotFound -> {
                    log.error("Resource not found")
                    null
                }
                else -> {
                    log.error("Unhandled status code: ${response.status}")
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
            "Error while requesting from isaktivitetskrav with {}, {}",
            StructuredArguments.keyValue("statusCode", response.status.value.toString()),
            callIdArgument(callId)
        )
    }

    companion object {
        private const val API_BASE_PATH = "/api/internad/v1/aktivitetskrav"
        private val log = LoggerFactory.getLogger(AktivitetskravClient::class.java)
    }
}
