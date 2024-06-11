package no.nav.syfo.testutil

import io.ktor.server.application.*
import no.nav.syfo.application.cache.RedisStore
import no.nav.syfo.client.pdl.PdlClient
import no.nav.syfo.personstatus.PersonoversiktStatusService
import no.nav.syfo.personstatus.api.v2.apiModule
import no.nav.syfo.personstatus.infrastructure.ArbeidsuforhetvurderingClient
import no.nav.syfo.personstatus.infrastructure.clients.azuread.AzureAdClient
import no.nav.syfo.personstatus.infrastructure.database.repository.PersonOversiktStatusRepository

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
    val arbeidsuforhetvurderingClient = ArbeidsuforhetvurderingClient(
        azureAdClient = azureAdClient,
        clientEnvironment = externalMockEnvironment.environment.clients.arbeidsuforhetvurdering,
    )
    val personoversiktRepository = PersonOversiktStatusRepository(database = externalMockEnvironment.database)
    val personoversiktStatusService = PersonoversiktStatusService(
        database = externalMockEnvironment.database,
        pdlClient = pdlClient,
        personoversiktStatusRepository = personoversiktRepository,
        arbeidsuforhetvurderingClient = arbeidsuforhetvurderingClient,
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
