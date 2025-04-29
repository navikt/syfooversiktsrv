package no.nav.syfo.personstatus.api.v2.auth

data class JwtIssuer(
    val acceptedAudienceList: List<String>,
    val jwtIssuerType: JwtIssuerType,
    val wellKnown: WellKnown,
)

enum class JwtIssuerType {
    INTERNAL_AZUREAD,
}
