package no.nav.syfo.client

data class ApplicationEnvironmentClients(
    val isproxy: ApplicationEnvironmentClient,
    val pdl: ApplicationEnvironmentClient,
    val syfobehandlendeenhet: ApplicationEnvironmentClient,
    val syfotilgangskontroll: ApplicationEnvironmentClient,
)

data class ApplicationEnvironmentClient(
    val baseUrl: String,
    val clientId: String,
)
