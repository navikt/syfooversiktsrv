package no.nav.syfo.testutil

import no.nav.syfo.ApplicationState
import no.nav.syfo.Environment
import java.net.ServerSocket

fun testEnvironment(
    syfotilgangskontrollUrl: String? = null
) = Environment(
    applicationPort = 8080,
    applicationThreads = 1,
    oversikthendelseOppfolgingstilfelleTopic = "topic1",
    kafkaBootstrapServers = "boostrapserver",
    syfooversiktsrvDBURL = "12314.adeo.no",
    mountPathVault = "vault.adeo.no",
    databaseName = "syfooversiktsrv",
    applicationName = "syfooversiktsrv",
    jwkKeysUrl = "",
    jwtIssuer = "",
    aadDiscoveryUrl = "",
    syfotilgangskontrollUrl = syfotilgangskontrollUrl ?: "tilgangskontroll",
    clientid = ""
)

fun testAppState() = ApplicationState(
    alive = true,
    ready = true
)

fun getRandomPort() = ServerSocket(0).use {
    it.localPort
}
