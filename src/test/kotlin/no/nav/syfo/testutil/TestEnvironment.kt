package no.nav.syfo.testutil

import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.Environment
import no.nav.syfo.application.cache.RedisEnvironment
import no.nav.syfo.application.database.DatabaseEnvironment
import no.nav.syfo.application.kafka.KafkaEnvironment
import no.nav.syfo.client.ClientEnvironment
import no.nav.syfo.client.ClientsEnvironment
import no.nav.syfo.client.azuread.AzureEnvironment
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
    azure = AzureEnvironment(
        appClientId = "appClientId",
        appClientSecret = "appClientSecret",
        appWellKnownUrl = "appWellKnownUrl",
        openidConfigTokenEndpoint = azureTokenEndpoint,
    ),
    database = DatabaseEnvironment(
        host = "localhost",
        port = "5432",
        name = "syfooversiktsrv_dev",
        username = "username",
        password = "password",
    ),
    electorPath = "/tmp",
    kafkaBootstrapServers = kafkaBootstrapServers,
    kafkaSchemaRegistryUrl = "http://kafka-schema-registry.tpa.svc.nais.local:8081",
    kafka = KafkaEnvironment(
        aivenBootstrapServers = kafkaBootstrapServers,
        aivenCredstorePassword = "credstorepassord",
        aivenKeystoreLocation = "keystore",
        aivenSecurityProtocol = "SSL",
        aivenTruststoreLocation = "truststore",
    ),
    kafkaOppfolgingstilfellePersonProcessingEnabled = true,
    kafkaDialogmotekandidatProcessingEnabled = true,
    clients = ClientsEnvironment(
        isproxy = ClientEnvironment(
            baseUrl = isproxyUrl,
            clientId = "dev-fss.teamsykefravr.isproxy",
        ),
        pdl = ClientEnvironment(
            baseUrl = pdlUrl,
            clientId = "dev-fss.pdl.pdl-api",
        ),
        syfobehandlendeenhet = ClientEnvironment(
            baseUrl = syfobehandlendeenhetUrl,
            clientId = "dev-gcp.teamsykefravr.syfobehandlendeenhet",
        ),
        syfotilgangskontroll = ClientEnvironment(
            baseUrl = syfotilgangskontrollUrl,
            clientId = "dev-fss.teamsykefravr.syfotilgangskontroll",
        ),
    ),
    redis = RedisEnvironment(
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
