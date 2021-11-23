package no.nav.syfo

import com.typesafe.config.ConfigFactory
import io.ktor.application.*
import io.ktor.config.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.slf4j.MDCContext
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.api.apiModule
import no.nav.syfo.application.api.authentication.getWellKnown
import no.nav.syfo.application.database.databaseModule
import no.nav.syfo.db.DatabaseInterface
import no.nav.syfo.kafka.kafkaModule
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

val LOG: org.slf4j.Logger = LoggerFactory.getLogger("no.nav.syfo.AppKt")

val backgroundTasksContext = Executors.newFixedThreadPool(4).asCoroutineDispatcher() + MDCContext()

fun main() {
    val applicationState = ApplicationState()
    val environment = getEnvironment()
    val wellKnownVeilederV2 = getWellKnown(
        wellKnownUrl = environment.azureAppWellKnownUrl,
    )

    val applicationEngineEnvironment = applicationEngineEnvironment {
        log = LoggerFactory.getLogger("ktor.application")
        config = HoconApplicationConfig(ConfigFactory.load())

        connector {
            port = environment.applicationPort
        }

        module {
            databaseModule(
                applicationState = applicationState,
                environment = environment,
            )
            apiModule(
                applicationState = applicationState,
                database = database,
                environment = environment,
                wellKnownVeilederV2 = wellKnownVeilederV2,
            )
            kafkaModule(
                applicationState = applicationState,
                environment = environment,
            )
        }
    }

    val server = embeddedServer(
        environment = applicationEngineEnvironment,
        factory = Netty,
    )
    Runtime.getRuntime().addShutdownHook(
        Thread {
            server.stop(10, 10, TimeUnit.SECONDS)
        }
    )

    server.start(wait = false)
}

lateinit var database: DatabaseInterface

val Application.envKind get() = environment.config.property("ktor.environment").getString()

fun Application.isDev(block: () -> Unit) {
    if (envKind == "dev") block()
}

fun Application.isProd(block: () -> Unit) {
    if (envKind == "production") block()
}

fun isPreProd(): Boolean = getEnvVar("NAIS_CLUSTER_NAME", "dev-fss") == "dev-fss"
