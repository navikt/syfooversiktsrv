package no.nav.syfo.personstatus.infrastructure.clients.azuread

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.syfo.application.cache.ValkeyStore
import no.nav.syfo.testutil.ExternalMockEnvironment
import no.nav.syfo.testutil.UserConstants.VEILEDER_ID
import no.nav.syfo.testutil.UserConstants.VEILEDER_IDENT_NO_AZURE_AD_TOKEN
import no.nav.syfo.testutil.generateJWT
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.LocalDateTime

object AzureAdClientSpek : Spek({
    val externalMockEnvironment = ExternalMockEnvironment.instance
    val valkeyStoreMock = mockk<ValkeyStore>(relaxed = true)
    val oneHourInSeconds = 1 * 60 * 60L

    val azureAdClient = AzureAdClient(
        azureEnvironment = externalMockEnvironment.environment.azure,
        valkeyStore = valkeyStoreMock,
        httpClient = externalMockEnvironment.mockHttpClient
    )

    describe("AzureAdClient") {
        afterEachTest { clearMocks(valkeyStoreMock) }

        val validToken = generateJWT(
            audience = externalMockEnvironment.environment.azure.appClientId,
            issuer = externalMockEnvironment.wellKnownVeilederV2.issuer,
            navIdent = VEILEDER_ID,
        )

        describe("Get obo-token") {
            it("Returns obo-token from AzureAD and stores in cache") {
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

            it("Returns obo-token from cache") {
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

            it("Does not store cache when azureAd return null") {
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

        describe("Get system token") {
            it("Returns system-token from AzureAD and stores in cache") {
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

            it("Returns system-token from cache") {
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
})
