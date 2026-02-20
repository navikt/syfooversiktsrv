package no.nav.syfo.infrastructure.clients.dialogmotekandidat

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.application.dialogmotekandidat.DialogmotekandidatRequestDTO
import no.nav.syfo.application.dialogmotekandidat.DialogmotekandidatResponseDTO
import no.nav.syfo.application.dialogmotekandidat.IDialogmotekandidatClient
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.infrastructure.clients.ClientEnvironment
import no.nav.syfo.infrastructure.clients.azuread.AzureAdClient
import no.nav.syfo.infrastructure.clients.httpClientDefault
import no.nav.syfo.util.NAV_CALL_ID_HEADER
import no.nav.syfo.util.bearerHeader
import no.nav.syfo.util.callIdArgument
import org.slf4j.LoggerFactory

class DialogmotekandidatClient(
    private val azureAdClient: AzureAdClient,
    private val clientEnvironment: ClientEnvironment,
    private val httpClient: HttpClient = httpClientDefault(),
) : IDialogmotekandidatClient {

    override suspend fun getDialogmotekandidater(
        callId: String,
        token: String,
        personidenter: List<PersonIdent>,
    ): DialogmotekandidatResponseDTO? {
        val oboToken = azureAdClient.getOnBehalfOfToken(
            scopeClientId = clientEnvironment.clientId,
            token,
        )?.accessToken ?: throw RuntimeException("Failed to get OBO-token for dialogmotekandidat")
        val requestDTO = DialogmotekandidatRequestDTO(personidenter.map { it.value })

        return try {
            val response = httpClient.post("${clientEnvironment.baseUrl}$DIALOGMOTEKANDIDATER_API_PATH") {
                header(HttpHeaders.Authorization, bearerHeader(oboToken))
                header(NAV_CALL_ID_HEADER, callId)
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                setBody(requestDTO)
            }
            when (response.status) {
                HttpStatusCode.OK -> {
                    response.body<DialogmotekandidatResponseDTO>()
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
            "Error while requesting from isdialogmotekandidat with {}, {}",
            StructuredArguments.keyValue("statusCode", response.status.value.toString()),
            callIdArgument(callId)
        )
    }

    companion object {
        private const val DIALOGMOTEKANDIDATER_API_PATH = "/api/internad/v1/kandidat/get-kandidater"
        private val log = LoggerFactory.getLogger(this::class.java)
    }
}
