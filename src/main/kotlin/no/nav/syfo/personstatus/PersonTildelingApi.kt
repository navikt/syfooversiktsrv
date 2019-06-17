package no.nav.syfo.personstatus

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.*
import no.nav.syfo.auth.isInvalidToken
import no.nav.syfo.personstatus.domain.VeilederBrukerKnytning
import no.nav.syfo.personstatus.domain.VeilederBrukerKnytningListe
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val log: Logger = LoggerFactory.getLogger("no.nav.syfo")

fun Route.registerPersonTildelingApi(personTildelingService: PersonTildelingService) {
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
                val veilederBrukerKnytningerListe: VeilederBrukerKnytningListe = call.receive()

                val veilederBrukerKnytninger: List<VeilederBrukerKnytning> = veilederBrukerKnytningerListe.tilknytninger

                personTildelingService.lagreKnytningMellomVeilederOgBruker(veilederBrukerKnytninger)

                call.respond(HttpStatusCode.Created)
            }
        }
    }
}
