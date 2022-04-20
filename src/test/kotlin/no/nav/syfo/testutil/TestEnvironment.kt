package no.nav.syfo.testutil

import no.nav.syfo.application.*
import java.net.ServerSocket

fun testEnvironment(
    azureTokenEndpoint: String = "azureTokenEndpoint",
    kafkaBootstrapServers: String = "boostrapserver",
    isproxyUrl: String = "isproxy",
    pdlUrl: String,
    syfobehandlendeenhetUrl: String = "syfobehandlendeenhet",
    syfotilgangskontrollUrl: String? = null,
) = Environment(
    azure = ApplicationEnvironmentAzure(
        appClientId = "appClientId",
        appClientSecret = "appClientSecret",
        appWellKnownUrl = "appWellKnownUrl",
        tokenEndpoint = azureTokenEndpoint,
    ),
    database = ApplicationEnvironmentDatabase(
        host = "localhost",
        port = "5432",
        name = "syfooversiktsrv_dev",
        username = "username",
        password = "password",
    ),
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
    kafkaOversikthendelsetilfelleProcessingEnabled = false,
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
    syfobehandlendeenhetClientId = "dev-gcp.teamsykefravr.syfobehandlendeenhet",
    syfobehandlendeenhetUrl = syfobehandlendeenhetUrl,
    syfotilgangskontrollClientId = "syfotilgangskontrollClientId",
    syfotilgangskontrollUrl = syfotilgangskontrollUrl ?: "tilgangskontroll",
    personBehandlendeEnhetCronjobEnabled = true,
    personOppfolgingstilfelleVirksomhetsnavnCronjobEnabled = true,
)

fun testAppState() = ApplicationState(
    alive = true,
    ready = true,
)

fun getRandomPort() = ServerSocket(0).use {
    it.localPort
}
