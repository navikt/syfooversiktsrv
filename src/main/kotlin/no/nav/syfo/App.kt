package no.nav.syfo

import com.typesafe.config.ConfigFactory
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.syfo.application.cache.RedisStore
import no.nav.syfo.personstatus.PersonoversiktOppgaverService
import no.nav.syfo.personstatus.PersonoversiktStatusService
import no.nav.syfo.personstatus.api.v2.apiModule
import no.nav.syfo.personstatus.api.v2.auth.getWellKnown
import no.nav.syfo.personstatus.infrastructure.clients.aktivitetskrav.AktivitetskravClient
import no.nav.syfo.personstatus.infrastructure.clients.arbeidsuforhet.ArbeidsuforhetvurderingClient
import no.nav.syfo.personstatus.infrastructure.clients.manglendemedvirkning.ManglendeMedvirkningClient
import no.nav.syfo.personstatus.infrastructure.clients.azuread.AzureAdClient
import no.nav.syfo.personstatus.infrastructure.clients.behandlendeenhet.BehandlendeEnhetClient
import no.nav.syfo.personstatus.infrastructure.clients.meroppfolging.MerOppfolgingClient
import no.nav.syfo.personstatus.infrastructure.clients.oppfolgingsoppgave.OppfolgingsoppgaveClient
import no.nav.syfo.personstatus.infrastructure.clients.pdl.PdlClient
import no.nav.syfo.personstatus.infrastructure.clients.veiledertilgang.VeilederTilgangskontrollClient
import no.nav.syfo.personstatus.infrastructure.cronjob.behandlendeenhet.PersonBehandlendeEnhetService
import no.nav.syfo.personstatus.infrastructure.cronjob.launchCronjobModule
import no.nav.syfo.personstatus.infrastructure.database.database
import no.nav.syfo.personstatus.infrastructure.database.databaseModule
import no.nav.syfo.personstatus.infrastructure.database.repository.PersonOversiktStatusRepository
import no.nav.syfo.personstatus.infrastructure.kafka.launchKafkaModule
import org.slf4j.LoggerFactory
import redis.clients.jedis.DefaultJedisClientConfig
import redis.clients.jedis.HostAndPort
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig
import java.util.concurrent.TimeUnit

const val applicationPort = 8080

fun main() {
    val applicationState = ApplicationState()
    val logger = LoggerFactory.getLogger("ktor.application")
    logger.info("syfooversiktsrv starting with java version: " + Runtime.version())
    val environment = Environment()

    val wellKnownVeilederV2 = getWellKnown(
        wellKnownUrl = environment.azure.appWellKnownUrl,
    )

    val redisConfig = environment.redisConfig
    val redisStore = RedisStore(
        JedisPool(
            JedisPoolConfig(),
            HostAndPort(redisConfig.host, redisConfig.port),
            DefaultJedisClientConfig.builder()
                .ssl(redisConfig.ssl)
                .user(redisConfig.redisUsername)
                .password(redisConfig.redisPassword)
                .database(redisConfig.redisDB)
                .build()
        )
    )

    val azureAdClient = AzureAdClient(
        azureEnvironment = environment.azure,
        redisStore = redisStore,
    )

    val pdlClient = PdlClient(
        azureAdClient = azureAdClient,
        clientEnvironment = environment.clients.pdl,
    )
    val behandlendeEnhetClient = BehandlendeEnhetClient(
        azureAdClient = azureAdClient,
        clientEnvironment = environment.clients.syfobehandlendeenhet,
    )
    val arbeidsuforhetvurderingClient = ArbeidsuforhetvurderingClient(
        azureAdClient = azureAdClient,
        clientEnvironment = environment.clients.arbeidsuforhetvurdering,
    )
    val oppfolgingsoppgaveClient = OppfolgingsoppgaveClient(
        azureAdClient = azureAdClient,
        clientEnvironment = environment.clients.ishuskelapp,
    )
    val aktivitetskravClient = AktivitetskravClient(
        azureAdClient = azureAdClient,
        clientEnvironment = environment.clients.aktivitetskrav,
    )
    val manglendeMedvirkningClient = ManglendeMedvirkningClient(
        azureAdClient = azureAdClient,
        clientEnvironment = environment.clients.manglendeMedvirkning,
    )
    val merOppfolgingClient = MerOppfolgingClient(
        azureAdClient = azureAdClient,
        clientEnvironment = environment.clients.ismeroppfolging,
    )
    val veilederTilgangskontrollClient = VeilederTilgangskontrollClient(
        azureAdClient = azureAdClient,
        istilgangskontrollEnv = environment.clients.istilgangskontroll,
    )

    lateinit var personBehandlendeEnhetService: PersonBehandlendeEnhetService
    lateinit var personoversiktStatusService: PersonoversiktStatusService

    val applicationEngineEnvironment = applicationEngineEnvironment {
        log = logger
        config = HoconApplicationConfig(ConfigFactory.load())

        connector {
            port = applicationPort
        }

        module {
            databaseModule(
                databaseEnvironment = environment.database,
            )
            val personoversiktStatusRepository = PersonOversiktStatusRepository(database = database)
            personoversiktStatusService = PersonoversiktStatusService(
                database = database,
                pdlClient = pdlClient,
                personoversiktStatusRepository = personoversiktStatusRepository,
            )
            personBehandlendeEnhetService = PersonBehandlendeEnhetService(
                personoversiktStatusRepository = personoversiktStatusRepository,
                behandlendeEnhetClient = behandlendeEnhetClient,
            )
            apiModule(
                applicationState = applicationState,
                database = database,
                environment = environment,
                wellKnownVeilederV2 = wellKnownVeilederV2,
                personoversiktStatusService = personoversiktStatusService,
                tilgangskontrollClient = veilederTilgangskontrollClient,
                personoversiktOppgaverService = PersonoversiktOppgaverService(
                    oppfolgingsoppgaveClient = oppfolgingsoppgaveClient,
                    aktivitetskravClient = aktivitetskravClient,
                    manglendeMedvirkningClient = manglendeMedvirkningClient,
                    arbeidsuforhetvurderingClient = arbeidsuforhetvurderingClient,
                    merOppfolgingClient = merOppfolgingClient,
                ),
                personBehandlendeEnhetService = personBehandlendeEnhetService,
                personoversiktStatusRepository = personoversiktStatusRepository,
            )
        }
    }

    val server = embeddedServer(
        environment = applicationEngineEnvironment,
        factory = Netty,
    ) {
        connectionGroupSize = 8
        workerGroupSize = 8
        callGroupSize = 16
    }

    applicationEngineEnvironment.monitor.subscribe(ApplicationStarted) {
        applicationState.ready = true
        logger.info("Application is ready, running Java VM ${Runtime.version()}")
        launchKafkaModule(
            applicationState = applicationState,
            environment = environment,
            azureAdClient = azureAdClient,
            personoversiktStatusService = personoversiktStatusService,
            personBehandlendeEnhetService = personBehandlendeEnhetService,
        )
        launchCronjobModule(
            applicationState = applicationState,
            database = database,
            environment = environment,
            redisStore = redisStore,
            azureAdClient = azureAdClient,
            personBehandlendeEnhetService = personBehandlendeEnhetService,
        )
    }

    Runtime.getRuntime().addShutdownHook(
        Thread {
            server.stop(10, 10, TimeUnit.SECONDS)
        }
    )

    server.start(wait = true)
}

val Application.envKind get() = environment.config.property("ktor.environment").getString()

fun Application.isDev(block: () -> Unit) {
    if (envKind == "dev") block()
}

fun Application.isProd(block: () -> Unit) {
    if (envKind == "production") block()
}
