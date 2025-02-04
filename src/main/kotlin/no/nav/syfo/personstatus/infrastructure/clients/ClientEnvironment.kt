package no.nav.syfo.personstatus.infrastructure.clients

data class ClientsEnvironment(
    val ereg: ClientEnvironment,
    val pdl: ClientEnvironment,
    val syfobehandlendeenhet: ClientEnvironment,
    val arbeidsuforhetvurdering: ClientEnvironment,
    val manglendeMedvirkning: ClientEnvironment,
    val aktivitetskrav: ClientEnvironment,
    val istilgangskontroll: ClientEnvironment,
    val ishuskelapp: ClientEnvironment,
    val ismeroppfolging: ClientEnvironment,
    val syfoveileder: ClientEnvironment,
)

data class ClientEnvironment(
    val baseUrl: String,
    val clientId: String,
)
