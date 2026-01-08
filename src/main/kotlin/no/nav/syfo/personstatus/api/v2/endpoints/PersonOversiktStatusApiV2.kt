package no.nav.syfo.personstatus.api.v2.endpoints

import io.ktor.http.*
import io.ktor.server.application.call
import io.ktor.server.request.*
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.micrometer.core.instrument.Timer
import no.nav.syfo.personstatus.application.PersonoversiktOppgaverService
import no.nav.syfo.personstatus.application.PersonoversiktSearchService
import no.nav.syfo.personstatus.infrastructure.clients.veiledertilgang.VeilederTilgangskontrollClient
import no.nav.syfo.personstatus.infrastructure.COUNT_PERSONOVERSIKTSTATUS_ENHET_HENTET
import no.nav.syfo.personstatus.infrastructure.HISTOGRAM_PERSONOVERSIKT
import no.nav.syfo.personstatus.application.PersonoversiktStatusService
import no.nav.syfo.personstatus.api.v2.model.SearchQueryDTO
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
private const val NAV_UTLAND_ENHETID = "0393"

const val personOversiktApiV2Path = "/api/v2/personoversikt"

fun Route.registerPersonoversiktApiV2(
    veilederTilgangskontrollClient: VeilederTilgangskontrollClient,
    personoversiktStatusService: PersonoversiktStatusService,
    personoversiktOppgaverService: PersonoversiktOppgaverService,
    personoversiktSearchService: PersonoversiktSearchService,
) {
    route(personOversiktApiV2Path) {
        post("/search") {
            val callId = getCallId()
            val token = getBearerHeader()
                ?: throw java.lang.IllegalArgumentException("No Authorization header supplied")
            val searchQuery = call.receive<SearchQueryDTO>().toSearchQuery()

            val searchResult = personoversiktSearchService.searchSykmeldt(search = searchQuery)
            val fnrWithVeilederAccess = veilederTilgangskontrollClient.veilederPersonAccessListMedOBO(
                personidenter = searchResult.map { it.fnr },
                token = token,
                callId = callId,
            ) ?: emptyList()

            val personer = searchResult.filter { fnrWithVeilederAccess.contains(it.fnr) }
            log.info("Completed search for sykmeldt, found ${personer.size} personer")

            if (personer.isNotEmpty()) {
                val personerAktiveOppgaver = personoversiktOppgaverService.getAktiveOppgaver(
                    callId = callId,
                    token = token,
                    personer = personer,
                )
                val personOversiktStatusDTOs = personer.map {
                    val aktiveOppgaver = personerAktiveOppgaver[it.fnr]
                    it.toPersonOversiktStatusDTO(
                        arbeidsuforhetvurdering = aktiveOppgaver?.arbeidsuforhet,
                        oppfolgingsoppgave = aktiveOppgaver?.oppfolgingsoppgave,
                        aktivitetskravvurdering = aktiveOppgaver?.aktivitetskrav,
                        manglendeMedvirkning = aktiveOppgaver?.manglendeMedvirkning,
                        senOppfolgingKandidat = aktiveOppgaver?.senOppfolgingKandidat,
                        dialogmotekandidatStatus = aktiveOppgaver?.dialogmotekandidat,
                    )
                }

                call.respond(personOversiktStatusDTOs)
            } else {
                call.respond(HttpStatusCode.NoContent)
            }
        }
        get("/enhet/{enhet}") {
            try {
                val callId = getCallId()
                val token = getBearerHeader()
                    ?: throw java.lang.IllegalArgumentException("No Authorization header supplied")

                val enhet: String = call.parameters["enhet"]?.takeIf { validateEnhet(it) }
                    ?: throw java.lang.IllegalArgumentException("Enhet mangler")

                if (enhet == NAV_UTLAND_ENHETID) {
                    log.warn("Enhetens oversikt lastes for Nav utland")
                }
                when (veilederTilgangskontrollClient.harVeilederTilgangTilEnhetMedOBO(enhet, token, callId)) {
                    true -> {
                        val requestTimer: Timer.Sample = Timer.start()
                        val personOversiktStatusList: List<PersonOversiktStatus> = personoversiktStatusService
                            .hentPersonoversiktStatusTilknyttetEnhet(
                                enhet = enhet,
                            )

                        val personFnrListWithVeilederAccess: List<String> =
                            veilederTilgangskontrollClient.veilederPersonAccessListMedOBO(
                                personidenter = personOversiktStatusList.map { it.fnr },
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
                                    dialogmotekandidatStatus = aktiveOppgaver?.dialogmotekandidat,
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
