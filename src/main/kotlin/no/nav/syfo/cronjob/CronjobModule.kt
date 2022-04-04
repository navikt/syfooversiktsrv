package no.nav.syfo.cronjob

import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.Environment
import no.nav.syfo.application.backgroundtask.launchBackgroundTask
import no.nav.syfo.cronjob.virksomhetsnavn.PersonOppfolgingstilfelleVirksomhetnavnCronjob
import no.nav.syfo.cronjob.leaderelection.LeaderPodClient

fun launchCronjobModule(
    applicationState: ApplicationState,
    environment: Environment,
) {
    val leaderPodClient = LeaderPodClient(
        electorPath = environment.electorPath
    )
    val cronjobRunner = CronjobRunner(
        applicationState = applicationState,
        leaderPodClient = leaderPodClient
    )
    val personOppfolgingstilfelleVirksomhetnavnCronjob = PersonOppfolgingstilfelleVirksomhetnavnCronjob()

    launchBackgroundTask(
        applicationState = applicationState,
    ) {
        cronjobRunner.start(
            cronjob = personOppfolgingstilfelleVirksomhetnavnCronjob
        )
    }
}
