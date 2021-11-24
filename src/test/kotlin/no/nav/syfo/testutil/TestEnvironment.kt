package no.nav.syfo.testutil

import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.Environment
import java.net.ServerSocket

fun testEnvironment(
    azureTokenEndpoint: String = "azureTokenEndpoint",
    kafkaBootstrapServers: String = "boostrapserver",
    syfotilgangskontrollUrl: String? = null,
) = Environment(
    azureAppClientId = "azureAppClientId",
    azureAppClientSecret = "azureAppClientSecret",
    azureAppWellKnownUrl = "azureAppWellKnownUrl",
    azureTokenEndpoint = azureTokenEndpoint,
    databaseHost = "localhost",
    databasePort = "5432",
    databaseName = "syfooversiktsrv_dev",
    databaseUsername = "username",
    databasePassword = "password",
    oversikthendelseOppfolgingstilfelleTopic = "topic1",
    kafkaBootstrapServers = kafkaBootstrapServers,
    kafkaSchemaRegistryUrl = "http://kafka-schema-registry.tpa.svc.nais.local:8081",
    applicationName = "syfooversiktsrv",
    serviceuserUsername = "",
    serviceuserPassword = "",
    syfotilgangskontrollClientId = "syfotilgangskontrollClientId",
    syfotilgangskontrollUrl = syfotilgangskontrollUrl ?: "tilgangskontroll",
    toggleKafkaConsumerEnabled = true,
)

fun testAppState() = ApplicationState(
    alive = true,
    ready = true,
)

fun getRandomPort() = ServerSocket(0).use {
    it.localPort
}
