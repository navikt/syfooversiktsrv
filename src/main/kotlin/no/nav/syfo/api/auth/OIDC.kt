package no.nav.syfo.api.auth

import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.runBlocking
import no.nav.syfo.infrastructure.clients.httpClientProxy
import kotlin.io.use

fun getWellKnown(wellKnownUrl: String) = runBlocking {
    httpClientProxy().use { client ->
        client.get(wellKnownUrl).body<WellKnown>()
    }
}

data class WellKnown(
    val authorization_endpoint: String,
    val token_endpoint: String,
    val jwks_uri: String,
    val issuer: String,
)
