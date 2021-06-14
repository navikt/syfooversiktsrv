package no.nav.syfo.testutil

import io.ktor.server.netty.*
import no.nav.syfo.ApplicationState
import no.nav.syfo.testutil.mock.*

class ExternalMockEnvironment {
    val applicationState: ApplicationState = testAppState()
    val database = TestDB()

    val azureAdV2Mock = AzureAdV2Mock()
    val tilgangskontrollMock = VeilederTilgangskontrollMock()

    val externalApplicationMockMap = hashMapOf(
        azureAdV2Mock.name to azureAdV2Mock.server,
        tilgangskontrollMock.name to tilgangskontrollMock.server
    )

    val environment = testEnvironment(
        azureTokenEndpoint = azureAdV2Mock.url,
        syfotilgangskontrollUrl = tilgangskontrollMock.url
    )

    val wellKnownVeileder = wellKnownVeilederMock()
    val wellKnownVeilederV2 = wellKnownVeilederV2Mock()
}

fun ExternalMockEnvironment.startExternalMocks() {
    this.externalApplicationMockMap.start()
}

fun ExternalMockEnvironment.stopExternalMocks() {
    this.externalApplicationMockMap.stop()
    this.database.stop()
}

fun HashMap<String, NettyApplicationEngine>.start() {
    this.forEach {
        it.value.start()
    }
}

fun HashMap<String, NettyApplicationEngine>.stop(
    gracePeriodMillis: Long = 1L,
    timeoutMillis: Long = 10L
) {
    this.forEach {
        it.value.stop(gracePeriodMillis, timeoutMillis)
    }
}
