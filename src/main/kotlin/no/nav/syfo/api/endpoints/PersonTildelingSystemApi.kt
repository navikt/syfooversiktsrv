package no.nav.syfo.api.endpoints

import io.ktor.http.*
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.syfo.api.access.APIConsumerAccessService
import no.nav.syfo.application.PersonoversiktStatusService
import no.nav.syfo.api.model.VeilederBrukerKnytningDTO
import no.nav.syfo.util.*
import no.nav.syfo.util.getBearerHeader
import no.nav.syfo.util.getCallId
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val log: Logger = LoggerFactory.getLogger("no.nav.syfo")

const val personTildelingSystemApiPath = "/api/v1/system/persontildeling"

fun Route.registerPersonoversiktSystemApi(
    apiConsumerAccessService: APIConsumerAccessService,
    personoversiktStatusService: PersonoversiktStatusService,
    authorizedApplicationNames: List<String>,
) {
    route(personTildelingSystemApiPath) {
        get("/personer/single") {
            try {
                val token = getBearerHeader()
                    ?: throw java.lang.IllegalArgumentException("No Authorization header supplied")
                val personident = call.getPersonIdent()
                    ?: throw IllegalArgumentException("Failed to get veileder/bruker knytning: No $NAV_PERSONIDENT_HEADER supplied in request header")

                apiConsumerAccessService.validateConsumerApplicationAZP(
                    authorizedApplicationNames = authorizedApplicationNames,
                    token = token,
                )
                personoversiktStatusService.getPersonstatus(personident)?.let {
                    call.respond(VeilederBrukerKnytningDTO.fromPersonstatus(it))
                } ?: call.respond(HttpStatusCode.NoContent)
            } catch (e: IllegalArgumentException) {
                log.warn("Kan ikke hente veileder/bruker knytning: {}, {}", e.message, callIdArgument(getCallId()))
                call.respond(HttpStatusCode.BadRequest, e.message ?: "Kan ikke hente veileder/bruker knytning")
            }
        }
    }
}
