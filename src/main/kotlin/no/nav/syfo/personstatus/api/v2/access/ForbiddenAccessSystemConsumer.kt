package no.nav.syfo.personstatus.api.v2.access

class ForbiddenAccessSystemConsumer(
    consumerClientIdAzp: String,
    message: String = "Consumer with clientId(azp)=$consumerClientIdAzp is denied access to system API",
) : RuntimeException(message)
