package no.nav.syfo.testutil

import no.nav.syfo.application.cache.RedisStore
import no.nav.syfo.personstatus.PersonTildelingService
import no.nav.syfo.personstatus.PersonoversiktStatusService
import no.nav.syfo.personstatus.infrastructure.clients.azuread.AzureAdClient
import no.nav.syfo.personstatus.infrastructure.clients.behandlendeenhet.BehandlendeEnhetClient
import no.nav.syfo.personstatus.infrastructure.clients.ereg.EregClient
import no.nav.syfo.personstatus.infrastructure.clients.pdl.PdlClient
import no.nav.syfo.personstatus.infrastructure.cronjob.behandlendeenhet.PersonBehandlendeEnhetCronjob
import no.nav.syfo.personstatus.infrastructure.cronjob.behandlendeenhet.PersonBehandlendeEnhetService
import no.nav.syfo.personstatus.infrastructure.cronjob.virksomhetsnavn.PersonOppfolgingstilfelleVirksomhetnavnCronjob
import no.nav.syfo.personstatus.infrastructure.cronjob.virksomhetsnavn.PersonOppfolgingstilfelleVirksomhetsnavnService
import no.nav.syfo.personstatus.infrastructure.database.repository.PersonOversiktStatusRepository

class InternalMockEnvironment private constructor() {
    private val externalMockEnvironment: ExternalMockEnvironment = ExternalMockEnvironment.instance
    private val database = externalMockEnvironment.database
    private val environment = externalMockEnvironment.environment
    private val redisStore = externalMockEnvironment.redisStore

    private val azureAdClient = AzureAdClient(
        azureEnvironment = environment.azure,
        redisStore = redisStore,
        httpClient = externalMockEnvironment.mockHttpClient
    )
    private val pdlClient = PdlClient(
        azureAdClient = azureAdClient,
        clientEnvironment = environment.clients.pdl,
        httpClient = externalMockEnvironment.mockHttpClient
    )
    private val behandlendeEnhetClient = BehandlendeEnhetClient(
        azureAdClient = azureAdClient,
        clientEnvironment = environment.clients.syfobehandlendeenhet,
        httpClient = externalMockEnvironment.mockHttpClient
    )
    private val eregClient = EregClient(
        clientEnvironment = environment.clients.ereg,
        redisStore = redisStore,
        httpClient = externalMockEnvironment.mockHttpClient
    )

    val personoversiktRepository = PersonOversiktStatusRepository(database = database)
    internal val personBehandlendeEnhetService = PersonBehandlendeEnhetService(
        personoversiktStatusRepository = personoversiktRepository,
        behandlendeEnhetClient = behandlendeEnhetClient,
    )

    val personBehandlendeEnhetCronjob = PersonBehandlendeEnhetCronjob(
        personBehandlendeEnhetService = personBehandlendeEnhetService,
        intervalDelayMinutes = environment.cronjobBehandlendeEnhetIntervalDelayMinutes,
    )
    private val personOppfolgingstilfelleVirksomhetsnavnService = PersonOppfolgingstilfelleVirksomhetsnavnService(
        database = database,
        eregClient = eregClient,
    )
    val personOppfolgingstilfelleVirksomhetnavnCronjob = PersonOppfolgingstilfelleVirksomhetnavnCronjob(
        personOppfolgingstilfelleVirksomhetsnavnService = personOppfolgingstilfelleVirksomhetsnavnService,
    )
    val personoversiktStatusService = PersonoversiktStatusService(
        database = database,
        pdlClient = pdlClient,
        personoversiktStatusRepository = personoversiktRepository,
    )
    val personTildelingService = PersonTildelingService(
        personoversiktStatusRepository = personoversiktRepository,
        personBehandlendeEnhetService = personBehandlendeEnhetService,
    )

    companion object {
        val instance: InternalMockEnvironment by lazy {
            InternalMockEnvironment()
        }
    }
}
