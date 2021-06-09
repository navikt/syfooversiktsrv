package no.nav.syfo.api

import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.routing.*
import no.nav.syfo.*
import no.nav.syfo.api.authentication.*
import no.nav.syfo.db.DatabaseInterface
import no.nav.syfo.personstatus.*
import no.nav.syfo.tilgangskontroll.TilgangskontrollConsumer

fun Application.apiModule(
    applicationState: ApplicationState,
    database: DatabaseInterface,
    environment: Environment,
    wellKnownVeileder: WellKnown
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
            )
        )
    )

    val personTildelingService = PersonTildelingService(database)
    val personoversiktStatusService = PersonoversiktStatusService(database)
    val tilgangskontrollConsumer = TilgangskontrollConsumer(environment.syfotilgangskontrollUrl)

    routing {
        registerPodApi(applicationState)
        registerPrometheusApi()
        authenticate(JwtIssuerType.VEILEDER.name) {
            registerPersonoversiktApi(tilgangskontrollConsumer, personoversiktStatusService)
            registerPersonTildelingApi(tilgangskontrollConsumer, personTildelingService)
        }
    }

    applicationState.initialized = true
}
