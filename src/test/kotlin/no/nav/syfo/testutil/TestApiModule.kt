package no.nav.syfo.testutil

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.testing.*
import no.nav.syfo.api.apiModule
import no.nav.syfo.application.PersonBehandlendeEnhetService
import no.nav.syfo.application.PersonoversiktOppgaverService
import no.nav.syfo.infrastructure.clients.aktivitetskrav.AktivitetskravClient
import no.nav.syfo.infrastructure.clients.arbeidsuforhet.ArbeidsuforhetvurderingClient
import no.nav.syfo.infrastructure.clients.azuread.AzureAdClient
import no.nav.syfo.infrastructure.clients.behandlendeenhet.BehandlendeEnhetClient
import no.nav.syfo.infrastructure.clients.dialogmotekandidat.DialogmotekandidatClient
import no.nav.syfo.infrastructure.clients.manglendemedvirkning.ManglendeMedvirkningClient
import no.nav.syfo.infrastructure.clients.meroppfolging.MerOppfolgingClient
import no.nav.syfo.infrastructure.clients.oppfolgingsoppgave.OppfolgingsoppgaveClient
import no.nav.syfo.infrastructure.clients.pdl.PdlClient
import no.nav.syfo.infrastructure.clients.veileder.VeilederClient
import no.nav.syfo.infrastructure.clients.veiledertilgang.VeilederTilgangskontrollClient
import no.nav.syfo.util.configure

fun Application.testApiModule(
    externalMockEnvironment: ExternalMockEnvironment,
) {
    val azureAdClient = AzureAdClient(
        azureEnvironment = externalMockEnvironment.environment.azure,
        valkeyStore = externalMockEnvironment.valkeyStore,
        httpClient = externalMockEnvironment.mockHttpClient
    )

    val pdlClient = PdlClient(
        azureAdClient = azureAdClient,
        clientEnvironment = externalMockEnvironment.environment.clients.pdl,
        httpClient = externalMockEnvironment.mockHttpClient
    )
    val arbeidsuforhetvurderingClient = ArbeidsuforhetvurderingClient(
        azureAdClient = azureAdClient,
        clientEnvironment = externalMockEnvironment.environment.clients.arbeidsuforhetvurdering,
        httpClient = externalMockEnvironment.mockHttpClient
    )
    val manglendeMedvirkningClient = ManglendeMedvirkningClient(
        azureAdClient = azureAdClient,
        clientEnvironment = externalMockEnvironment.environment.clients.manglendeMedvirkning,
        httpClient = externalMockEnvironment.mockHttpClient
    )
    val oppfolgingsoppgaveClient = OppfolgingsoppgaveClient(
        azureAdClient = azureAdClient,
        clientEnvironment = externalMockEnvironment.environment.clients.ishuskelapp,
        httpClient = externalMockEnvironment.mockHttpClient
    )
    val aktivitetskravClient = AktivitetskravClient(
        azureAdClient = azureAdClient,
        clientEnvironment = externalMockEnvironment.environment.clients.aktivitetskrav,
        httpClient = externalMockEnvironment.mockHttpClient
    )
    val merOppfolgingClient = MerOppfolgingClient(
        azureAdClient = azureAdClient,
        clientEnvironment = externalMockEnvironment.environment.clients.ismeroppfolging,
        httpClient = externalMockEnvironment.mockHttpClient
    )
    val dialogmotekandidatClient = DialogmotekandidatClient(
        azureAdClient = azureAdClient,
        clientEnvironment = externalMockEnvironment.environment.clients.dialogmotekandidat,
        httpClient = externalMockEnvironment.mockHttpClient
    )
    val veilederTilgangskontrollClient = VeilederTilgangskontrollClient(
        azureAdClient = azureAdClient,
        istilgangskontrollEnv = externalMockEnvironment.environment.clients.istilgangskontroll,
        httpClient = externalMockEnvironment.mockHttpClient
    )
    val veilederClient = VeilederClient(
        azureAdClient = azureAdClient,
        clientEnvironment = externalMockEnvironment.environment.clients.syfoveileder,
        valkeyStore = externalMockEnvironment.valkeyStore,
        httpClient = externalMockEnvironment.mockHttpClient,
    )
    val behandlendeEnhetClient = BehandlendeEnhetClient(
        azureAdClient = azureAdClient,
        clientEnvironment = externalMockEnvironment.environment.clients.syfobehandlendeenhet,
        httpClient = externalMockEnvironment.mockHttpClient,
    )
    val personoversiktRepository = externalMockEnvironment.personOversiktStatusRepository
    val personoversiktStatusService = externalMockEnvironment.personoversiktStatusService
    val personBehandlendeEnhetService = PersonBehandlendeEnhetService(
        personoversiktStatusRepository = personoversiktRepository,
        behandlendeEnhetClient = behandlendeEnhetClient,
    )

    this.apiModule(
        applicationState = externalMockEnvironment.applicationState,
        database = externalMockEnvironment.database,
        environment = externalMockEnvironment.environment,
        wellKnownVeilederV2 = externalMockEnvironment.wellKnownVeilederV2,
        personoversiktStatusService = personoversiktStatusService,
        tilgangskontrollClient = veilederTilgangskontrollClient,
        personoversiktOppgaverService = PersonoversiktOppgaverService(
            arbeidsuforhetvurderingClient = arbeidsuforhetvurderingClient,
            manglendeMedvirkningClient = manglendeMedvirkningClient,
            oppfolgingsoppgaveClient = oppfolgingsoppgaveClient,
            aktivitetskravClient = aktivitetskravClient,
            merOppfolgingClient = merOppfolgingClient,
            dialogmotekandidatClient = dialogmotekandidatClient,
        ),
        personBehandlendeEnhetService = personBehandlendeEnhetService,
        personoversiktStatusRepository = personoversiktRepository,
        veilederClient = veilederClient,
    )
}

fun ApplicationTestBuilder.setupApiAndClient(): HttpClient {
    application {
        testApiModule(
            externalMockEnvironment = ExternalMockEnvironment.instance,
        )
    }
    val client = createClient {
        install(ContentNegotiation) {
            jackson { configure() }
        }
    }
    return client
}
