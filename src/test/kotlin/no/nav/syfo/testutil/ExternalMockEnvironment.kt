package no.nav.syfo.testutil

import no.nav.syfo.application.ApplicationState
import no.nav.syfo.testutil.mock.*

class ExternalMockEnvironment private constructor() {
    val applicationState: ApplicationState = testAppState()
    val database = TestDatabase()
    val embeddedEnvironment = testKafka()

    val azureAdMock = AzureAdMock()
    val eregMock = EregMock()
    val pdlMock = PdlMock()
    val syfobehandlendeenhetMock = SyfobehandlendeenhetMock()
    val tilgangskontrollMock = VeilederTilgangskontrollMock()

    val externalApplicationMockMap = hashMapOf(
        azureAdMock.name to azureAdMock.server,
        eregMock.name to eregMock.server,
        pdlMock.name to pdlMock.server,
        syfobehandlendeenhetMock.name to syfobehandlendeenhetMock.server,
        tilgangskontrollMock.name to tilgangskontrollMock.server,
    )

    val environment = testEnvironment(
        azureTokenEndpoint = azureAdMock.url,
        eregUrl = eregMock.url,
        pdlUrl = pdlMock.url,
        syfobehandlendeenhetUrl = syfobehandlendeenhetMock.url,
        syfotilgangskontrollUrl = tilgangskontrollMock.url,
        istilgangskontrollUrl = tilgangskontrollMock.url,
        kafkaBootstrapServers = embeddedEnvironment.brokersURL
    )
    val redisServer = testRedis(
        redisEnvironment = environment.redis,
    )

    val wellKnownVeilederV2 = wellKnownVeilederV2Mock()

    companion object {
        val instance: ExternalMockEnvironment by lazy {
            ExternalMockEnvironment().also {
                it.startExternalMocks()
            }
        }
    }
}

private fun ExternalMockEnvironment.startExternalMocks() {
    this.embeddedEnvironment.start()
    this.externalApplicationMockMap.forEach { (_, externalMock) -> externalMock.start() }
    this.redisServer.start()
}
