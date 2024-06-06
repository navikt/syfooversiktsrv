package no.nav.syfo.client

data class ClientsEnvironment(
    val ereg: ClientEnvironment,
    val pdl: ClientEnvironment,
    val syfobehandlendeenhet: ClientEnvironment,
    val arbeidsuforhetvurdering: ClientEnvironment,
    val istilgangskontroll: ClientEnvironment,
)

data class ClientEnvironment(
    val baseUrl: String,
    val clientId: String,
)
