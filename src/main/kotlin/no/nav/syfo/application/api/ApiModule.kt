package no.nav.syfo.application.api

import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.routing.*
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
) {
    installCallId()
    installContentNegotiation()
    installMetrics()
    installStatusPages()

    installJwtAuthentication(
        jwtIssuerList = listOf(
            JwtIssuer(
                acceptedAudienceList = listOf(environment.azureAppClientId),
                jwtIssuerType = JwtIssuerType.VEILEDER_V2,
                wellKnown = wellKnownVeilederV2,
            )
        )
    )

    val personTildelingService = PersonTildelingService(
        database = database,
    )
    val personoversiktStatusService = PersonoversiktStatusService(
        database = database,
    )

    val azureAdClient = AzureAdClient(
        aadAppClient = environment.azureAppClientId,
        aadAppSecret = environment.azureAppClientSecret,
        aadTokenEndpoint = environment.azureTokenEndpoint,
    )
    val syfotilgangskontrollClientId = environment.syfotilgangskontrollClientId
    val tilgangskontrollConsumer = VeilederTilgangskontrollClient(
        endpointUrl = environment.syfotilgangskontrollUrl,
        azureAdClient = azureAdClient,
        syfotilgangskontrollClientId = syfotilgangskontrollClientId,
    )

    routing {
        registerPodApi(
            applicationState = applicationState,
            database = database,
        )
        registerPrometheusApi()
        authenticate(JwtIssuerType.VEILEDER_V2.name) {
            registerPersonoversiktApiV2(tilgangskontrollConsumer, personoversiktStatusService)
            registerPersonTildelingApiV2(tilgangskontrollConsumer, personTildelingService)
        }
    }
}
