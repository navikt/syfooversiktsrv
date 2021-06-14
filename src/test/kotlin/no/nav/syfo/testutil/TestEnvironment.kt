package no.nav.syfo.testutil

import no.nav.syfo.ApplicationState
import no.nav.syfo.Environment
import java.net.ServerSocket

fun testEnvironment(
    azureTokenEndpoint: String = "azureTokenEndpoint",
    syfotilgangskontrollUrl: String? = null
) = Environment(
    applicationPort = 8080,
    applicationThreads = 1,
    azureAppClientId = "azureAppClientId",
    azureAppClientSecret = "azureAppClientSecret",
    azureAppWellKnownUrl = "azureAppWellKnownUrl",
    azureTokenEndpoint = azureTokenEndpoint,
    oversikthendelseOppfolgingstilfelleTopic = "topic1",
    kafkaBootstrapServers = "boostrapserver",
    syfooversiktsrvDBURL = "12314.adeo.no",
    mountPathVault = "vault.adeo.no",
    databaseName = "syfooversiktsrv",
    applicationName = "syfooversiktsrv",
    jwkKeysUrl = "",
    jwtIssuer = "",
    aadDiscoveryUrl = "",
    syfotilgangskontrollClientId = "syfotilgangskontrollClientId",
    syfotilgangskontrollUrl = syfotilgangskontrollUrl ?: "tilgangskontroll",
    clientid = "loginservice"
)

fun testAppState() = ApplicationState(
    running = true,
    initialized = true
)

fun getRandomPort() = ServerSocket(0).use {
    it.localPort
}
