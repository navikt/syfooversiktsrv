package no.nav.syfo.client.azuread

import io.ktor.client.call.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.syfo.client.httpClientProxy
import org.slf4j.LoggerFactory

class AzureAdClient(
    private val aadAppClient: String,
    private val aadAppSecret: String,
    private val aadTokenEndpoint: String
) {
    private val httpClient = httpClientProxy()

    suspend fun getOnBehalfOfToken(
        scopeClientId: String,
        token: String
    ): AzureAdToken? =
        getAccessToken(
            Parameters.build {
                append("client_id", aadAppClient)
                append("client_secret", aadAppSecret)
                append("client_assertion_type", "urn:ietf:params:oauth:grant-type:jwt-bearer")
                append("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer")
                append("assertion", token)
                append("scope", "api://$scopeClientId/.default")
                append("requested_token_use", "on_behalf_of")
            }
        )?.toAzureAdToken()

    private suspend fun getAccessToken(
        formParameters: Parameters,
    ): AzureAdTokenResponse? =
        try {
            val response: HttpResponse = httpClient.post(aadTokenEndpoint) {
                accept(ContentType.Application.Json)
                body = FormDataContent(formParameters)
            }
            response.receive<AzureAdTokenResponse>()
        } catch (e: ClientRequestException) {
            handleUnexpectedResponseException(e)
        } catch (e: ServerResponseException) {
            handleUnexpectedResponseException(e)
        }

    private fun handleUnexpectedResponseException(
        responseException: ResponseException
    ): AzureAdTokenResponse? {
        log.error(
            "Error while requesting AzureAdAccessToken with statusCode=${responseException.response.status.value}",
            responseException
        )
        return null
    }

    companion object {
        private val log = LoggerFactory.getLogger(AzureAdClient::class.java)
    }
}
