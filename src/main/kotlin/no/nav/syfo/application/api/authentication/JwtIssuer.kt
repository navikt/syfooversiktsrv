package no.nav.syfo.application.api.authentication

data class JwtIssuer(
    val acceptedAudienceList: List<String>,
    val jwtIssuerType: JwtIssuerType,
    val wellKnown: WellKnown,
)

enum class JwtIssuerType {
    VEILEDER_V2,
}
