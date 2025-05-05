package no.nav.syfo.testutil

import no.nav.syfo.personstatus.application.PersonBehandlendeEnhetService
import no.nav.syfo.personstatus.application.PersonoversiktStatusService
import no.nav.syfo.personstatus.infrastructure.clients.azuread.AzureAdClient
import no.nav.syfo.personstatus.infrastructure.clients.behandlendeenhet.BehandlendeEnhetClient
import no.nav.syfo.personstatus.infrastructure.clients.ereg.EregClient
import no.nav.syfo.personstatus.infrastructure.clients.pdl.PdlClient
import no.nav.syfo.personstatus.infrastructure.cronjob.behandlendeenhet.PersonBehandlendeEnhetCronjob
import no.nav.syfo.personstatus.infrastructure.cronjob.virksomhetsnavn.PersonOppfolgingstilfelleVirksomhetnavnCronjob
import no.nav.syfo.personstatus.infrastructure.cronjob.virksomhetsnavn.PersonOppfolgingstilfelleVirksomhetsnavnService

class InternalMockEnvironment private constructor() {
    private val externalMockEnvironment: ExternalMockEnvironment = ExternalMockEnvironment.instance
    private val database = externalMockEnvironment.database
    private val environment = externalMockEnvironment.environment
    private val redisStore = externalMockEnvironment.valkeyStore

    private val azureAdClient = AzureAdClient(
        azureEnvironment = environment.azure,
        valkeyStore = redisStore,
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
        valkeyStore = redisStore,
        httpClient = externalMockEnvironment.mockHttpClient
    )

    val personoversiktRepository = externalMockEnvironment.personOversiktStatusRepository
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

    companion object {
        val instance: InternalMockEnvironment by lazy {
            InternalMockEnvironment()
        }
    }
}
