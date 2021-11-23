package no.nav.syfo.personstatus.api.v2

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import no.nav.syfo.application.api.authentication.getNAVIdentFromToken
import no.nav.syfo.metric.COUNT_PERSONTILDELING_TILDELT
import no.nav.syfo.personstatus.PersonTildelingService
import no.nav.syfo.personstatus.domain.VeilederBrukerKnytning
import no.nav.syfo.personstatus.domain.VeilederBrukerKnytningListe
import no.nav.syfo.client.veiledertilgang.VeilederTilgangskontrollClient
import no.nav.syfo.util.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val log: Logger = LoggerFactory.getLogger("no.nav.syfo")

const val personTildelingApiV2Path = "/api/v2/persontildeling"

fun Route.registerPersonTildelingApiV2(
    veilederTilgangskontrollClient: VeilederTilgangskontrollClient,
    personTildelingService: PersonTildelingService
) {
    route(personTildelingApiV2Path) {

        get("/veileder/{veileder}") {
            try {
                val veileder: String = call.parameters["veileder"]?.takeIf { it.isNotEmpty() }
                    ?: throw IllegalArgumentException("Veileder mangler")

                val tilknytninger: List<VeilederBrukerKnytning> = personTildelingService.hentBrukertilknytningerPaVeileder(veileder)

                when {
                    tilknytninger.isNotEmpty() -> call.respond(tilknytninger)
                    else -> call.respond(HttpStatusCode.NoContent)
                }
            } catch (e: IllegalArgumentException) {
                log.warn("Kan ikke hente tilknytninger: {}, {}", e.message, callIdArgument(getCallId()))
                call.respond(HttpStatusCode.BadRequest, e.message ?: "Kan ikke hente tilknytninger")
            }
        }

        post("/registrer") {
            val callId = getCallId()
            val token = getBearerHeader()
                ?: throw IllegalArgumentException("No Authorization header supplied")
            try {
                val veilederBrukerKnytningerListe: VeilederBrukerKnytningListe = call.receive()

                val tilknytningFnrListWithVeilederAccess: List<String> = veilederTilgangskontrollClient.veilederPersonAccessListMedOBO(
                    veilederBrukerKnytningerListe.tilknytninger.map { it.fnr },
                    token,
                    callId
                ) ?: emptyList()

                val veilederBrukerKnytninger: List<VeilederBrukerKnytning> = veilederBrukerKnytningerListe.tilknytninger
                    .filter { tilknytningFnrListWithVeilederAccess.contains(it.fnr) }

                if (veilederBrukerKnytninger.isEmpty()) {
                    log.error("tilknytningFnrListWithVeilederAccess size ${tilknytningFnrListWithVeilederAccess.size}")
                    log.error("Kan ikke registrere tilknytning fordi veileder ikke har tilgang til noen av de spesifiserte tilknytningene, {}", callIdArgument(callId))
                    call.respond(HttpStatusCode.Forbidden)
                } else {
                    personTildelingService.lagreKnytningMellomVeilederOgBruker(veilederBrukerKnytninger)

                    COUNT_PERSONTILDELING_TILDELT.inc(veilederBrukerKnytninger.size.toDouble())

                    call.respond(HttpStatusCode.OK)
                }
            } catch (e: Error) {
                val navIdent = getNAVIdentFromToken(token)
                log.error("Feil under tildeling av bruker for navIdent=$navIdent, ${e.message}", e.cause)
                call.respond(HttpStatusCode.InternalServerError)
            }
        }
    }
}
