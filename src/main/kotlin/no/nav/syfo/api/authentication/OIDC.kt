package no.nav.syfo.api.authentication

import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking
import no.nav.syfo.client.proxyConfig

fun getWellKnown(wellKnownUrl: String) = runBlocking {
    HttpClient(Apache, proxyConfig).use { client ->
        client.get<WellKnown>(wellKnownUrl)
    }
}

data class WellKnown(
    val authorization_endpoint: String,
    val token_endpoint: String,
    val jwks_uri: String,
    val issuer: String
)
