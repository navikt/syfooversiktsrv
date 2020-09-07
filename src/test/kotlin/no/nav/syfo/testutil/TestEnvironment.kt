package no.nav.syfo.testutil

import no.nav.syfo.Environment

val testEnvironment = Environment(
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
    syfotilgangskontrollUrl = "",
    clientid = "",
    syfoveilederUrl = ""
)
