package no.nav.syfo.testutil

import io.ktor.server.application.*
import no.nav.syfo.application.api.apiModule

fun Application.testApiModule(
    externalMockEnvironment: ExternalMockEnvironment
) {
    this.apiModule(
        applicationState = externalMockEnvironment.applicationState,
        database = externalMockEnvironment.database,
        environment = externalMockEnvironment.environment,
        wellKnownVeilederV2 = externalMockEnvironment.wellKnownVeilederV2,
    )
}
