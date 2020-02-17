package no.nav.syfo.personstatus

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.*
import no.nav.syfo.auth.getTokenFromCookie
import no.nav.syfo.auth.getVeilederTokenPayload
import no.nav.syfo.metric.COUNT_PERSONTILDELING_TILDEL
import no.nav.syfo.metric.COUNT_PERSONTILDELING_TILDELT
import no.nav.syfo.personstatus.domain.VeilederBrukerKnytning
import no.nav.syfo.personstatus.domain.VeilederBrukerKnytningListe
import no.nav.syfo.tilgangskontroll.TilgangskontrollConsumer
import no.nav.syfo.util.CallIdArgument
import no.nav.syfo.util.getCallId
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val log: Logger = LoggerFactory.getLogger("no.nav.syfo")

fun Route.registerPersonTildelingApi(
        tilgangskontrollConsumer: TilgangskontrollConsumer,
        personTildelingService: PersonTildelingService
) {
    route("/api/v1/persontildeling") {

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
                log.warn("Kan ikke hente tilknytninger: {}, {}", e.message, CallIdArgument(getCallId()))
                call.respond(HttpStatusCode.BadRequest, e.message ?: "Kan ikke hente tilknytninger")
            }

        }

        post("/registrer") {
            val callId = getCallId()
            try {
                COUNT_PERSONTILDELING_TILDEL.inc()

                val token = getTokenFromCookie(call.request.cookies)

                val veilederBrukerKnytningerListe: VeilederBrukerKnytningListe = call.receive()

                val tilknytningFnrListWithVeilederAccess: List<String> = tilgangskontrollConsumer.veilederPersonAccessList(
                        veilederBrukerKnytningerListe.tilknytninger.map { it.fnr },
                        token,
                        callId
                ) ?: emptyList()

                val veilederBrukerKnytninger: List<VeilederBrukerKnytning> = veilederBrukerKnytningerListe.tilknytninger
                        .filter { tilknytningFnrListWithVeilederAccess.contains(it.fnr) }

                if (veilederBrukerKnytninger.isEmpty()) {
                    log.error("tilknytningFnrListWithVeilederAccess size ${tilknytningFnrListWithVeilederAccess.size}")
                    log.error("Kan ikke registrere tilknytning fordi veileder ikke har tilgang til noen av de spesifiserte tilknytningene, {}", CallIdArgument(callId))
                    call.respond(HttpStatusCode.Forbidden)
                } else {
                    personTildelingService.lagreKnytningMellomVeilederOgBruker(veilederBrukerKnytninger)

                    COUNT_PERSONTILDELING_TILDELT.inc(veilederBrukerKnytninger.size.toDouble())

                    call.respond(HttpStatusCode.OK)
                }
            } catch (e: Error) {
                val navIdent = getVeilederTokenPayload(getTokenFromCookie(call.request.cookies)).navIdent
                log.error("Feil under tildeling av bruker for navIdent=$navIdent, ${e.message}", e.cause)
                call.respond(HttpStatusCode.InternalServerError)
            }
        }
    }
}
