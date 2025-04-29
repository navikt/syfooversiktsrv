package no.nav.syfo.personstatus.api.v2.access

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.syfo.personstatus.api.v2.auth.getConsumerClientId
import no.nav.syfo.util.configuredJacksonMapper
import kotlin.collections.filter
import kotlin.collections.map

class APIConsumerAccessService(
    azureAppPreAuthorizedApps: String
) {
    private val preAuthorizedClients: List<PreAuthorizedClient> = configuredJacksonMapper()
        .readValue(azureAppPreAuthorizedApps)

    fun validateConsumerApplicationAZP(
        token: String,
        authorizedApplicationNames: List<String>,
    ) {
        val consumerClientIdAzp: String = getConsumerClientId(token = token)
        val clientIds = preAuthorizedClients
            .filter {
                authorizedApplicationNames.contains(
                    it.toNamespaceAndApplicationName().applicationName
                )
            }
            .map { it.clientId }
        if (!clientIds.contains(consumerClientIdAzp)) {
            throw ForbiddenAccessSystemConsumer(consumerClientIdAzp = consumerClientIdAzp)
        }
    }
}
