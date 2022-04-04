package no.nav.syfo.testutil

import no.nav.syfo.application.*
import java.net.ServerSocket

fun testEnvironment(
    azureTokenEndpoint: String = "azureTokenEndpoint",
    kafkaBootstrapServers: String = "boostrapserver",
    isproxyUrl: String = "isproxy",
    pdlUrl: String,
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
    electorPath = "/tmp",
    oversikthendelseOppfolgingstilfelleTopic = "topic1",
    kafkaBootstrapServers = kafkaBootstrapServers,
    kafkaSchemaRegistryUrl = "http://kafka-schema-registry.tpa.svc.nais.local:8081",
    kafka = ApplicationEnvironmentKafka(
        aivenBootstrapServers = kafkaBootstrapServers,
        aivenCredstorePassword = "credstorepassord",
        aivenKeystoreLocation = "keystore",
        aivenSecurityProtocol = "SSL",
        aivenTruststoreLocation = "truststore",
    ),
    kafkaOppfolgingstilfellePersonProcessingEnabled = true,
    applicationName = "syfooversiktsrv",
    isproxyClientId = "dev-fss.teamsykefravr.isproxy",
    isproxyUrl = isproxyUrl,
    pdlClientId = "dev-fss.pdl.pdl-api",
    pdlUrl = pdlUrl,
    redisHost = "localhost",
    redisSecret = "password",
    serviceuserUsername = "",
    serviceuserPassword = "",
    syfotilgangskontrollClientId = "syfotilgangskontrollClientId",
    syfotilgangskontrollUrl = syfotilgangskontrollUrl ?: "tilgangskontroll",
    personOppfolgingstilfelleVirksomhetsnavnCronjobEnabled = true,
    toggleKafkaConsumerEnabled = true,
)

fun testAppState() = ApplicationState(
    alive = true,
    ready = true,
)

fun getRandomPort() = ServerSocket(0).use {
    it.localPort
}
