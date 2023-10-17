package no.nav.syfo.testutil

import no.nav.syfo.application.cache.RedisStore
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.client.behandlendeenhet.BehandlendeEnhetClient
import no.nav.syfo.client.ereg.EregClient
import no.nav.syfo.client.pdl.PdlClient
import no.nav.syfo.cronjob.behandlendeenhet.PersonBehandlendeEnhetCronjob
import no.nav.syfo.cronjob.behandlendeenhet.PersonBehandlendeEnhetService
import no.nav.syfo.cronjob.virksomhetsnavn.PersonOppfolgingstilfelleVirksomhetnavnCronjob
import no.nav.syfo.cronjob.virksomhetsnavn.PersonOppfolgingstilfelleVirksomhetsnavnService
import no.nav.syfo.personstatus.PersonoversiktStatusService

class InternalMockEnvironment private constructor() {
    private val externalMockEnvironment: ExternalMockEnvironment = ExternalMockEnvironment.instance
    private val database = externalMockEnvironment.database
    private val environment = externalMockEnvironment.environment

    private val redisStore = RedisStore(
        redisEnvironment = environment.redis,
    )
    private val azureAdClient = AzureAdClient(
        azureEnvironment = environment.azure,
        redisStore = redisStore,
    )
    private val pdlClient = PdlClient(
        azureAdClient = azureAdClient,
        clientEnvironment = environment.clients.pdl,
    )
    private val behandlendeEnhetClient = BehandlendeEnhetClient(
        azureAdClient = azureAdClient,
        clientEnvironment = environment.clients.syfobehandlendeenhet,
    )
    private val eregClient = EregClient(
        clientEnvironment = environment.clients.ereg,
        redisStore = redisStore,
    )

    private val personBehandlendeEnhetService = PersonBehandlendeEnhetService(
        database = database,
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
    )

    companion object {
        val instance: InternalMockEnvironment by lazy {
            InternalMockEnvironment()
        }
    }
}
