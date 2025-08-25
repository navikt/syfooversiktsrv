package no.nav.syfo.personstatus.infrastructure.clients.veileder

import kotlinx.coroutines.runBlocking
import no.nav.syfo.personstatus.infrastructure.clients.azuread.AzureAdClient
import no.nav.syfo.testutil.ExternalMockEnvironment
import no.nav.syfo.testutil.UserConstants
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull

private const val anyCallId = "callId"

class VeilederClientTest {
    private val externalMockEnvironment = ExternalMockEnvironment.instance
    private val azureAdClient = AzureAdClient(
        azureEnvironment = externalMockEnvironment.environment.azure,
        valkeyStore = externalMockEnvironment.valkeyStore,
        httpClient = externalMockEnvironment.mockHttpClient
    )
    private val veilederClient = VeilederClient(
        azureAdClient = azureAdClient,
        clientEnvironment = externalMockEnvironment.environment.clients.syfoveileder,
        valkeyStore = externalMockEnvironment.valkeyStore,
        httpClient = externalMockEnvironment.mockHttpClient
    )

    @Test
    fun `Returns veileder result when veileder found`() {
        val result = runBlocking {
            veilederClient.getVeileder(
                callId = anyCallId,
                veilederIdent = UserConstants.VEILEDER_ID,
            )
        }

        val veilederDTO = result.getOrThrow()
        assertTrue(veilederDTO?.enabled!!)
        assertEquals(UserConstants.VEILEDER_ID, veilederDTO?.ident)
    }

    @Test
    fun `Returns null result when veileder not found`() {
        val result = runBlocking {
            veilederClient.getVeileder(
                callId = anyCallId,
                veilederIdent = UserConstants.VEILEDER_ID_2,
            )
        }
        val veilederDTO = result.getOrThrow()
        assertNull(veilederDTO)
    }

    @Test
    fun `Returns failure when request fails`() {
        val result = runBlocking {
            veilederClient.getVeileder(
                callId = anyCallId,
                veilederIdent = UserConstants.VEILEDER_ID_WITH_ERROR,
            )
        }
        assertTrue(result.isFailure)
    }
}
