package no.nav.syfo.application

data class ApplicationEnvironmentClients(
    val isproxy: ApplicationEnvironmentClient,
    val pdl: ApplicationEnvironmentClient,
    val syfobehandlendeenhet: ApplicationEnvironmentClient,
    val syfotilgangskontroll: ApplicationEnvironmentClient,
)

data class ApplicationEnvironmentClient(
    val clientId: String,
    val url: String,
)
