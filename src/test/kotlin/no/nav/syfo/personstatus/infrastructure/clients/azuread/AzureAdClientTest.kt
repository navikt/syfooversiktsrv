package no.nav.syfo.infrastructure.clients.azuread

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.syfo.util.cache.ValkeyStore
import no.nav.syfo.testutil.ExternalMockEnvironment
import no.nav.syfo.testutil.UserConstants.VEILEDER_ID
import no.nav.syfo.testutil.UserConstants.VEILEDER_IDENT_NO_AZURE_AD_TOKEN
import no.nav.syfo.testutil.generateJWT
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class AzureAdClientTest {
    private val externalMockEnvironment = ExternalMockEnvironment.instance
    private val valkeyStoreMock = mockk<ValkeyStore>(relaxed = true)
    private val oneHourInSeconds = 1 * 60 * 60L

    private val azureAdClient = AzureAdClient(
        azureEnvironment = externalMockEnvironment.environment.azure,
        valkeyStore = valkeyStoreMock,
        httpClient = externalMockEnvironment.mockHttpClient
    )

    @AfterEach
    fun tearDown() {
        clearMocks(valkeyStoreMock)
    }

    private val validToken = generateJWT(
        audience = externalMockEnvironment.environment.azure.appClientId,
        issuer = externalMockEnvironment.wellKnownVeilederV2.issuer,
        navIdent = VEILEDER_ID,
    )

    @Nested
    @DisplayName("Get obo-token")
    inner class GetOboToken {

        @Test
        fun `Returns obo-token from AzureAD and stores in cache`() {
            val huskelappClientId = externalMockEnvironment.environment.clients.ishuskelapp.clientId
            val cacheKey =
                "${AzureAdClient.CACHE_AZUREAD_TOKEN_OBO_KEY_PREFIX}$huskelappClientId-$VEILEDER_ID"
            every { valkeyStoreMock.getObject<AzureAdToken?>(key = cacheKey) } returns null

            runBlocking {
                azureAdClient.getOnBehalfOfToken(
                    scopeClientId = huskelappClientId,
                    token = validToken,
                )
            }

            verify(exactly = 1) { valkeyStoreMock.getObject<AzureAdToken?>(key = cacheKey) }
            verify(exactly = 1) {
                valkeyStoreMock.setObject<Any>(
                    key = cacheKey,
                    value = any(),
                    expireSeconds = oneHourInSeconds,
                )
            }
        }

        @Test
        fun `Returns obo-token from cache`() {
            val huskelappClientId = externalMockEnvironment.environment.clients.ishuskelapp.clientId
            val cacheKey =
                "${AzureAdClient.CACHE_AZUREAD_TOKEN_OBO_KEY_PREFIX}$huskelappClientId-$VEILEDER_ID"
            every {
                valkeyStoreMock.getObject<AzureAdToken?>(key = cacheKey)
            } returns AzureAdToken(
                accessToken = "123",
                expires = LocalDateTime.now().plusHours(1),
            )

            runBlocking {
                azureAdClient.getOnBehalfOfToken(
                    scopeClientId = huskelappClientId,
                    token = validToken,
                )
            }

            verify(exactly = 1) { valkeyStoreMock.getObject<AzureAdToken?>(key = cacheKey) }
            verify(exactly = 0) { valkeyStoreMock.setObject<Any>(any(), any(), any()) }
        }

        @Test
        fun `Does not store cache when azureAd return null`() {
            val validTokenReturningNull = generateJWT(
                audience = externalMockEnvironment.environment.azure.appClientId,
                issuer = externalMockEnvironment.wellKnownVeilederV2.issuer,
                navIdent = VEILEDER_IDENT_NO_AZURE_AD_TOKEN,
            )
            val huskelappClientId = externalMockEnvironment.environment.clients.ishuskelapp.clientId
            val cacheKey =
                "${AzureAdClient.CACHE_AZUREAD_TOKEN_OBO_KEY_PREFIX}$huskelappClientId-$VEILEDER_IDENT_NO_AZURE_AD_TOKEN"
            every { valkeyStoreMock.getObject<AzureAdToken?>(key = cacheKey) } returns null

            runBlocking {
                azureAdClient.getOnBehalfOfToken(
                    scopeClientId = huskelappClientId,
                    token = validTokenReturningNull,
                )
            }

            verify(exactly = 1) { valkeyStoreMock.getObject<AzureAdToken?>(key = cacheKey) }
            verify(exactly = 0) { valkeyStoreMock.setObject<Any>(any(), any(), any()) }
        }
    }

    @Nested
    @DisplayName("Get system token")
    inner class GetSystemToken {

        @Test
        fun `Returns system-token from AzureAD and stores in cache`() {
            val pdlClientId = externalMockEnvironment.environment.clients.pdl.clientId
            val cacheKey = "${AzureAdClient.CACHE_AZUREAD_TOKEN_SYSTEM_KEY_PREFIX}$pdlClientId"
            every { valkeyStoreMock.getObject<AzureAdToken?>(any()) } returns null

            runBlocking {
                azureAdClient.getSystemToken(
                    scopeClientId = pdlClientId,
                )
            }

            verify(exactly = 1) { valkeyStoreMock.getObject<AzureAdToken?>(key = cacheKey) }
            verify(exactly = 1) {
                valkeyStoreMock.setObject<Any>(
                    key = cacheKey,
                    value = any(),
                    expireSeconds = oneHourInSeconds,
                )
            }
        }

        @Test
        fun `Returns system-token from cache`() {
            val pdlClientId = externalMockEnvironment.environment.clients.pdl.clientId
            val cacheKey = "${AzureAdClient.CACHE_AZUREAD_TOKEN_SYSTEM_KEY_PREFIX}$pdlClientId"
            every {
                valkeyStoreMock.getObject<AzureAdToken?>(key = cacheKey)
            } returns AzureAdToken(
                accessToken = "123",
                expires = LocalDateTime.now().plusHours(1),
            )

            runBlocking {
                azureAdClient.getSystemToken(
                    scopeClientId = pdlClientId,
                )
            }

            verify(exactly = 1) { valkeyStoreMock.getObject<AzureAdToken?>(key = cacheKey) }
            verify(exactly = 0) { valkeyStoreMock.setObject<Any>(any(), any(), any()) }
        }
    }
}
