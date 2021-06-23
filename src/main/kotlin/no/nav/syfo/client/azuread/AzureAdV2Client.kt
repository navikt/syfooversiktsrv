package no.nav.syfo.client.azuread

import io.ktor.client.call.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import no.nav.syfo.client.httpClientProxy
import org.slf4j.LoggerFactory

class AzureAdV2Client(
    private val aadAppClient: String,
    private val aadAppSecret: String,
    private val aadTokenEndpoint: String
) {
    private val httpClient = httpClientProxy()

    suspend fun getOnBehalfOfToken(
        scopeClientId: String,
        token: String
    ): AzureAdV2Token? {
        val scopeToken = scopeTokenMap[scopeClientId]

        return mutex.withLock {
            if (scopeToken == null || scopeToken.isExpired()) {
                val azureAdV2TokenResponse = getAccessToken(
                    Parameters.build {
                        append("client_id", aadAppClient)
                        append("client_secret", aadAppSecret)
                        append("client_assertion_type", "urn:ietf:params:oauth:grant-type:jwt-bearer")
                        append("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer")
                        append("assertion", token)
                        append("scope", "api://$scopeClientId/.default")
                        append("requested_token_use", "on_behalf_of")
                    }
                )
                if (azureAdV2TokenResponse == null) {
                    null
                } else {
                    val azureADV2Token = azureAdV2TokenResponse.toAzureAdV2Token()
                    scopeTokenMap[scopeClientId] = azureADV2Token
                    azureADV2Token
                }
            } else {
                scopeToken
            }
        }
    }

    private suspend fun getAccessToken(formParameters: Parameters): AzureAdV2TokenResponse? {
        return try {
            val response: HttpResponse = httpClient.post(aadTokenEndpoint) {
                accept(ContentType.Application.Json)
                body = FormDataContent(formParameters)
            }
            response.receive<AzureAdV2TokenResponse>()
        } catch (e: ClientRequestException) {
            handleUnexpectedResponseException(e)
        } catch (e: ServerResponseException) {
            handleUnexpectedResponseException(e)
        }
    }

    private fun handleUnexpectedResponseException(
        responseException: ResponseException
    ): AzureAdV2TokenResponse? {
        log.error(
            "Error while requesting AzureAdAccessToken with statusCode=${responseException.response.status.value}",
            responseException
        )
        return null
    }

    companion object {
        private val log = LoggerFactory.getLogger(AzureAdV2Client::class.java)

        private val mutex = Mutex()

        @Volatile
        private var scopeTokenMap = HashMap<String, AzureAdV2Token>()
    }
}
