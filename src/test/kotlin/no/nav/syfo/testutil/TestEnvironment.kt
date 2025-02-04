package no.nav.syfo.testutil

import no.nav.syfo.ApplicationState
import no.nav.syfo.Environment
import no.nav.syfo.application.cache.RedisConfig
import no.nav.syfo.personstatus.infrastructure.clients.ClientEnvironment
import no.nav.syfo.personstatus.infrastructure.clients.ClientsEnvironment
import no.nav.syfo.personstatus.infrastructure.clients.azuread.AzureEnvironment
import no.nav.syfo.personstatus.infrastructure.database.DatabaseEnvironment
import no.nav.syfo.personstatus.infrastructure.kafka.KafkaEnvironment
import java.net.URI

fun testEnvironment(
    azureTokenEndpoint: String = "azureTokenEndpoint",
    kafkaBootstrapServers: String = "boostrapserver",
    eregUrl: String = "ereg",
    pdlUrl: String = "pdl",
    syfobehandlendeenhetUrl: String = "syfobehandlendeenhet",
    arbeidsuforhetvurderingUrl: String = "arbeidsuforhetvurdering",
    isaktivitetskravUrl: String = "isaktivitetskrav",
    ismanglendemedvirkningUrl: String = "ismanglendemedvirkning",
    istilgangskontrollUrl: String = "istilgangskontroll",
    ishuskelappUrl: String = "ishuskelapp",
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
    kafka = KafkaEnvironment(
        aivenBootstrapServers = kafkaBootstrapServers,
        aivenCredstorePassword = "credstorepassord",
        aivenKeystoreLocation = "keystore",
        aivenSecurityProtocol = "SSL",
        aivenTruststoreLocation = "truststore",
        aivenSchemaRegistryUrl = "http://kafka-schema-registry.tpa.svc.nais.local:8081",
        aivenRegistryUser = "registryuser",
        aivenRegistryPassword = "registrypassword",
    ),
    clients = ClientsEnvironment(
        ereg = ClientEnvironment(
            baseUrl = eregUrl,
            clientId = "",
        ),
        pdl = ClientEnvironment(
            baseUrl = pdlUrl,
            clientId = "dev-fss.pdl.pdl-api",
        ),
        syfobehandlendeenhet = ClientEnvironment(
            baseUrl = syfobehandlendeenhetUrl,
            clientId = "dev-gcp.teamsykefravr.syfobehandlendeenhet",
        ),
        arbeidsuforhetvurdering = ClientEnvironment(
            baseUrl = arbeidsuforhetvurderingUrl,
            clientId = "dev-gcp.teamsykefravr.arbeidsuforhetvurdering",
        ),
        manglendeMedvirkning = ClientEnvironment(
            baseUrl = ismanglendemedvirkningUrl,
            clientId = "dev-gcp.teamsykefravr.ismanglendemedvirkning",
        ),
        aktivitetskrav = ClientEnvironment(
            baseUrl = isaktivitetskravUrl,
            clientId = "dev-gcp.teamsykefravr.isaktivitetskrav",
        ),
        istilgangskontroll = ClientEnvironment(
            baseUrl = istilgangskontrollUrl,
            clientId = "dev-gcp.teamsykefravr.istilgangskontroll",
        ),
        ishuskelapp = ClientEnvironment(
            baseUrl = ishuskelappUrl,
            clientId = "dev-gcp.teamsykefravr.ishuskelapp",
        ),
        ismeroppfolging = ClientEnvironment(
            baseUrl = "ismeroppfolgingUrl",
            clientId = "dev-gcp.teamsykefravr.ismeroppfolging",
        ),
        syfoveileder = ClientEnvironment(
            clientId = "dev-gcp.teamsykefravr.syfoveileder",
            baseUrl = "syfoveilederUrl",
        ),
    ),
    redisConfig = RedisConfig(
        redisUri = URI("http://localhost:6379"),
        redisDB = 0,
        redisUsername = "redisUser",
        redisPassword = "redisPassword",
        ssl = false,
    ),
    cronjobBehandlendeEnhetIntervalDelayMinutes = 5,
)

fun testAppState() = ApplicationState(
    alive = true,
    ready = true,
)
