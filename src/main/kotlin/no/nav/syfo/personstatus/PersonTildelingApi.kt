package no.nav.syfo.personstatus

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.*
import no.nav.syfo.auth.getTokenFromCookie
import no.nav.syfo.auth.isInvalidToken
import no.nav.syfo.metric.COUNT_PERSONTILDELING_TILDEL
import no.nav.syfo.metric.COUNT_PERSONTILDELING_TILDELT
import no.nav.syfo.personstatus.domain.VeilederBrukerKnytning
import no.nav.syfo.personstatus.domain.VeilederBrukerKnytningListe
import no.nav.syfo.tilgangskontroll.TilgangskontrollConsumer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val log: Logger = LoggerFactory.getLogger("no.nav.syfo")

fun Route.registerPersonTildelingApi(
        tilgangskontrollConsumer: TilgangskontrollConsumer,
        personTildelingService: PersonTildelingService
) {
    route("/api/v1/persontildeling") {

        get("/veileder/{veileder}") {
            if (isInvalidToken(call.request.cookies)) {
                call.respond(HttpStatusCode.Unauthorized)
            } else {
                try {
                    val veileder: String = call.parameters["veileder"]?.takeIf { it.isNotEmpty() }
                            ?: throw IllegalArgumentException("Veileder mangler")

                    val tilknytninger: List<VeilederBrukerKnytning> = personTildelingService.hentBrukertilknytningerPaVeileder(veileder)

                    when {
                        tilknytninger.isNotEmpty() -> call.respond(tilknytninger)
                        else -> call.respond(HttpStatusCode.NoContent)
                    }
                } catch (e: IllegalArgumentException) {
                    log.warn("Kan ikke hente tilknytninger: {}", e.message)
                    call.respond(HttpStatusCode.BadRequest, e.message ?: "Kan ikke hente tilknytninger")
                }
            }
        }

        post("/registrer") {
            if (isInvalidToken(call.request.cookies)) {
                call.respond(HttpStatusCode.Unauthorized)
            } else {
                COUNT_PERSONTILDELING_TILDEL.inc()

                val token = getTokenFromCookie(call.request.cookies)

                val veilederBrukerKnytningerListe: VeilederBrukerKnytningListe = call.receive()

                val veilederBrukerKnytninger: List<VeilederBrukerKnytning> = veilederBrukerKnytningerListe.tilknytninger
                        .filter { tilgangskontrollConsumer.harVeilederTilgangTilPerson(it.fnr, token) }

                if (veilederBrukerKnytninger.isEmpty()) {
                    call.respond(HttpStatusCode.Forbidden)
                } else {
                    personTildelingService.lagreKnytningMellomVeilederOgBruker(veilederBrukerKnytninger)

                    COUNT_PERSONTILDELING_TILDELT.inc(veilederBrukerKnytninger.size.toDouble())

                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }
}
