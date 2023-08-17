package no.nav.syfo

import com.typesafe.config.ConfigFactory
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.Environment
import no.nav.syfo.application.api.apiModule
import no.nav.syfo.application.api.authentication.getWellKnown
import no.nav.syfo.application.cache.RedisStore
import no.nav.syfo.application.database.database
import no.nav.syfo.application.database.databaseModule
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.client.pdl.PdlClient
import no.nav.syfo.cronjob.launchCronjobModule
import no.nav.syfo.kafka.launchKafkaModule
import no.nav.syfo.personstatus.PersonoversiktStatusService
import org.slf4j.LoggerFactory
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

    val redisStore = RedisStore(
        redisEnvironment = environment.redis,
    )

    val azureAdClient = AzureAdClient(
        azureEnvironment = environment.azure,
        redisStore = redisStore,
    )

    val pdlClient = PdlClient(
        azureAdClient = azureAdClient,
        clientEnvironment = environment.clients.pdl,
    )

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
            personoversiktStatusService = PersonoversiktStatusService(
                database = database,
                pdlClient = pdlClient,
            )
            apiModule(
                applicationState = applicationState,
                database = database,
                environment = environment,
                wellKnownVeilederV2 = wellKnownVeilederV2,
                azureAdClient = azureAdClient,
                personoversiktStatusService = personoversiktStatusService,
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
        )
        launchCronjobModule(
            applicationState = applicationState,
            database = database,
            environment = environment,
            redisStore = redisStore,
            azureAdClient = azureAdClient,
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
