package no.nav.syfo.api

import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.routing
import no.nav.syfo.ApplicationState
import no.nav.syfo.Environment
import no.nav.syfo.api.access.APIConsumerAccessService
import no.nav.syfo.api.auth.installCallId
import no.nav.syfo.api.auth.installContentNegotiation
import no.nav.syfo.api.auth.installJwtAuthentication
import no.nav.syfo.api.auth.installMetrics
import no.nav.syfo.api.auth.installStatusPages
import no.nav.syfo.api.endpoints.registerPodApi
import no.nav.syfo.api.endpoints.registerPrometheusApi
import no.nav.syfo.infrastructure.database.DatabaseInterface
import no.nav.syfo.infrastructure.clients.veiledertilgang.VeilederTilgangskontrollClient
import no.nav.syfo.application.PersonoversiktOppgaverService
import no.nav.syfo.application.PersonoversiktStatusService
import no.nav.syfo.api.auth.JwtIssuer
import no.nav.syfo.api.auth.JwtIssuerType
import no.nav.syfo.api.auth.WellKnown
import no.nav.syfo.api.endpoints.registerPersonTildelingApiV2
import no.nav.syfo.api.endpoints.registerPersonoversiktApiV2
import no.nav.syfo.api.endpoints.registerPersonoversiktSystemApi
import no.nav.syfo.application.IPersonOversiktStatusRepository
import no.nav.syfo.application.PersonTildelingService
import no.nav.syfo.application.PersonoversiktSearchService
import no.nav.syfo.application.PersonBehandlendeEnhetService
import no.nav.syfo.infrastructure.clients.veileder.VeilederClient

fun Application.apiModule(
    applicationState: ApplicationState,
    database: DatabaseInterface,
    environment: Environment,
    wellKnownVeilederV2: WellKnown,
    personoversiktStatusService: PersonoversiktStatusService,
    tilgangskontrollClient: VeilederTilgangskontrollClient,
    veilederClient: VeilederClient,
    personoversiktOppgaverService: PersonoversiktOppgaverService,
    personBehandlendeEnhetService: PersonBehandlendeEnhetService,
    personoversiktStatusRepository: IPersonOversiktStatusRepository,
) {
    installCallId()
    installContentNegotiation()
    installMetrics()
    installStatusPages()

    installJwtAuthentication(
        jwtIssuerList = listOf(
            JwtIssuer(
                acceptedAudienceList = listOf(environment.azure.appClientId),
                jwtIssuerType = JwtIssuerType.INTERNAL_AZUREAD,
                wellKnown = wellKnownVeilederV2,
            )
        )
    )

    val personTildelingService = PersonTildelingService(
        personoversiktStatusRepository = personoversiktStatusRepository,
        personBehandlendeEnhetService = personBehandlendeEnhetService,
        veilederClient = veilederClient,
    )
    val personoversiktSearchService = PersonoversiktSearchService(
        personoversiktStatusRepository = personoversiktStatusRepository,
    )
    val apiConsumerAccessService = APIConsumerAccessService(
        azureAppPreAuthorizedApps = environment.azure.azureAppPreAuthorizedApps,
    )

    routing {
        registerPodApi(
            applicationState = applicationState,
            database = database,
        )
        registerPrometheusApi()
        authenticate(JwtIssuerType.INTERNAL_AZUREAD.name) {
            registerPersonoversiktApiV2(
                veilederTilgangskontrollClient = tilgangskontrollClient,
                personoversiktStatusService = personoversiktStatusService,
                personoversiktOppgaverService = personoversiktOppgaverService,
                personoversiktSearchService = personoversiktSearchService,
            )
            registerPersonTildelingApiV2(tilgangskontrollClient, personTildelingService, personoversiktStatusService)
            registerPersonoversiktSystemApi(
                apiConsumerAccessService = apiConsumerAccessService,
                personoversiktStatusService = personoversiktStatusService,
                authorizedApplicationNames = environment.systemAPIAuthorizedConsumerApplicationNameList,
            )
        }
    }
}
