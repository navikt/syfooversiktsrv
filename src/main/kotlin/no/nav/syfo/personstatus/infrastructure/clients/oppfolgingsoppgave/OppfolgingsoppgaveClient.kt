package no.nav.syfo.personstatus.infrastructure.clients.oppfolgingsoppgave

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.personstatus.application.oppfolgingsoppgave.*
import no.nav.syfo.personstatus.domain.PersonIdent
import no.nav.syfo.personstatus.infrastructure.clients.ClientEnvironment
import no.nav.syfo.personstatus.infrastructure.clients.azuread.AzureAdClient
import no.nav.syfo.personstatus.infrastructure.clients.httpClientDefault
import no.nav.syfo.util.NAV_CALL_ID_HEADER
import no.nav.syfo.util.bearerHeader
import no.nav.syfo.util.callIdArgument
import org.slf4j.LoggerFactory

class OppfolgingsoppgaveClient(
    private val azureAdClient: AzureAdClient,
    private val clientEnvironment: ClientEnvironment,
    private val httpClient: HttpClient = httpClientDefault(),
) : IOppfolgingsoppgaveClient {

    private val ishuskelappUrl = "${clientEnvironment.baseUrl}$GET_OPPFOLGINGSOPPGAVER_API_PATH"

    override suspend fun getActiveOppfolgingsoppgaver(
        callId: String,
        token: String,
        personidenter: List<PersonIdent>,
    ): OppfolgingsoppgaverLatestVersionResponseDTO? {
        val oboToken = azureAdClient.getOnBehalfOfToken(
            scopeClientId = clientEnvironment.clientId,
            token,
        )?.accessToken ?: throw RuntimeException("Failed to get OBO-token for oppfolgingsoppgave")

        val requestDTO = OppfolgingsoppgaverRequestDTO(personidenter.map { it.value })

        return try {
            val response: HttpResponse = httpClient.post(ishuskelappUrl) {
                header(HttpHeaders.Authorization, bearerHeader(oboToken))
                header(NAV_CALL_ID_HEADER, callId)
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                setBody(requestDTO)
            }
            when (response.status) {
                HttpStatusCode.OK -> {
                    val responseDTO = response.body<OppfolgingsoppgaverResponseDTO>()
                    responseDTO.oppfolgingsoppgaver
                        .mapValues { OppfolgingsoppgaveLatestVersionDTO.fromOppfolgingsoppgaveDTO(it.value) }
                        .run { OppfolgingsoppgaverLatestVersionResponseDTO(this) }
                }
                HttpStatusCode.NoContent -> null
                HttpStatusCode.NotFound -> {
                    log.error("Resource not found")
                    null
                }

                else -> {
                    log.error("Unhandled status code: ${response.status}")
                    null
                }
            }
        } catch (e: ResponseException) {
            log.error(
                "Error while requesting from ishuskelapp with {}, {}",
                StructuredArguments.keyValue("statusCode", e.response.status.value.toString()),
                callIdArgument(callId)
            )
            throw e
        }
    }

    companion object {
        const val GET_OPPFOLGINGSOPPGAVER_API_PATH = "/api/internad/v1/huskelapp/get-oppfolgingsoppgaver"

        private val log = LoggerFactory.getLogger(OppfolgingsoppgaveClient::class.java)
    }
}
