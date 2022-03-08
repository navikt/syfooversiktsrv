package no.nav.syfo.personstatus.api.v2

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.micrometer.core.instrument.Timer
import no.nav.syfo.client.veiledertilgang.VeilederTilgangskontrollClient
import no.nav.syfo.metric.COUNT_PERSONOVERSIKTSTATUS_ENHET_HENTET
import no.nav.syfo.metric.HISTOGRAM_PERSONOVERSIKT
import no.nav.syfo.personstatus.PersonoversiktStatusService
import no.nav.syfo.personstatus.domain.*
import no.nav.syfo.util.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val log: Logger = LoggerFactory.getLogger("no.nav.syfo")

const val personOversiktApiV2Path = "/api/v2/personoversikt"

fun Route.registerPersonoversiktApiV2(
    veilederTilgangskontrollClient: VeilederTilgangskontrollClient,
    personoversiktStatusService: PersonoversiktStatusService
) {
    route(personOversiktApiV2Path) {
        get("/enhet/{enhet}") {
            try {
                val callId = getCallId()
                val token = getBearerHeader()
                    ?: throw IllegalArgumentException("No Authorization header supplied")

                val enhet: String = call.parameters["enhet"]?.takeIf { validateEnhet(it) }
                    ?: throw IllegalArgumentException("Enhet mangler")

                when (veilederTilgangskontrollClient.harVeilederTilgangTilEnhetMedOBO(enhet, token, callId)) {
                    true -> {
                        val requestTimer: Timer.Sample = Timer.start()
                        val personOversiktStatusList: List<PersonOversiktStatus> = personoversiktStatusService
                            .hentPersonoversiktStatusTilknyttetEnhet(enhet)

                        val personFnrListWithVeilederAccess: List<String> = veilederTilgangskontrollClient.veilederPersonAccessListMedOBO(
                            personOversiktStatusList.map { it.fnr },
                            token,
                            callId
                        ) ?: emptyList()

                        val personListNoPdlName = personOversiktStatusList
                            .filter { personFnrListWithVeilederAccess.contains(it.fnr) }

                        if (personListNoPdlName.isNotEmpty()) {
                            val personList = personoversiktStatusService.getPersonOversiktStatusListWithName(
                                callId = callId,
                                personOversiktStatusList = personListNoPdlName
                            ).map { personOversiktStatus ->
                                personOversiktStatus.toPersonOversiktStatusDTO()
                            }

                            call.respond(personList)
                        } else {
                            call.respond(HttpStatusCode.NoContent)
                        }

                        requestTimer.stop(HISTOGRAM_PERSONOVERSIKT)
                        COUNT_PERSONOVERSIKTSTATUS_ENHET_HENTET.increment()
                    }
                    else -> {
                        log.warn("Veileder mangler tilgang til enhet, {}", callIdArgument(callId))
                        call.respond(HttpStatusCode.Forbidden, "Veileder mangler tilgang til enhet")
                    }
                }
            } catch (e: IllegalArgumentException) {
                log.warn("Kan ikke hente personoversikt for enhet: {}, {}", e.message, callIdArgument(getCallId()))
                call.respond(HttpStatusCode.BadRequest, e.message ?: "Kan ikke hente personoversikt for enhet")
            }
        }
    }
}
