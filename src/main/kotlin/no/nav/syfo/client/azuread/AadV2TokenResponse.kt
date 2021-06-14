package no.nav.syfo.client.azuread

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class AadV2TokenResponse(
    val access_token: String,
    val expires_in: Int,
    val token_type: String
)
