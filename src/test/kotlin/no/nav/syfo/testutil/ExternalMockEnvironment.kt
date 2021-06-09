package no.nav.syfo.testutil

import io.ktor.server.netty.*
import no.nav.syfo.ApplicationState
import no.nav.syfo.testutil.mock.VeilederTilgangskontrollMock
import no.nav.syfo.testutil.mock.wellKnownVeilederMock

class ExternalMockEnvironment {
    val applicationState: ApplicationState = testAppState()
    val database = TestDB()

    val tilgangskontrollMock = VeilederTilgangskontrollMock()

    val externalApplicationMockMap = hashMapOf(
        tilgangskontrollMock.name to tilgangskontrollMock.server
    )

    val environment = testEnvironment(
        syfotilgangskontrollUrl = tilgangskontrollMock.url
    )

    val wellKnownVeileder = wellKnownVeilederMock()
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
    timeoutMillis: Long = 10L,
) {
    this.forEach {
        it.value.stop(gracePeriodMillis, timeoutMillis)
    }
}
