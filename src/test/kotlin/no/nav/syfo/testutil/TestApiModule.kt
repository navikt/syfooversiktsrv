package no.nav.syfo.testutil

import io.ktor.server.application.*
import io.mockk.mockk
import no.nav.syfo.application.cache.RedisStore
import no.nav.syfo.personstatus.infrastructure.clients.pdl.PdlClient
import no.nav.syfo.personstatus.PersonoversiktStatusService
import no.nav.syfo.personstatus.api.v2.apiModule
import no.nav.syfo.personstatus.infrastructure.clients.arbeidsuforhet.ArbeidsuforhetvurderingClient
import no.nav.syfo.personstatus.infrastructure.clients.arbeidsuforhet.ManglendeMedvirkningClient
import no.nav.syfo.personstatus.infrastructure.clients.oppfolgingsoppgave.OppfolgingsoppgaveClient
import no.nav.syfo.personstatus.infrastructure.clients.azuread.AzureAdClient
import no.nav.syfo.personstatus.infrastructure.clients.veiledertilgang.VeilederTilgangskontrollClient
import no.nav.syfo.personstatus.infrastructure.database.repository.PersonOversiktStatusRepository

fun Application.testApiModule(
    externalMockEnvironment: ExternalMockEnvironment,
) {
    val redisStore = RedisStore(externalMockEnvironment.environment.redis)
    val azureAdClient = AzureAdClient(
        azureEnvironment = externalMockEnvironment.environment.azure,
        redisStore = redisStore,
        httpClient = externalMockEnvironment.mockHttpClient
    )

    val pdlClient = PdlClient(
        azureAdClient = azureAdClient,
        clientEnvironment = externalMockEnvironment.environment.clients.pdl,
        httpClient = externalMockEnvironment.mockHttpClient
    )
    val arbeidsuforhetvurderingClient = ArbeidsuforhetvurderingClient(
        azureAdClient = azureAdClient,
        clientEnvironment = externalMockEnvironment.environment.clients.arbeidsuforhetvurdering,
        httpClient = externalMockEnvironment.mockHttpClient
    )
    val manglendeMedvirkningClient = ManglendeMedvirkningClient(
        azureAdClient = azureAdClient,
        clientEnvironment = externalMockEnvironment.environment.clients.manglendeMedvirkning,
        httpClient = externalMockEnvironment.mockHttpClient
    )
    val oppfolgingsoppgaveClient = OppfolgingsoppgaveClient(
        azureAdClient = azureAdClient,
        clientEnvironment = externalMockEnvironment.environment.clients.ishuskelapp,
        httpClient = externalMockEnvironment.mockHttpClient
    )
    val veilederTilgangskontrollClient = VeilederTilgangskontrollClient(
        azureAdClient = azureAdClient,
        istilgangskontrollEnv = externalMockEnvironment.environment.clients.istilgangskontroll,
        httpClient = externalMockEnvironment.mockHttpClient
    )
    val personoversiktRepository = PersonOversiktStatusRepository(database = externalMockEnvironment.database)
    val personoversiktStatusService = PersonoversiktStatusService(
        database = externalMockEnvironment.database,
        pdlClient = pdlClient,
        personoversiktStatusRepository = personoversiktRepository,
        arbeidsuforhetvurderingClient = arbeidsuforhetvurderingClient,
        manglendeMedvirkningClient = manglendeMedvirkningClient,
        oppfolgingsoppgaveClient = oppfolgingsoppgaveClient,
        aktivitetskravClient = mockk(relaxed = true),
    )

    this.apiModule(
        applicationState = externalMockEnvironment.applicationState,
        database = externalMockEnvironment.database,
        environment = externalMockEnvironment.environment,
        wellKnownVeilederV2 = externalMockEnvironment.wellKnownVeilederV2,
        tilgangskontrollClient = veilederTilgangskontrollClient,
        personoversiktStatusService = personoversiktStatusService,
    )
}
