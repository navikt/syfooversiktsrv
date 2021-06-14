package no.nav.syfo.api

import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.routing.*
import no.nav.syfo.ApplicationState
import no.nav.syfo.Environment
import no.nav.syfo.api.authentication.*
import no.nav.syfo.client.azuread.AzureAdV2Client
import no.nav.syfo.db.DatabaseInterface
import no.nav.syfo.personstatus.PersonTildelingService
import no.nav.syfo.personstatus.PersonoversiktStatusService
import no.nav.syfo.personstatus.api.v1.registerPersonTildelingApi
import no.nav.syfo.personstatus.api.v1.registerPersonoversiktApi
import no.nav.syfo.personstatus.api.v2.registerPersonTildelingApiV2
import no.nav.syfo.personstatus.api.v2.registerPersonoversiktApiV2
import no.nav.syfo.client.veiledertilgang.VeilederTilgangskontrollClient

fun Application.apiModule(
    applicationState: ApplicationState,
    database: DatabaseInterface,
    environment: Environment,
    wellKnownVeileder: WellKnown,
    wellKnownVeilederV2: WellKnown
) {
    installCallId()
    installContentNegotiation()
    installStatusPages()

    installJwtAuthentication(
        jwtIssuerList = listOf(
            JwtIssuer(
                accectedAudienceList = listOf(environment.clientid),
                jwtIssuerType = JwtIssuerType.VEILEDER,
                wellKnown = wellKnownVeileder
            ),
            JwtIssuer(
                accectedAudienceList = listOf(environment.azureAppClientId),
                jwtIssuerType = JwtIssuerType.VEILEDER_V2,
                wellKnown = wellKnownVeilederV2
            )
        )
    )

    val personTildelingService = PersonTildelingService(database)
    val personoversiktStatusService = PersonoversiktStatusService(database)

    val azureAdV2Client = AzureAdV2Client(
        aadAppClient = environment.azureAppClientId,
        aadAppSecret = environment.azureAppClientSecret,
        aadTokenEndpoint = environment.azureTokenEndpoint
    )
    val syfotilgangskontrollClientId = environment.syfotilgangskontrollClientId
    val tilgangskontrollConsumer = VeilederTilgangskontrollClient(
        endpointUrl = environment.syfotilgangskontrollUrl,
        azureAdV2Client = azureAdV2Client,
        syfotilgangskontrollClientId = syfotilgangskontrollClientId
    )

    routing {
        registerPodApi(applicationState)
        registerPrometheusApi()
        authenticate(JwtIssuerType.VEILEDER.name) {
            registerPersonoversiktApi(tilgangskontrollConsumer, personoversiktStatusService)
            registerPersonTildelingApi(tilgangskontrollConsumer, personTildelingService)
        }
        authenticate(JwtIssuerType.VEILEDER_V2.name) {
            registerPersonoversiktApiV2(tilgangskontrollConsumer, personoversiktStatusService)
            registerPersonTildelingApiV2(tilgangskontrollConsumer, personTildelingService)
        }
    }

    applicationState.initialized = true
}
