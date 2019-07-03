package no.nav.syfo.personstatus

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.*
import no.nav.syfo.auth.getTokenFromCookie
import no.nav.syfo.auth.isInvalidToken
import no.nav.syfo.metric.COUNT_PERSONOVERSIKTSTATUS_ENHET_HENTET
import no.nav.syfo.personstatus.domain.PersonOversiktStatus
import no.nav.syfo.tilgangskontroll.TilgangskontrollConsumer
import no.nav.syfo.util.validateEnhet
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val log: Logger = LoggerFactory.getLogger("no.nav.syfo")

fun Route.registerPersonoversiktApi(
        tilgangskontrollConsumer: TilgangskontrollConsumer,
        personoversiktStatusService: PersonoversiktStatusService
) {
    route("/api/v1/personoversikt") {

        get("/enhet/{enhet}") {
            if (isInvalidToken(call.request.cookies)) {
                call.respond(HttpStatusCode.Unauthorized)
            } else {
                try {
                    val token = getTokenFromCookie(call.request.cookies)

                    val enhet: String = call.parameters["enhet"]?.takeIf { validateEnhet(it) }
                            ?: throw IllegalArgumentException("Enhet mangler")

                    val harTilgangTilEnhet = tilgangskontrollConsumer.harVeilederTilgangTilEnhet(enhet, getTokenFromCookie(call.request.cookies))

                    if (harTilgangTilEnhet) {
                        val personListe: List<PersonOversiktStatus> = personoversiktStatusService.hentPersonoversiktStatusTilknyttetEnhet(enhet)
                                .filter { tilgangskontrollConsumer.harVeilederTilgangTilPerson(it.fnr, token) }

                        when {
                            personListe.isNotEmpty() -> call.respond(personListe)
                            else -> call.respond(HttpStatusCode.NoContent)
                        }

                        COUNT_PERSONOVERSIKTSTATUS_ENHET_HENTET.inc()
                    } else {
                        call.respond(HttpStatusCode.Forbidden)
                    }
                } catch (e: IllegalArgumentException) {
                    log.warn("Kan ikke hente personoversikt for enhet: {}", e.message)
                    call.respond(HttpStatusCode.BadRequest, e.message ?: "Kan ikke hente personoversikt for enhet")
                }
            }
        }
    }
}
