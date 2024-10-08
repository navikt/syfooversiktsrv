package no.nav.syfo.personstatus.infrastructure.cronjob

import no.nav.syfo.ApplicationState
import no.nav.syfo.Environment
import no.nav.syfo.launchBackgroundTask
import no.nav.syfo.application.cache.RedisStore
import no.nav.syfo.personstatus.infrastructure.database.DatabaseInterface
import no.nav.syfo.personstatus.infrastructure.clients.azuread.AzureAdClient
import no.nav.syfo.personstatus.infrastructure.clients.ereg.EregClient
import no.nav.syfo.personstatus.infrastructure.clients.veiledertilgang.VeilederTilgangskontrollClient
import no.nav.syfo.personstatus.infrastructure.cronjob.behandlendeenhet.PersonBehandlendeEnhetCronjob
import no.nav.syfo.personstatus.infrastructure.cronjob.behandlendeenhet.PersonBehandlendeEnhetService
import no.nav.syfo.personstatus.infrastructure.cronjob.leaderelection.LeaderPodClient
import no.nav.syfo.personstatus.infrastructure.cronjob.preloadcache.PreloadCacheCronjob
import no.nav.syfo.personstatus.infrastructure.cronjob.reaper.ReaperCronjob
import no.nav.syfo.personstatus.infrastructure.cronjob.reaper.ReaperService
import no.nav.syfo.personstatus.infrastructure.cronjob.virksomhetsnavn.PersonOppfolgingstilfelleVirksomhetnavnCronjob
import no.nav.syfo.personstatus.infrastructure.cronjob.virksomhetsnavn.PersonOppfolgingstilfelleVirksomhetsnavnService

fun launchCronjobModule(
    applicationState: ApplicationState,
    database: DatabaseInterface,
    environment: Environment,
    redisStore: RedisStore,
    azureAdClient: AzureAdClient,
    personBehandlendeEnhetService: PersonBehandlendeEnhetService,
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
        istilgangskontrollEnv = environment.clients.istilgangskontroll,
    )
    val preloadCacheCronjob = PreloadCacheCronjob(
        database = database,
        tilgangskontrollClient = tilgangskontrollClient,
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
