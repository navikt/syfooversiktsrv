package no.nav.syfo.testutil

import no.nav.syfo.Environment
import no.nav.syfo.application.ApplicationState
import java.net.ServerSocket

fun testEnvironment(
    azureTokenEndpoint: String = "azureTokenEndpoint",
    syfotilgangskontrollUrl: String? = null,
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
    syfotilgangskontrollClientId = "syfotilgangskontrollClientId",
    syfotilgangskontrollUrl = syfotilgangskontrollUrl ?: "tilgangskontroll",
)

fun testAppState() = ApplicationState(
    alive = true,
    ready = true,
)

fun getRandomPort() = ServerSocket(0).use {
    it.localPort
}
