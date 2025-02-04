package no.nav.syfo.personstatus.infrastructure.clients.veileder

import kotlinx.coroutines.runBlocking
import no.nav.syfo.personstatus.infrastructure.clients.azuread.AzureAdClient
import no.nav.syfo.testutil.ExternalMockEnvironment
import no.nav.syfo.testutil.UserConstants
import org.amshove.kluent.shouldBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

private const val anyCallId = "callId"

object VeilederClientSpek : Spek({
    val externalMockEnvironment = ExternalMockEnvironment.instance
    val azureAdClient = AzureAdClient(
        azureEnvironment = externalMockEnvironment.environment.azure,
        redisStore = externalMockEnvironment.redisStore,
        httpClient = externalMockEnvironment.mockHttpClient
    )
    val veilederClient = VeilederClient(
        azureAdClient = azureAdClient,
        clientEnvironment = externalMockEnvironment.environment.clients.syfoveileder,
        httpClient = externalMockEnvironment.mockHttpClient
    )

    describe("getVeileder") {
        it("Returns veileder result when veileder found") {
            val result = runBlocking {
                veilederClient.getVeileder(
                    callId = anyCallId,
                    veilederIdent = UserConstants.VEILEDER_ID,
                )
            }

            val veilederDTO = result.getOrThrow()
            veilederDTO?.enabled shouldBeEqualTo true
            veilederDTO?.ident shouldBeEqualTo UserConstants.VEILEDER_ID
        }
        it("Returns null result when veileder not found") {
            val result = runBlocking {
                veilederClient.getVeileder(
                    callId = anyCallId,
                    veilederIdent = UserConstants.VEILEDER_ID_2,
                )
            }
            val veilederDTO = result.getOrThrow()
            veilederDTO shouldBeEqualTo null
        }
        it("Returns failure when request fails") {
            val result = runBlocking {
                veilederClient.getVeileder(
                    callId = anyCallId,
                    veilederIdent = UserConstants.VEILEDER_ID_WITH_ERROR,
                )
            }
            result.isFailure shouldBeEqualTo true
        }
    }
})
