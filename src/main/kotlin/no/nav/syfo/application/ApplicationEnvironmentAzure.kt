package no.nav.syfo.application

data class ApplicationEnvironmentAzure(
    val appClientId: String,
    val appClientSecret: String,
    val appWellKnownUrl: String,
    val openidConfigTokenEndpoint: String,
)
