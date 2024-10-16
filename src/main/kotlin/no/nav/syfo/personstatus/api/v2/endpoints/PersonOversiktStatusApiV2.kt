package no.nav.syfo.personstatus.api.v2.endpoints

import io.ktor.http.*
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.micrometer.core.instrument.Timer
import no.nav.syfo.personstatus.PersonoversiktOppgaverService
import no.nav.syfo.personstatus.infrastructure.clients.veiledertilgang.VeilederTilgangskontrollClient
import no.nav.syfo.personstatus.infrastructure.COUNT_PERSONOVERSIKTSTATUS_ENHET_HENTET
import no.nav.syfo.personstatus.infrastructure.HISTOGRAM_PERSONOVERSIKT
import no.nav.syfo.personstatus.PersonoversiktStatusService
import no.nav.syfo.personstatus.domain.*
import no.nav.syfo.util.*
import no.nav.syfo.util.getBearerHeader
import no.nav.syfo.util.getCallId
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.collections.filter
import kotlin.collections.isNotEmpty
import kotlin.collections.map
import kotlin.takeIf

private val log: Logger = LoggerFactory.getLogger("no.nav.syfo")

const val personOversiktApiV2Path = "/api/v2/personoversikt"

fun Route.registerPersonoversiktApiV2(
    veilederTilgangskontrollClient: VeilederTilgangskontrollClient,
    personoversiktStatusService: PersonoversiktStatusService,
    personoversiktOppgaverService: PersonoversiktOppgaverService,
) {
    route(personOversiktApiV2Path) {
        get("/enhet/{enhet}") {
            try {
                val callId = getCallId()
                val token = getBearerHeader()
                    ?: throw java.lang.IllegalArgumentException("No Authorization header supplied")

                val enhet: String = call.parameters["enhet"]?.takeIf { validateEnhet(it) }
                    ?: throw java.lang.IllegalArgumentException("Enhet mangler")

                when (veilederTilgangskontrollClient.harVeilederTilgangTilEnhetMedOBO(enhet, token, callId)) {
                    true -> {
                        val requestTimer: Timer.Sample = Timer.start()
                        val personOversiktStatusList: List<PersonOversiktStatus> = personoversiktStatusService
                            .hentPersonoversiktStatusTilknyttetEnhet(
                                enhet = enhet,
                            )

                        val personFnrListWithVeilederAccess: List<String> =
                            veilederTilgangskontrollClient.veilederPersonAccessListMedOBO(
                                personIdentNumberList = personOversiktStatusList.map { it.fnr },
                                token = token,
                                callId = callId,
                            ) ?: emptyList()

                        val personer = personOversiktStatusList
                            .filter { personFnrListWithVeilederAccess.contains(it.fnr) }

                        if (personer.isNotEmpty()) {
                            val personerWithName = personoversiktStatusService.getPersonOversiktStatusListWithName(
                                callId = callId,
                                personOversiktStatusList = personer,
                            )
                            val personerAktiveOppgaver = personoversiktOppgaverService.getAktiveOppgaver(
                                callId = callId,
                                token = token,
                                personer = personerWithName,
                            )
                            val personOversiktStatusDTO = personerWithName.map {
                                val aktiveOppgaver = personerAktiveOppgaver[it.fnr]
                                it.toPersonOversiktStatusDTO(
                                    arbeidsuforhetvurdering = aktiveOppgaver?.arbeidsuforhet,
                                    oppfolgingsoppgave = aktiveOppgaver?.oppfolgingsoppgave,
                                    aktivitetskravvurdering = aktiveOppgaver?.aktivitetskrav,
                                    manglendeMedvirkning = aktiveOppgaver?.manglendeMedvirkning,
                                    senOppfolgingKandidat = aktiveOppgaver?.senOppfolgingKandidat,
                                )
                            }

                            call.respond(personOversiktStatusDTO)
                        } else {
                            call.respond(HttpStatusCode.NoContent)
                        }

                        requestTimer.stop(HISTOGRAM_PERSONOVERSIKT)
                        COUNT_PERSONOVERSIKTSTATUS_ENHET_HENTET.increment()
                    }

                    else -> {
                        log.warn("Veileder mangler tilgang til enhet $enhet, {}", callIdArgument(callId))
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
