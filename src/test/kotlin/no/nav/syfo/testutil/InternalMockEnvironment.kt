package no.nav.syfo.testutil

import no.nav.syfo.application.PersonBehandlendeEnhetService
import no.nav.syfo.application.PersonoversiktStatusService
import no.nav.syfo.infrastructure.database.TransactionManager
import no.nav.syfo.infrastructure.clients.azuread.AzureAdClient
import no.nav.syfo.infrastructure.clients.behandlendeenhet.BehandlendeEnhetClient
import no.nav.syfo.infrastructure.clients.ereg.EregClient
import no.nav.syfo.infrastructure.clients.pdl.PdlClient
import no.nav.syfo.infrastructure.cronjob.behandlendeenhet.PersonBehandlendeEnhetCronjob
import no.nav.syfo.infrastructure.cronjob.virksomhetsnavn.PersonOppfolgingstilfelleVirksomhetnavnCronjob
import no.nav.syfo.infrastructure.cronjob.virksomhetsnavn.PersonOppfolgingstilfelleVirksomhetsnavnService

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
        pdlClient = pdlClient,
        personoversiktStatusRepository = personoversiktRepository,
        transactionManager = TransactionManager(database),
    )

    companion object {
        val instance: InternalMockEnvironment by lazy {
            InternalMockEnvironment()
        }
    }
}
