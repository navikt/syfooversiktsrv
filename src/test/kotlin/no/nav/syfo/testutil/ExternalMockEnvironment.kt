package no.nav.syfo.testutil

import io.ktor.server.netty.*
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.testutil.mock.*

class ExternalMockEnvironment {
    val applicationState: ApplicationState = testAppState()
    val database = TestDatabase()

    val azureAdMock = AzureAdMock()
    val isproxyMock = IsproxyMock()
    val pdlMock = PdlMock()
    val syfobehandlendeenhetMock = SyfobehandlendeenhetMock()
    val tilgangskontrollMock = VeilederTilgangskontrollMock()

    val externalApplicationMockMap = hashMapOf(
        azureAdMock.name to azureAdMock.server,
        isproxyMock.name to isproxyMock.server,
        pdlMock.name to pdlMock.server,
        syfobehandlendeenhetMock.name to syfobehandlendeenhetMock.server,
        tilgangskontrollMock.name to tilgangskontrollMock.server,
    )

    val environment = testEnvironment(
        azureTokenEndpoint = azureAdMock.url,
        isproxyUrl = isproxyMock.url,
        pdlUrl = pdlMock.url,
        syfobehandlendeenhetUrl = syfobehandlendeenhetMock.url,
        syfotilgangskontrollUrl = tilgangskontrollMock.url,
    )

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
