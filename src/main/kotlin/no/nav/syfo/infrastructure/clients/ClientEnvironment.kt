package no.nav.syfo.infrastructure.clients

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
    val dialogmotekandidat: ClientEnvironment,
    val syfoveileder: ClientEnvironment,
)

data class ClientEnvironment(
    val baseUrl: String,
    val clientId: String,
)
