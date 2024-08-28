package no.nav.syfo.testutil

import no.nav.syfo.ApplicationState
import no.nav.syfo.application.cache.RedisStore
import no.nav.syfo.personstatus.infrastructure.clients.pdl.PdlClient
import no.nav.syfo.personstatus.infrastructure.clients.azuread.AzureAdClient
import no.nav.syfo.testutil.mock.*

class ExternalMockEnvironment private constructor() {
    val applicationState: ApplicationState = testAppState()
    val database = TestDatabase()

    val environment = testEnvironment()
    val mockHttpClient = mockHttpClient(environment = environment)
    val wellKnownVeilederV2 = wellKnownVeilederV2Mock()

    val pdlClient = PdlClient(
        azureAdClient = AzureAdClient(
            azureEnvironment = environment.azure,
            redisStore = RedisStore(environment.redis),
            httpClient = mockHttpClient,
        ),
        clientEnvironment = environment.clients.pdl,
        httpClient = mockHttpClient
    )

    companion object {
        val instance: ExternalMockEnvironment = ExternalMockEnvironment()
    }
}
