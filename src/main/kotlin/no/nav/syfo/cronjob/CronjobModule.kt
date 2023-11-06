package no.nav.syfo.cronjob

import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.Environment
import no.nav.syfo.application.backgroundtask.launchBackgroundTask
import no.nav.syfo.application.cache.RedisStore
import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.client.behandlendeenhet.BehandlendeEnhetClient
import no.nav.syfo.client.ereg.EregClient
import no.nav.syfo.client.veiledertilgang.VeilederTilgangskontrollClient
import no.nav.syfo.cronjob.behandlendeenhet.PersonBehandlendeEnhetCronjob
import no.nav.syfo.cronjob.behandlendeenhet.PersonBehandlendeEnhetService
import no.nav.syfo.cronjob.leaderelection.LeaderPodClient
import no.nav.syfo.cronjob.preloadcache.PreloadCacheCronjob
import no.nav.syfo.cronjob.reaper.ReaperCronjob
import no.nav.syfo.cronjob.reaper.ReaperService
import no.nav.syfo.cronjob.virksomhetsnavn.PersonOppfolgingstilfelleVirksomhetnavnCronjob
import no.nav.syfo.cronjob.virksomhetsnavn.PersonOppfolgingstilfelleVirksomhetsnavnService

fun launchCronjobModule(
    applicationState: ApplicationState,
    database: DatabaseInterface,
    environment: Environment,
    redisStore: RedisStore,
    azureAdClient: AzureAdClient,
) {
    val eregClient = EregClient(
        clientEnvironment = environment.clients.ereg,
        redisStore = redisStore,
    )
    val personOppfolgingstilfelleVirksomhetsnavnService = PersonOppfolgingstilfelleVirksomhetsnavnService(
        database = database,
        eregClient = eregClient,
    )
    val personOppfolgingstilfelleVirksomhetnavnCronjob = PersonOppfolgingstilfelleVirksomhetnavnCronjob(
        personOppfolgingstilfelleVirksomhetsnavnService = personOppfolgingstilfelleVirksomhetsnavnService,
    )

    val behandlendeEnhetClient = BehandlendeEnhetClient(
        azureAdClient = azureAdClient,
        clientEnvironment = environment.clients.syfobehandlendeenhet,
    )
    val personBehandlendeEnhetService = PersonBehandlendeEnhetService(
        database = database,
        behandlendeEnhetClient = behandlendeEnhetClient,
    )
    val personBehandlendeEnhetCronjob = PersonBehandlendeEnhetCronjob(
        personBehandlendeEnhetService = personBehandlendeEnhetService,
        intervalDelayMinutes = environment.cronjobBehandlendeEnhetIntervalDelayMinutes,
    )

    val reaperService = ReaperService(
        database = database,
    )
    val reaperCronjob = ReaperCronjob(
        reaperService = reaperService,
    )

    val tilgangskontrollClient = VeilederTilgangskontrollClient(
        azureAdClient = azureAdClient,
        syfotilgangskontrollEnv = environment.clients.syfotilgangskontroll,
        istilgangskontrollEnv = environment.clients.istilgangskontroll,
    )
    val preloadCacheCronjob = PreloadCacheCronjob(
        database = database,
        tilgangskontrollClient = tilgangskontrollClient,
        arenaCutoff = environment.arenaCutoff,
    )

    val cronjobRunner = CronjobRunner(
        applicationState = applicationState,
        leaderPodClient = LeaderPodClient(
            electorPath = environment.electorPath,
        ),
    )

    listOf(
        personOppfolgingstilfelleVirksomhetnavnCronjob,
        personBehandlendeEnhetCronjob,
        reaperCronjob,
        preloadCacheCronjob,
    ).forEach { cronjob ->
        launchBackgroundTask(
            applicationState = applicationState,
        ) {
            cronjobRunner.start(cronjob)
        }
    }
}
