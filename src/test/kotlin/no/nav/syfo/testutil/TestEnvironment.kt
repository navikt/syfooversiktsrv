package no.nav.syfo.testutil

import no.nav.syfo.ApplicationState
import no.nav.syfo.Environment
import no.nav.syfo.ValkeyConfig
import no.nav.syfo.personstatus.api.v2.access.PreAuthorizedClient
import no.nav.syfo.personstatus.infrastructure.clients.ClientEnvironment
import no.nav.syfo.personstatus.infrastructure.clients.ClientsEnvironment
import no.nav.syfo.personstatus.infrastructure.clients.azuread.AzureEnvironment
import no.nav.syfo.personstatus.infrastructure.database.DatabaseEnvironment
import no.nav.syfo.personstatus.infrastructure.kafka.KafkaEnvironment
import no.nav.syfo.util.configuredJacksonMapper
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
        azureAppPreAuthorizedApps = configuredJacksonMapper().writeValueAsString(testAzureAppPreAuthorizedApps),
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
    valkeyConfig = ValkeyConfig(
        valkeyUri = URI("http://localhost:6379"),
        valkeyDB = 0,
        valkeyUsername = "valkeyUser",
        valkeyPassword = "valkeyPassword",
        ssl = false,
    ),
    cronjobBehandlendeEnhetIntervalDelayMinutes = 5,
)

fun testAppState() = ApplicationState(
    alive = true,
    ready = true,
)

const val testSyfobehandlendeenhetClientId = "syfobehandlendeenhet-client-id"

val testAzureAppPreAuthorizedApps = listOf(
    PreAuthorizedClient(
        name = "dev-gcp:teamsykefravr:syfobehandlendeenhet",
        clientId = testSyfobehandlendeenhetClientId,
    ),
)
