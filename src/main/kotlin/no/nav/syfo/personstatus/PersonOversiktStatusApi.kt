package no.nav.syfo.personstatus

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.*
import no.nav.syfo.auth.getTokenFromCookie
import no.nav.syfo.metric.COUNT_PERSONOVERSIKTSTATUS_ENHET_HENTET
import no.nav.syfo.metric.HISTOGRAM_PERSONOVERSIKT
import no.nav.syfo.personstatus.domain.PersonOversiktStatus
import no.nav.syfo.tilgangskontroll.TilgangskontrollConsumer
import no.nav.syfo.util.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val log: Logger = LoggerFactory.getLogger("no.nav.syfo")

fun Route.registerPersonoversiktApi(
        tilgangskontrollConsumer: TilgangskontrollConsumer,
        personoversiktStatusService: PersonoversiktStatusService
) {
    route("/api/v1/personoversikt") {
        get("/enhet/{enhet}") {
            try {
                val callId = getCallId()
                val token = getTokenFromCookie(call.request.cookies)

                val enhet: String = call.parameters["enhet"]?.takeIf { validateEnhet(it) }
                        ?: throw IllegalArgumentException("Enhet mangler")

                when (tilgangskontrollConsumer.harVeilederTilgangTilEnhet(enhet, token, callId)) {
                    true -> {
                        var requestTimer = HISTOGRAM_PERSONOVERSIKT.startTimer();
                        val personOversiktStatusList: List<PersonOversiktStatus> = personoversiktStatusService
                                .hentPersonoversiktStatusTilknyttetEnhet(enhet)

                        val personFnrListWithVeilederAccess: List<String> = tilgangskontrollConsumer.veilederPersonAccessList(
                                personOversiktStatusList.map { it.fnr },
                                token,
                                callId
                        ) ?: emptyList()

                        val personList = personOversiktStatusList
                                .filter { personFnrListWithVeilederAccess.contains(it.fnr) }

                        when {
                            personList.isNotEmpty() -> call.respond(personList)
                            else -> call.respond(HttpStatusCode.NoContent)
                        }
                        requestTimer.observeDuration()
                        COUNT_PERSONOVERSIKTSTATUS_ENHET_HENTET.inc()
                    }
                    else -> {
                        log.error("Veileder mangler tilgang til enhet, {}", CallIdArgument(callId))
                        call.respond(HttpStatusCode.Forbidden, "Veileder mangler tilgang til enhet")
                    }
                }
            } catch (e: IllegalArgumentException) {
                log.warn("Kan ikke hente personoversikt for enhet: {}, {}", e.message, CallIdArgument(getCallId()))
                call.respond(HttpStatusCode.BadRequest, e.message ?: "Kan ikke hente personoversikt for enhet")
            }
        }

    }
}
