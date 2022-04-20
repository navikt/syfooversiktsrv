package no.nav.syfo.cronjob

import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.Environment
import no.nav.syfo.application.backgroundtask.launchBackgroundTask
import no.nav.syfo.application.cache.RedisStore
import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.client.behandlendeenhet.BehandlendeEnhetClient
import no.nav.syfo.client.ereg.EregClient
import no.nav.syfo.cronjob.behandlendeenhet.PersonBehandlendeEnhetCronjob
import no.nav.syfo.cronjob.behandlendeenhet.PersonBehandlendeEnhetService
import no.nav.syfo.cronjob.leaderelection.LeaderPodClient
import no.nav.syfo.cronjob.virksomhetsnavn.PersonOppfolgingstilfelleVirksomhetnavnCronjob
import no.nav.syfo.cronjob.virksomhetsnavn.PersonOppfolgingstilfelleVirksomhetsnavnService

fun launchCronjobModule(
    applicationState: ApplicationState,
    database: DatabaseInterface,
    environment: Environment,
) {
    val redisStore = RedisStore(
        redisEnvironment = environment.redis,
    )

    val azureAdClient = AzureAdClient(
        azureEnviroment = environment.azure,
        redisStore = redisStore,
    )

    val eregClient = EregClient(
        azureAdClient = azureAdClient,
        isproxyClientId = environment.isproxyClientId,
        baseUrl = environment.isproxyUrl,
        redisStore = redisStore,
    )

    val leaderPodClient = LeaderPodClient(
        electorPath = environment.electorPath,
    )
    val cronjobRunner = CronjobRunner(
        applicationState = applicationState,
        leaderPodClient = leaderPodClient,
    )
    val personOppfolgingstilfelleVirksomhetsnavnService = PersonOppfolgingstilfelleVirksomhetsnavnService(
        database = database,
        eregClient = eregClient,
    )
    val personOppfolgingstilfelleVirksomhetnavnCronjob = PersonOppfolgingstilfelleVirksomhetnavnCronjob(
        personOppfolgingstilfelleVirksomhetsnavnService = personOppfolgingstilfelleVirksomhetsnavnService,
    )

    if (environment.personOppfolgingstilfelleVirksomhetsnavnCronjobEnabled) {
        launchBackgroundTask(
            applicationState = applicationState,
        ) {
            cronjobRunner.start(
                cronjob = personOppfolgingstilfelleVirksomhetnavnCronjob,
            )
        }
    }

    val behandlendeEnhetClient = BehandlendeEnhetClient(
        azureAdClient = azureAdClient,
        baseUrl = environment.syfobehandlendeenhetUrl,
        syfobehandlendeenhetClientId = environment.syfobehandlendeenhetClientId
    )

    val personBehandlendeEnhetService = PersonBehandlendeEnhetService(
        database = database,
        behandlendeEnhetClient = behandlendeEnhetClient,
    )

    val personBehandlendeEnhetCronjob = PersonBehandlendeEnhetCronjob(
        personBehandlendeEnhetService = personBehandlendeEnhetService,
    )

    if (environment.personBehandlendeEnhetCronjobEnabled) {
        launchBackgroundTask(
            applicationState = applicationState,
        ) {
            cronjobRunner.start(
                cronjob = personBehandlendeEnhetCronjob,
            )
        }
    }
}
