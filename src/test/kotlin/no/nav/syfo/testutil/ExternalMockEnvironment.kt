package no.nav.syfo.testutil

import no.nav.syfo.ApplicationState
import no.nav.syfo.application.cache.ValkeyStore
import no.nav.syfo.personstatus.application.OppfolgingstilfelleService
import no.nav.syfo.personstatus.application.PersonoversiktStatusService
import no.nav.syfo.personstatus.infrastructure.clients.azuread.AzureAdClient
import no.nav.syfo.personstatus.infrastructure.clients.pdl.PdlClient
import no.nav.syfo.personstatus.infrastructure.clients.veileder.VeilederClient
import no.nav.syfo.personstatus.infrastructure.database.repository.PersonOversiktStatusRepository
import no.nav.syfo.testutil.mock.*
import redis.clients.jedis.DefaultJedisClientConfig
import redis.clients.jedis.HostAndPort
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig

class ExternalMockEnvironment private constructor() {
    val applicationState: ApplicationState = testAppState()
    val database = TestDatabase()
    val environment = testEnvironment()
    val redisConfig = environment.valkeyConfig
    var valkeyStore = ValkeyStore(
        JedisPool(
            JedisPoolConfig(),
            HostAndPort(redisConfig.host, redisConfig.port),
            DefaultJedisClientConfig.builder()
                .ssl(redisConfig.ssl)
                .password(redisConfig.valkeyPassword)
                .build()
        )
    )

    val mockHttpClient = mockHttpClient(environment = environment)
    val wellKnownVeilederV2 = wellKnownVeilederV2Mock()

    val personOversiktStatusRepository = PersonOversiktStatusRepository(database = database)

    private val azureAdClient = AzureAdClient(
        azureEnvironment = environment.azure,
        valkeyStore = valkeyStore,
        httpClient = mockHttpClient,
    )
    private val pdlClient = PdlClient(
        azureAdClient = azureAdClient,
        clientEnvironment = environment.clients.pdl,
        httpClient = mockHttpClient
    )
    private val veilederClient = VeilederClient(
        azureAdClient = azureAdClient,
        clientEnvironment = environment.clients.syfoveileder,
        httpClient = mockHttpClient
    )

    val personoversiktStatusService = PersonoversiktStatusService(
        database = database,
        pdlClient = pdlClient,
        personoversiktStatusRepository = personOversiktStatusRepository,
    )
    val oppfolgingstilfelleService = OppfolgingstilfelleService(
        pdlClient = pdlClient,
        personOversiktStatusRepository = personOversiktStatusRepository,
        veilederClient = veilederClient
    )

    companion object {
        val instance: ExternalMockEnvironment = ExternalMockEnvironment()
    }
}
