package no.nav.syfo.testutil

import io.ktor.server.netty.*
import no.nav.syfo.ApplicationState
import no.nav.syfo.application.cache.RedisStore
import no.nav.syfo.personstatus.infrastructure.clients.pdl.PdlClient
import no.nav.syfo.personstatus.infrastructure.clients.azuread.AzureAdClient
import no.nav.syfo.testutil.mock.*

class ExternalMockEnvironment private constructor() {
    val applicationState: ApplicationState = testAppState()
    val database = TestDatabase()
    lateinit var redisStore: RedisStore

    val environment = testEnvironment()
    val mockHttpClient = mockHttpClient(environment = environment)
    val wellKnownVeilederV2 = wellKnownVeilederV2Mock()
    val pdlClient = PdlClient(
        azureAdClient = AzureAdClient(
            azureEnvironment = environment.azure,
            redisStore = redisStore,
            httpClient = mockHttpClient,
        ),
        clientEnvironment = environment.clients.pdl,
        httpClient = mockHttpClient
    )
    val redisServer = testRedis(
        port = environment.redisConfig.redisUri.port,
        secret = environment.redisConfig.redisPassword,
    )

    companion object {
        val instance: ExternalMockEnvironment = ExternalMockEnvironment()
    }
}

fun ExternalMockEnvironment.startExternalMocks() {
    this.redisServer.start()
}

fun ExternalMockEnvironment.stopExternalMocks() {
    this.redisServer.stop()
}


