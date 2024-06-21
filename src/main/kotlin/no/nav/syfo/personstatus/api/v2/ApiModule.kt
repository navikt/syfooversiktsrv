package no.nav.syfo.personstatus.api.v2

import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.routing
import no.nav.syfo.ApplicationState
import no.nav.syfo.Environment
import no.nav.syfo.personstatus.api.v2.auth.installCallId
import no.nav.syfo.personstatus.api.v2.auth.installContentNegotiation
import no.nav.syfo.personstatus.api.v2.auth.installJwtAuthentication
import no.nav.syfo.personstatus.api.v2.auth.installMetrics
import no.nav.syfo.personstatus.api.v2.auth.installStatusPages
import no.nav.syfo.personstatus.api.v2.endpoints.registerPodApi
import no.nav.syfo.personstatus.api.v2.endpoints.registerPrometheusApi
import no.nav.syfo.personstatus.infrastructure.database.DatabaseInterface
import no.nav.syfo.personstatus.infrastructure.clients.azuread.AzureAdClient
import no.nav.syfo.personstatus.infrastructure.clients.veiledertilgang.VeilederTilgangskontrollClient
import no.nav.syfo.personstatus.PersonTildelingService
import no.nav.syfo.personstatus.PersonoversiktStatusService
import no.nav.syfo.personstatus.api.v2.auth.JwtIssuer
import no.nav.syfo.personstatus.api.v2.auth.JwtIssuerType
import no.nav.syfo.personstatus.api.v2.auth.WellKnown
import no.nav.syfo.personstatus.api.v2.endpoints.registerPersonTildelingApiV2
import no.nav.syfo.personstatus.api.v2.endpoints.registerPersonoversiktApiV2

fun Application.apiModule(
    applicationState: ApplicationState,
    database: DatabaseInterface,
    environment: Environment,
    wellKnownVeilederV2: WellKnown,
    azureAdClient: AzureAdClient,
    personoversiktStatusService: PersonoversiktStatusService,
) {
    installCallId()
    installContentNegotiation()
    installMetrics()
    installStatusPages()

    installJwtAuthentication(
        jwtIssuerList = listOf(
            JwtIssuer(
                acceptedAudienceList = listOf(environment.azure.appClientId),
                jwtIssuerType = JwtIssuerType.VEILEDER_V2,
                wellKnown = wellKnownVeilederV2,
            )
        )
    )

    val personTildelingService = PersonTildelingService(
        database = database,
    )

    val tilgangskontrollConsumer = VeilederTilgangskontrollClient(
        azureAdClient = azureAdClient,
        istilgangskontrollEnv = environment.clients.istilgangskontroll,
    )

    routing {
        registerPodApi(
            applicationState = applicationState,
            database = database,
        )
        registerPrometheusApi()
        authenticate(JwtIssuerType.VEILEDER_V2.name) {
            registerPersonoversiktApiV2(
                veilederTilgangskontrollClient = tilgangskontrollConsumer,
                personoversiktStatusService = personoversiktStatusService,
                arenaCutoff = environment.arenaCutoff
            )
            registerPersonTildelingApiV2(tilgangskontrollConsumer, personTildelingService, personoversiktStatusService)
        }
    }
}
