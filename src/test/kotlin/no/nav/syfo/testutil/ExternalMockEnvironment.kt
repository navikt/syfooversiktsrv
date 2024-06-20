package no.nav.syfo.testutil

import no.nav.syfo.ApplicationState
import no.nav.syfo.application.cache.RedisStore
import no.nav.syfo.personstatus.infrastructure.clients.pdl.PdlClient
import no.nav.syfo.personstatus.infrastructure.clients.azuread.AzureAdClient
import no.nav.syfo.testutil.mock.*

class ExternalMockEnvironment private constructor() {
    val applicationState: ApplicationState = testAppState()
    val database = TestDatabase()
    val embeddedEnvironment = testKafka()

    private val azureAdMock = AzureAdMock()
    private val eregMock = EregMock()
    private val pdlMock = PdlMock()
    private val syfobehandlendeenhetMock = SyfobehandlendeenhetMock()
    private val arbeidsuforhetvurderingMock = ArbeidsuforhetvurderingMock()
    private val tilgangskontrollMock = VeilederTilgangskontrollMock()
    private val oppfolgingsoppgaveMock = OppfolgingsoppgaveMock()

    val externalApplicationMockMap = hashMapOf(
        azureAdMock.name to azureAdMock.server,
        eregMock.name to eregMock.server,
        pdlMock.name to pdlMock.server,
        syfobehandlendeenhetMock.name to syfobehandlendeenhetMock.server,
        arbeidsuforhetvurderingMock.name to arbeidsuforhetvurderingMock.server,
        tilgangskontrollMock.name to tilgangskontrollMock.server,
        oppfolgingsoppgaveMock.name to oppfolgingsoppgaveMock.server,
    )

    val environment = testEnvironment(
        azureTokenEndpoint = azureAdMock.url,
        eregUrl = eregMock.url,
        pdlUrl = pdlMock.url,
        syfobehandlendeenhetUrl = syfobehandlendeenhetMock.url,
        arbeidsuforhetvurderingUrl = arbeidsuforhetvurderingMock.url,
        istilgangskontrollUrl = tilgangskontrollMock.url,
        kafkaBootstrapServers = embeddedEnvironment.brokersURL,
        ishuskelappUrl = oppfolgingsoppgaveMock.url,
    )
    val redisServer = testRedis(
        redisEnvironment = environment.redis,
    )

    val wellKnownVeilederV2 = wellKnownVeilederV2Mock()

    val pdlClient = PdlClient(
        azureAdClient = AzureAdClient(
            azureEnvironment = environment.azure,
            redisStore = RedisStore(environment.redis),
        ),
        clientEnvironment = environment.clients.pdl,
    )

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
