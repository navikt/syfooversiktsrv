package no.nav.syfo.application.api

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.Environment
import no.nav.syfo.application.api.authentication.*
import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.client.veiledertilgang.VeilederTilgangskontrollClient
import no.nav.syfo.personstatus.PersonTildelingService
import no.nav.syfo.personstatus.PersonoversiktStatusService
import no.nav.syfo.personstatus.api.v2.registerPersonTildelingApiV2
import no.nav.syfo.personstatus.api.v2.registerPersonoversiktApiV2

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
        syfotilgangskontrollEnv = environment.clients.syfotilgangskontroll,
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
            registerPersonTildelingApiV2(tilgangskontrollConsumer, personTildelingService)
        }
    }
}
