package no.nav.syfo.testutil

import io.ktor.server.netty.*
import no.nav.syfo.ApplicationState
import no.nav.syfo.application.cache.RedisStore
import no.nav.syfo.personstatus.infrastructure.clients.pdl.PdlClient
import no.nav.syfo.personstatus.infrastructure.clients.azuread.AzureAdClient
import no.nav.syfo.testutil.mock.*
import redis.clients.jedis.DefaultJedisClientConfig
import redis.clients.jedis.HostAndPort
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig

class ExternalMockEnvironment private constructor() {
    val applicationState: ApplicationState = testAppState()
    val database = TestDatabase()
    val environment = testEnvironment()
    val redisConfig = environment.redisConfig
    var redisStore = RedisStore(
        JedisPool(
            JedisPoolConfig(),
            HostAndPort(redisConfig.host, redisConfig.port),
            DefaultJedisClientConfig.builder()
                .ssl(redisConfig.ssl)
                .password(redisConfig.redisPassword)
                .build()
        )
    )

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

    companion object {
        val instance: ExternalMockEnvironment = ExternalMockEnvironment()
    }
}
