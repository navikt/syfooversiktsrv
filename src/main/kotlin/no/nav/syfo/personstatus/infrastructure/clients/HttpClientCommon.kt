package no.nav.syfo.personstatus.infrastructure.clients

import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.apache.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.jackson.jackson
import no.nav.syfo.util.configure
import org.apache.http.impl.conn.SystemDefaultRoutePlanner
import java.net.ProxySelector

val commonConfig: HttpClientConfig<out HttpClientEngineConfig>.() -> Unit = {
    install(HttpTimeout) {
        requestTimeoutMillis = 30000
    }
    install(ContentNegotiation) {
        jackson { configure() }
    }
    install(HttpRequestRetry) {
        retryOnExceptionIf(2) { _, cause ->
            cause !is ClientRequestException
        }
        constantDelay(500L)
    }
    expectSuccess = true
}

val proxyConfig: HttpClientConfig<ApacheEngineConfig>.() -> Unit = {
    this.commonConfig()
    engine {
        customizeClient {
            setRoutePlanner(SystemDefaultRoutePlanner(ProxySelector.getDefault()))
        }
    }
}

fun httpClientDefault() = HttpClient(CIO, commonConfig)

fun httpClientProxy() = HttpClient(Apache, proxyConfig)
