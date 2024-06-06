package no.nav.syfo.personstatus.api.v2.auth

import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.engine.apache.*
import io.ktor.client.request.get
import kotlinx.coroutines.runBlocking
import no.nav.syfo.personstatus.infrastructure.clients.proxyConfig
import kotlin.io.use

fun getWellKnown(wellKnownUrl: String) = runBlocking {
    HttpClient(Apache, proxyConfig).use { client ->
        client.get(wellKnownUrl).body<WellKnown>()
    }
}

data class WellKnown(
    val authorization_endpoint: String,
    val token_endpoint: String,
    val jwks_uri: String,
    val issuer: String,
)
