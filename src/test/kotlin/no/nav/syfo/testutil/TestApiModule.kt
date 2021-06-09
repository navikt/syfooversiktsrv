package no.nav.syfo.testutil

import io.ktor.application.*
import no.nav.syfo.api.apiModule

fun Application.testApiModule(
    externalMockEnvironment: ExternalMockEnvironment
) {
    this.apiModule(
        database = externalMockEnvironment.database,
        environment = externalMockEnvironment.environment,
        wellKnownVeileder = externalMockEnvironment.wellKnownVeileder,
    )
}
