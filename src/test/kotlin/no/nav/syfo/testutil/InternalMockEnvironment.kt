package no.nav.syfo.testutil

import no.nav.syfo.application.cache.RedisStore
import no.nav.syfo.personstatus.infrastructure.clients.behandlendeenhet.BehandlendeEnhetClient
import no.nav.syfo.personstatus.infrastructure.clients.ereg.EregClient
import no.nav.syfo.personstatus.infrastructure.clients.pdl.PdlClient
import no.nav.syfo.personstatus.infrastructure.cronjob.behandlendeenhet.PersonBehandlendeEnhetCronjob
import no.nav.syfo.personstatus.infrastructure.cronjob.behandlendeenhet.PersonBehandlendeEnhetService
import no.nav.syfo.personstatus.infrastructure.cronjob.virksomhetsnavn.PersonOppfolgingstilfelleVirksomhetnavnCronjob
import no.nav.syfo.personstatus.infrastructure.cronjob.virksomhetsnavn.PersonOppfolgingstilfelleVirksomhetsnavnService
import no.nav.syfo.personstatus.PersonoversiktStatusService
import no.nav.syfo.personstatus.infrastructure.clients.aktivitetskrav.AktivitetskravClient
import no.nav.syfo.personstatus.infrastructure.clients.arbeidsuforhet.ArbeidsuforhetvurderingClient
import no.nav.syfo.personstatus.infrastructure.clients.manglendemedvirkning.ManglendeMedvirkningClient
import no.nav.syfo.personstatus.infrastructure.clients.azuread.AzureAdClient
import no.nav.syfo.personstatus.infrastructure.clients.oppfolgingsoppgave.OppfolgingsoppgaveClient
import no.nav.syfo.personstatus.infrastructure.database.repository.PersonOversiktStatusRepository

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
    private val arbeidsuforhetvurderingClient = ArbeidsuforhetvurderingClient(
        azureAdClient = azureAdClient,
        clientEnvironment = environment.clients.arbeidsuforhetvurdering,
        httpClient = externalMockEnvironment.mockHttpClient
    )
    private val manglendeMedvirkningClient = ManglendeMedvirkningClient(
        azureAdClient = azureAdClient,
        clientEnvironment = environment.clients.manglendeMedvirkning,
        httpClient = externalMockEnvironment.mockHttpClient
    )
    private val oppfolgingsoppgaveClient = OppfolgingsoppgaveClient(
        azureAdClient = azureAdClient,
        clientEnvironment = environment.clients.ishuskelapp,
        httpClient = externalMockEnvironment.mockHttpClient
    )
    private val aktivitetskravClient = AktivitetskravClient(
        azureAdClient = azureAdClient,
        clientEnvironment = environment.clients.aktivitetskrav,
        httpClient = externalMockEnvironment.mockHttpClient
    )
    private val eregClient = EregClient(
        clientEnvironment = environment.clients.ereg,
        redisStore = redisStore,
        httpClient = externalMockEnvironment.mockHttpClient
    )

    internal val personBehandlendeEnhetService = PersonBehandlendeEnhetService(
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
    val personoversiktRepository = PersonOversiktStatusRepository(database = database)
    val personoversiktStatusService = PersonoversiktStatusService(
        database = database,
        pdlClient = pdlClient,
        arbeidsuforhetvurderingClient = arbeidsuforhetvurderingClient,
        manglendeMedvirkningClient = manglendeMedvirkningClient,
        personoversiktStatusRepository = personoversiktRepository,
        oppfolgingsoppgaveClient = oppfolgingsoppgaveClient,
        aktivitetskravClient = aktivitetskravClient,
    )

    companion object {
        val instance: InternalMockEnvironment by lazy {
            InternalMockEnvironment()
        }
    }
}
