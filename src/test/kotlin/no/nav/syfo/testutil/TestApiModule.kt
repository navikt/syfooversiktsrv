package no.nav.syfo.testutil

import io.ktor.server.application.*
import no.nav.syfo.application.api.apiModule
import no.nav.syfo.application.cache.RedisStore
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.client.pdl.PdlClient
import no.nav.syfo.personstatus.PersonoversiktStatusService

fun Application.testApiModule(
    externalMockEnvironment: ExternalMockEnvironment
) {
    val redisStore = RedisStore(externalMockEnvironment.environment.redis)
    val azureAdClient = AzureAdClient(
        azureEnvironment = externalMockEnvironment.environment.azure,
        redisStore = redisStore,
    )

    val pdlClient = PdlClient(
        azureAdClient = azureAdClient,
        clientEnvironment = externalMockEnvironment.environment.clients.pdl,
    )

    val personoversiktStatusService = PersonoversiktStatusService(
        database = externalMockEnvironment.database,
        pdlClient = pdlClient,
    )

    this.apiModule(
        applicationState = externalMockEnvironment.applicationState,
        database = externalMockEnvironment.database,
        environment = externalMockEnvironment.environment,
        wellKnownVeilederV2 = externalMockEnvironment.wellKnownVeilederV2,
        azureAdClient = azureAdClient,
        personoversiktStatusService = personoversiktStatusService,
    )
}
