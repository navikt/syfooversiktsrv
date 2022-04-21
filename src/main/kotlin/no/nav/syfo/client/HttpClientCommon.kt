package no.nav.syfo.client

import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.jackson.*
import no.nav.syfo.util.configure
import org.apache.http.impl.conn.SystemDefaultRoutePlanner
import java.net.ProxySelector

fun httpClientDefault() = HttpClient(CIO) {
    install(HttpTimeout) {
        requestTimeoutMillis = 30000
    }
    install(ContentNegotiation) {
        jackson { configure() }
    }
    expectSuccess = true
}

val proxyConfig: HttpClientConfig<ApacheEngineConfig>.() -> Unit = {
    install(HttpTimeout) {
        requestTimeoutMillis = 30000
    }
    install(ContentNegotiation) {
        jackson { configure() }
    }
    expectSuccess = true
    engine {
        customizeClient {
            setRoutePlanner(SystemDefaultRoutePlanner(ProxySelector.getDefault()))
        }
    }
}

fun httpClientProxy() = HttpClient(Apache, proxyConfig)
