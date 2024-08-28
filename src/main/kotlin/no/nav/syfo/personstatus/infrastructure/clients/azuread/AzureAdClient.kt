package no.nav.syfo.personstatus.infrastructure.clients.azuread

import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.plugins.*
import io.ktor.client.request.accept
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.syfo.application.cache.RedisStore
import no.nav.syfo.personstatus.api.v2.auth.getNAVIdentFromToken
import no.nav.syfo.personstatus.infrastructure.clients.httpClientProxy
import org.slf4j.LoggerFactory
import kotlin.jvm.java
import kotlin.let

class AzureAdClient(
    private val azureEnvironment: AzureEnvironment,
    private val redisStore: RedisStore,
    private val httpClient: HttpClient = httpClientProxy(),
) {

    suspend fun getOnBehalfOfToken(
        scopeClientId: String,
        token: String
    ): AzureAdToken? {
        val veilederIdent = getNAVIdentFromToken(token)
        val cacheKey = "$CACHE_AZUREAD_TOKEN_OBO_KEY_PREFIX$scopeClientId-$veilederIdent"
        val cachedOboToken: AzureAdToken? = redisStore.getObject(key = cacheKey)
        return if (cachedOboToken?.isExpired() == false) {
            cachedOboToken
        } else {
            val azureAdTokenResponse = getAccessToken(
                Parameters.build {
                    append("client_id", azureEnvironment.appClientId)
                    append("client_secret", azureEnvironment.appClientSecret)
                    append("client_assertion_type", "urn:ietf:params:oauth:grant-type:jwt-bearer")
                    append("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer")
                    append("assertion", token)
                    append("scope", "api://$scopeClientId/.default")
                    append("requested_token_use", "on_behalf_of")
                }
            )

            azureAdTokenResponse?.toAzureAdToken()?.also { oboToken ->
                redisStore.setObject(
                    key = cacheKey,
                    value = oboToken,
                    expireSeconds = azureAdTokenResponse.expires_in,
                )
            }
        }
    }

    suspend fun getSystemToken(
        scopeClientId: String,
    ): AzureAdToken? {
        val cacheKey = "${CACHE_AZUREAD_TOKEN_SYSTEM_KEY_PREFIX}$scopeClientId"
        val cachedToken = redisStore.getObject<AzureAdToken>(key = cacheKey)
        return if (cachedToken?.isExpired() == false) {
            COUNT_CALL_AZUREAD_TOKEN_SYSTEM_CACHE_HIT.increment()
            cachedToken
        } else {
            val azureAdTokenResponse = getAccessToken(
                Parameters.build {
                    append("client_id", azureEnvironment.appClientId)
                    append("client_secret", azureEnvironment.appClientSecret)
                    append("grant_type", "client_credentials")
                    append("scope", "api://$scopeClientId/.default")
                }
            )
            azureAdTokenResponse?.let { token ->
                val azureAdToken = token.toAzureAdToken()
                COUNT_CALL_AZUREAD_TOKEN_SYSTEM_CACHE_MISS.increment()
                redisStore.setObject(
                    key = cacheKey,
                    value = azureAdToken,
                    expireSeconds = token.expires_in
                )
                azureAdToken
            }
        }
    }

    private suspend fun getAccessToken(
        formParameters: Parameters,
    ): AzureAdTokenResponse? =
        try {
            val response: HttpResponse = httpClient.post(azureEnvironment.openidConfigTokenEndpoint) {
                accept(ContentType.Application.Json)
                setBody(FormDataContent(formParameters))
            }
            response.body<AzureAdTokenResponse>()
        } catch (e: ResponseException) {
            log.error(
                "Error while requesting AzureAdAccessToken with statusCode=${e.response.status.value}",
                e
            )
            null
        }

    companion object {
        const val CACHE_AZUREAD_TOKEN_SYSTEM_KEY_PREFIX = "azuread-token-system-"
        const val CACHE_AZUREAD_TOKEN_OBO_KEY_PREFIX = "azuread-token-obo-"

        private val log = LoggerFactory.getLogger(AzureAdClient::class.java)
    }
}
