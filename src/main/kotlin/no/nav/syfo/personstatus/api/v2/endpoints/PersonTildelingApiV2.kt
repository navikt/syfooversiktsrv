package no.nav.syfo.personstatus.api.v2.endpoints

import io.ktor.http.*
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.syfo.personstatus.api.v2.auth.getNAVIdentFromToken
import no.nav.syfo.personstatus.infrastructure.COUNT_PERSONTILDELING_TILDELT
import no.nav.syfo.personstatus.PersonTildelingService
import no.nav.syfo.personstatus.domain.VeilederBrukerKnytning
import no.nav.syfo.personstatus.domain.VeilederBrukerKnytningListe
import no.nav.syfo.personstatus.infrastructure.clients.veiledertilgang.VeilederTilgangskontrollClient
import no.nav.syfo.personstatus.PersonoversiktStatusService
import no.nav.syfo.personstatus.api.v2.model.VeilederBrukerKnytningDTO
import no.nav.syfo.personstatus.domain.PersonIdent
import no.nav.syfo.util.*
import no.nav.syfo.util.getBearerHeader
import no.nav.syfo.util.getCallId
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.collections.filter
import kotlin.collections.map

private val log: Logger = LoggerFactory.getLogger("no.nav.syfo")

const val personTildelingApiV2Path = "/api/v2/persontildeling"

fun Route.registerPersonTildelingApiV2(
    veilederTilgangskontrollClient: VeilederTilgangskontrollClient,
    personTildelingService: PersonTildelingService,
    personoversiktStatusService: PersonoversiktStatusService,
) {
    route(personTildelingApiV2Path) {
        post("/registrer") {
            val callId = getCallId()
            val token = getBearerHeader()
                ?: throw java.lang.IllegalArgumentException("No Authorization header supplied")
            try {
                val veilederBrukerKnytningerListe: VeilederBrukerKnytningListe = call.receive()

                val tilknytningFnrListWithVeilederAccess: List<String> =
                    veilederTilgangskontrollClient.veilederPersonAccessListMedOBO(
                        veilederBrukerKnytningerListe.tilknytninger.map { it.fnr },
                        token,
                        callId
                    ) ?: emptyList()

                val veilederBrukerKnytninger: List<VeilederBrukerKnytning> = veilederBrukerKnytningerListe.tilknytninger
                    .filter { tilknytningFnrListWithVeilederAccess.contains(it.fnr) }

                if (veilederBrukerKnytninger.isEmpty()) {
                    log.error("tilknytningFnrListWithVeilederAccess size ${tilknytningFnrListWithVeilederAccess.size}")
                    log.error(
                        "Kan ikke registrere tilknytning fordi veileder ikke har tilgang til noen av de spesifiserte tilknytningene, {}",
                        callIdArgument(callId)
                    )
                    call.respond(HttpStatusCode.Forbidden)
                } else {
                    personTildelingService.lagreKnytningMellomVeilederOgBruker(veilederBrukerKnytninger)

                    COUNT_PERSONTILDELING_TILDELT.increment(veilederBrukerKnytninger.size.toDouble())

                    call.respond(HttpStatusCode.OK)
                }
            } catch (e: Error) {
                val navIdent = getNAVIdentFromToken(token)
                log.error("Feil under tildeling av bruker for navIdent=$navIdent, ${e.message}", e.cause)
                call.respond(HttpStatusCode.InternalServerError)
            }
        }

        post("/personer/single") {
            val callId = getCallId()
            val token = getBearerHeader()
                ?: throw java.lang.IllegalArgumentException("No Authorization header supplied")
            try {
                val veilederBrukerKnytning: VeilederBrukerKnytning = call.receive()

                val tilgang = veilederTilgangskontrollClient.getVeilederAccessToPerson(
                    personident = PersonIdent(veilederBrukerKnytning.fnr),
                    token = token,
                    callId = callId
                )
                if (tilgang?.erGodkjent == true) {
                    personTildelingService.lagreKnytningMellomVeilederOgBruker(listOf(veilederBrukerKnytning))
                    call.respond(HttpStatusCode.OK)
                } else {
                    log.error("Kan ikke registrere tilknytning fordi veileder ikke har tilgang til bruker, {}", callIdArgument(callId))
                    call.respond(HttpStatusCode.Forbidden)
                }
            } catch (e: Error) {
                val navIdent = getNAVIdentFromToken(token)
                log.error("Feil under tildeling av bruker for navIdent=$navIdent, ${e.message}", e.cause)
                call.respond(HttpStatusCode.InternalServerError)
            }
        }

        get("/personer/single") {
            try {
                val token = getBearerHeader()
                    ?: throw java.lang.IllegalArgumentException("No Authorization header supplied")
                val personident = call.getPersonIdent()
                    ?: throw IllegalArgumentException("Failed to get veileder/bruker knytning: No $NAV_PERSONIDENT_HEADER supplied in request header")

                val tilgang = veilederTilgangskontrollClient.getVeilederAccessToPerson(
                    personident = personident,
                    token = token,
                    callId = getCallId()
                )
                if (tilgang?.erGodkjent == true) {
                    personoversiktStatusService.getPersonstatus(personident)?.let {
                        call.respond(VeilederBrukerKnytningDTO.fromPersonstatus(it))
                    } ?: call.respond(HttpStatusCode.NoContent)
                } else {
                    call.respond(HttpStatusCode.Forbidden)
                }
            } catch (e: IllegalArgumentException) {
                log.warn("Kan ikke hente veileder/bruker knytning: {}, {}", e.message, callIdArgument(getCallId()))
                call.respond(HttpStatusCode.BadRequest, e.message ?: "Kan ikke hente veileder/bruker knytning")
            }
        }
    }
}
