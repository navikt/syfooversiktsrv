package no.nav.syfo.testutil

import no.nav.syfo.application.*
import java.net.ServerSocket

fun testEnvironment(
    azureTokenEndpoint: String = "azureTokenEndpoint",
    kafkaBootstrapServers: String = "boostrapserver",
    isproxyUrl: String = "isproxy",
    pdlUrl: String,
    syfobehandlendeenhetUrl: String = "syfobehandlendeenhet",
    syfotilgangskontrollUrl: String = "syfotilgangskontroll",
) = Environment(
    applicationName = "syfooversiktsrv",
    azure = ApplicationEnvironmentAzure(
        appClientId = "appClientId",
        appClientSecret = "appClientSecret",
        appWellKnownUrl = "appWellKnownUrl",
        openidConfigTokenEndpoint = azureTokenEndpoint,
    ),
    database = ApplicationEnvironmentDatabase(
        host = "localhost",
        port = "5432",
        name = "syfooversiktsrv_dev",
        username = "username",
        password = "password",
    ),
    electorPath = "/tmp",
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
    clients = ApplicationEnvironmentClients(
        isproxy = ApplicationEnvironmentClient(
            clientId = "dev-fss.teamsykefravr.isproxy",
            url = isproxyUrl,
        ),
        pdl = ApplicationEnvironmentClient(
            clientId = "dev-fss.pdl.pdl-api",
            url = pdlUrl,
        ),
        syfobehandlendeenhet = ApplicationEnvironmentClient(
            clientId = "dev-gcp.teamsykefravr.syfobehandlendeenhet",
            url = syfobehandlendeenhetUrl,
        ),
        syfotilgangskontroll = ApplicationEnvironmentClient(
            clientId = "dev-fss.teamsykefravr.syfotilgangskontroll",
            url = syfotilgangskontrollUrl,
        ),
    ),
    redis = ApplicationEnvironmentRedis(
        host = "localhost",
        port = 6379,
        secret = "password",
    ),
    serviceuserUsername = "",
    serviceuserPassword = "",
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
