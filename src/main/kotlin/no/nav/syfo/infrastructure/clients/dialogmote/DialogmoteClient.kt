package no.nav.syfo.infrastructure.clients.dialogmote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.application.dialogmote.DialogmoteAvventDTO
import no.nav.syfo.application.dialogmote.DialogmoteAvventQueryDTO
import no.nav.syfo.application.dialogmote.IDialogmoteClient
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.infrastructure.clients.ClientEnvironment
import no.nav.syfo.infrastructure.clients.azuread.AzureAdClient
import no.nav.syfo.infrastructure.clients.httpClientDefault
import no.nav.syfo.util.NAV_CALL_ID_HEADER
import no.nav.syfo.util.bearerHeader
import no.nav.syfo.util.callIdArgument
import org.slf4j.LoggerFactory

class DialogmoteClient(
    private val azureAdClient: AzureAdClient,
    private val clientEnvironment: ClientEnvironment,
    private val httpClient: HttpClient = httpClientDefault(),
) : IDialogmoteClient {
    override suspend fun getDialogmoteAvvent(
        callId: String,
        token: String,
        personidenter: List<PersonIdent>,
    ): List<DialogmoteAvventDTO>? {
        val oboToken =
            azureAdClient
                .getOnBehalfOfToken(
                    scopeClientId = clientEnvironment.clientId,
                    token,
                )?.accessToken ?: throw RuntimeException("Failed to get OBO-token for isdialogmote avvent")
        val requestDTO = DialogmoteAvventQueryDTO(personidenter.map { it.value })

        return try {
            val response =
                httpClient.post("${clientEnvironment.baseUrl}$AVVENT_API_PATH") {
                    header(HttpHeaders.Authorization, bearerHeader(oboToken))
                    header(NAV_CALL_ID_HEADER, callId)
                    accept(ContentType.Application.Json)
                    contentType(ContentType.Application.Json)
                    setBody(requestDTO)
                }
            when (response.status) {
                HttpStatusCode.OK -> {
                    response.body<List<DialogmoteAvventDTO>>()
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
            "Error while requesting from isdialogmote avvent with {}, {}",
            StructuredArguments.keyValue("statusCode", response.status.value.toString()),
            callIdArgument(callId),
        )
    }

    companion object {
        private const val AVVENT_API_PATH = "/api/avvent/query"
        private val log = LoggerFactory.getLogger(this::class.java)
    }
}
