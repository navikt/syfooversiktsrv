package no.nav.syfo.client.azuread

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDateTime

@JsonIgnoreProperties(ignoreUnknown = true)
data class AzureAdV2TokenResponse(
    val access_token: String,
    val expires_in: Long,
    val token_type: String
)

fun AzureAdV2TokenResponse.toAzureAdV2Token(): AzureAdV2Token {
    val expiresOn = LocalDateTime.now().plusSeconds(this.expires_in)
    return AzureAdV2Token(
        accessToken = this.access_token,
        expires = expiresOn
    )
}
