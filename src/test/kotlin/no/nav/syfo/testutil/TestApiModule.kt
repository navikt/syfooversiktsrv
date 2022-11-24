package no.nav.syfo.testutil

import io.ktor.server.application.*
import no.nav.syfo.application.api.apiModule
import no.nav.syfo.application.cache.RedisStore
import no.nav.syfo.client.azuread.AzureAdClient

fun Application.testApiModule(
    externalMockEnvironment: ExternalMockEnvironment
) {
    val redisStore = RedisStore(externalMockEnvironment.environment.redis)
    val azureAdClient = AzureAdClient(
        azureEnvironment = externalMockEnvironment.environment.azure,
        redisStore = redisStore,
    )

    this.apiModule(
        applicationState = externalMockEnvironment.applicationState,
        database = externalMockEnvironment.database,
        environment = externalMockEnvironment.environment,
        wellKnownVeilederV2 = externalMockEnvironment.wellKnownVeilederV2,
        redisStore = redisStore,
        azureAdClient = azureAdClient,
    )
}
