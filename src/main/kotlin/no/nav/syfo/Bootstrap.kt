package no.nav.syfo

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.jwt.jwt
import io.ktor.features.ContentNegotiation
import io.ktor.jackson.jackson
import io.ktor.metrics.micrometer.MicrometerMetrics
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.micrometer.core.instrument.Clock
import io.micrometer.core.instrument.binder.jvm.*
import io.micrometer.core.instrument.binder.logging.LogbackMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import io.prometheus.client.CollectorRegistry
import kotlinx.coroutines.*
import no.nav.syfo.api.registerNaisApi
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors

data class ApplicationState(var running: Boolean = true, var initialized: Boolean = false)

private val log: org.slf4j.Logger = LoggerFactory.getLogger("no.nav.syfo.syfooversiktsrv")

fun main() = runBlocking(Executors.newFixedThreadPool(2).asCoroutineDispatcher()) {
    val env = getEnvironment()
    val applicationState = ApplicationState()
    embeddedServer(Netty, env.applicationPort) {
        install(ContentNegotiation) {
            jackson {
                registerKotlinModule()
                registerModule(JavaTimeModule())
                configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            }
        }
        install(MicrometerMetrics) {
            registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT, CollectorRegistry.defaultRegistry, Clock.SYSTEM)
            meterBinders = listOf(
                    ClassLoaderMetrics(),
                    JvmMemoryMetrics(),
                    JvmGcMetrics(),
                    ProcessorMetrics(),
                    JvmThreadMetrics(),
                    LogbackMetrics()
            )
        }
        install(Authentication) {
            jwt {
                realm = "Syfooversiktsrv"
                validate { credentials ->
                    val appid: String = credentials.payload.getClaim("appid").asString()
                    log.info("authorization attempt for $appid")
                    log.info("authorization failed")
                    return@validate null
                }
            }
        }
        initRouting(applicationState)
    }.start(wait = false)

    Runtime.getRuntime().addShutdownHook(Thread {
        coroutineContext.cancelChildren()
    })

    applicationState.initialized = true
}

fun Application.initRouting(applicationState: ApplicationState) {
    routing {
        registerNaisApi(
                readynessCheck = {
                    applicationState.initialized
                },
                livenessCheck = {
                    applicationState.running
                }
        )
    }
}
