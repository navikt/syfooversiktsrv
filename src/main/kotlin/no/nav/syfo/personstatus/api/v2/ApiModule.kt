package no.nav.syfo.personstatus.api.v2

import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.routing
import no.nav.syfo.ApplicationState
import no.nav.syfo.Environment
import no.nav.syfo.personstatus.api.v2.access.APIConsumerAccessService
import no.nav.syfo.personstatus.api.v2.auth.installCallId
import no.nav.syfo.personstatus.api.v2.auth.installContentNegotiation
import no.nav.syfo.personstatus.api.v2.auth.installJwtAuthentication
import no.nav.syfo.personstatus.api.v2.auth.installMetrics
import no.nav.syfo.personstatus.api.v2.auth.installStatusPages
import no.nav.syfo.personstatus.api.v2.endpoints.registerPodApi
import no.nav.syfo.personstatus.api.v2.endpoints.registerPrometheusApi
import no.nav.syfo.personstatus.infrastructure.database.DatabaseInterface
import no.nav.syfo.personstatus.infrastructure.clients.veiledertilgang.VeilederTilgangskontrollClient
import no.nav.syfo.personstatus.application.PersonoversiktOppgaverService
import no.nav.syfo.personstatus.application.PersonoversiktStatusService
import no.nav.syfo.personstatus.api.v2.auth.JwtIssuer
import no.nav.syfo.personstatus.api.v2.auth.JwtIssuerType
import no.nav.syfo.personstatus.api.v2.auth.WellKnown
import no.nav.syfo.personstatus.api.v2.endpoints.registerPersonTildelingApiV2
import no.nav.syfo.personstatus.api.v2.endpoints.registerPersonoversiktApiV2
import no.nav.syfo.personstatus.api.v2.endpoints.registerPersonoversiktSystemApi
import no.nav.syfo.personstatus.application.IPersonOversiktStatusRepository
import no.nav.syfo.personstatus.application.PersonTildelingService
import no.nav.syfo.personstatus.application.PersonoversiktSearchService
import no.nav.syfo.personstatus.application.PersonBehandlendeEnhetService

fun Application.apiModule(
    applicationState: ApplicationState,
    database: DatabaseInterface,
    environment: Environment,
    wellKnownVeilederV2: WellKnown,
    personoversiktStatusService: PersonoversiktStatusService,
    tilgangskontrollClient: VeilederTilgangskontrollClient,
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
