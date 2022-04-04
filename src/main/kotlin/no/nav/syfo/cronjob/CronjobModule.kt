package no.nav.syfo.cronjob

import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.Environment
import no.nav.syfo.application.backgroundtask.launchBackgroundTask
import no.nav.syfo.application.cache.RedisStore
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.client.ereg.EregClient
import no.nav.syfo.cronjob.leaderelection.LeaderPodClient
import no.nav.syfo.cronjob.virksomhetsnavn.PersonOppfolgingstilfelleVirksomhetnavnCronjob
import redis.clients.jedis.*

fun launchCronjobModule(
    applicationState: ApplicationState,
    environment: Environment,
) {
    val redisStore = RedisStore(
        jedisPool = JedisPool(
            JedisPoolConfig(),
            environment.redisHost,
            environment.redisPort,
            Protocol.DEFAULT_TIMEOUT,
            environment.redisSecret,
        ),
    )

    val azureAdClient = AzureAdClient(
        aadAppClient = environment.azureAppClientId,
        aadAppSecret = environment.azureAppClientSecret,
        aadTokenEndpoint = environment.azureTokenEndpoint,
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
    val personOppfolgingstilfelleVirksomhetnavnCronjob = PersonOppfolgingstilfelleVirksomhetnavnCronjob(
        eregClient = eregClient,
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
}
