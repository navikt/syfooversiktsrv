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
import no.nav.syfo.application.database.database
import no.nav.syfo.application.database.databaseModule
import no.nav.syfo.cronjob.launchCronjobModule
import no.nav.syfo.kafka.launchKafkaModule
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

val LOG: org.slf4j.Logger = LoggerFactory.getLogger("no.nav.syfo.AppKt")

const val applicationPort = 8080

fun main() {
    val applicationState = ApplicationState()
    val environment = Environment()
    val wellKnownVeilederV2 = getWellKnown(
        wellKnownUrl = environment.azure.appWellKnownUrl,
    )

    val applicationEngineEnvironment = applicationEngineEnvironment {
        log = LoggerFactory.getLogger("ktor.application")
        config = HoconApplicationConfig(ConfigFactory.load())

        connector {
            port = applicationPort
        }

        module {
            databaseModule(
                databaseEnvironment = environment.database,
            )
            apiModule(
                applicationState = applicationState,
                database = database,
                environment = environment,
                wellKnownVeilederV2 = wellKnownVeilederV2,
            )
        }
    }

    val server = embeddedServer(
        environment = applicationEngineEnvironment,
        factory = Netty,
    )

    applicationEngineEnvironment.monitor.subscribe(ApplicationStarted) { application ->
        applicationState.ready = true
        application.environment.log.info("Application is ready, running Java VM ${Runtime.version()}")
        launchKafkaModule(
            applicationState = applicationState,
            environment = environment,
        )
        launchCronjobModule(
            applicationState = applicationState,
            database = database,
            environment = environment,
        )
    }

    Runtime.getRuntime().addShutdownHook(
        Thread {
            server.stop(10, 10, TimeUnit.SECONDS)
        }
    )

    server.start(wait = false)
}

val Application.envKind get() = environment.config.property("ktor.environment").getString()

fun Application.isDev(block: () -> Unit) {
    if (envKind == "dev") block()
}

fun Application.isProd(block: () -> Unit) {
    if (envKind == "production") block()
}
