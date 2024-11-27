package no.nav.syfo.personstatus.application

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import no.nav.syfo.personstatus.application.aktivitetskrav.GetAktivitetskravForPersonsResponseDTO
import no.nav.syfo.personstatus.application.aktivitetskrav.IAktivitetskravClient
import no.nav.syfo.personstatus.application.arbeidsuforhet.ArbeidsuforhetvurderingerResponseDTO
import no.nav.syfo.personstatus.application.arbeidsuforhet.IArbeidsuforhetvurderingClient
import no.nav.syfo.personstatus.application.manglendemedvirkning.IManglendeMedvirkningClient
import no.nav.syfo.personstatus.application.manglendemedvirkning.ManglendeMedvirkningResponseDTO
import no.nav.syfo.personstatus.application.meroppfolging.IMeroppfolgingClient
import no.nav.syfo.personstatus.application.meroppfolging.SenOppfolgingKandidaterResponseDTO
import no.nav.syfo.personstatus.application.oppfolgingsoppgave.IOppfolgingsoppgaveClient
import no.nav.syfo.personstatus.application.oppfolgingsoppgave.OppfolgingsoppgaverResponseDTO
import no.nav.syfo.personstatus.domain.PersonIdent
import no.nav.syfo.personstatus.domain.PersonOversiktStatus
import org.slf4j.LoggerFactory
import kotlin.collections.associate
import kotlin.collections.filter
import kotlin.collections.isNotEmpty
import kotlin.collections.map
import kotlin.jvm.java
import kotlin.to

class PersonoversiktOppgaverService(
    private val arbeidsuforhetvurderingClient: IArbeidsuforhetvurderingClient,
    private val manglendeMedvirkningClient: IManglendeMedvirkningClient,
    private val aktivitetskravClient: IAktivitetskravClient,
    private val oppfolgingsoppgaveClient: IOppfolgingsoppgaveClient,
    private val merOppfolgingClient: IMeroppfolgingClient,
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    suspend fun getAktiveOppgaver(
        callId: String,
        token: String,
        personer: List<PersonOversiktStatus>,
    ): Map<String, PersonoversiktAktiveOppgaver> {
        val activeOppfolgingsoppgaver = getActiveOppfolgingsoppgaver(
            callId = callId,
            token = token,
            personStatuser = personer,
        )
        val arbeidsuforhetvurderinger = getArbeidsuforhetvurderinger(
            callId = callId,
            token = token,
            personStatuser = personer,
        )
        val manglendeMedvirkning = getManglendeMedvirkningVurderinger(
            callId = callId,
            token = token,
            personStatuser = personer,
        )
        val activeAktivitetskrav = getActiveAktivitetskravForPersons(
            callId = callId,
            token = token,
            personStatuser = personer,
        )
        val senOppfolgingKandidater = getSenOppfolgingKandidaterForPersons(
            callId = callId,
            token = token,
            personStatuser = personer,
        )

        return personer.associate {
            it.fnr to PersonoversiktAktiveOppgaver(
                arbeidsuforhet = arbeidsuforhetvurderinger.await()
                    ?.vurderinger
                    ?.get(it.fnr),
                oppfolgingsoppgave = activeOppfolgingsoppgaver.await()
                    ?.oppfolgingsoppgaver
                    ?.get(it.fnr),
                aktivitetskrav = activeAktivitetskrav.await()
                    ?.aktivitetskravvurderinger
                    ?.get(it.fnr),
                manglendeMedvirkning = manglendeMedvirkning.await()
                    ?.vurderinger
                    ?.get(it.fnr),
                senOppfolgingKandidat = senOppfolgingKandidater.await()
                    ?.kandidater
                    ?.get(it.fnr),
            )
        }
    }

    private fun getArbeidsuforhetvurderinger(
        callId: String,
        token: String,
        personStatuser: List<PersonOversiktStatus>,
    ): Deferred<ArbeidsuforhetvurderingerResponseDTO?> =
        CoroutineScope(Dispatchers.IO).async {
            val personidenterWithArbeidsuforhetvurdering = personStatuser
                .filter { it.isAktivArbeidsuforhetvurdering }
                .map { PersonIdent(it.fnr) }
            if (personidenterWithArbeidsuforhetvurdering.isNotEmpty()) {
                arbeidsuforhetvurderingClient.getLatestVurderinger(
                    callId = callId,
                    token = token,
                    personidenter = personidenterWithArbeidsuforhetvurdering,
                )
            } else {
                null
            }
        }

    private fun getManglendeMedvirkningVurderinger(
        callId: String,
        token: String,
        personStatuser: List<PersonOversiktStatus>,
    ): Deferred<ManglendeMedvirkningResponseDTO?> =
        CoroutineScope(Dispatchers.IO).async {
            val personidenterWithManglendeMedvirkningVurdering = personStatuser
                .filter { it.isAktivManglendeMedvirkningVurdering }
                .map { PersonIdent(it.fnr) }
            if (personidenterWithManglendeMedvirkningVurdering.isNotEmpty()) {
                manglendeMedvirkningClient.getLatestVurderinger(
                    callId = callId,
                    token = token,
                    personidenter = personidenterWithManglendeMedvirkningVurdering,
                )
            } else {
                null
            }
        }

    private fun getActiveOppfolgingsoppgaver(
        callId: String,
        token: String,
        personStatuser: List<PersonOversiktStatus>,
    ): Deferred<OppfolgingsoppgaverResponseDTO?> =
        CoroutineScope(Dispatchers.IO).async {
            val personidenterWithOppfolgingsoppgave = personStatuser
                .filter { it.isAktivOppfolgingsoppgave }
                .map { PersonIdent(it.fnr) }
            if (personidenterWithOppfolgingsoppgave.isNotEmpty()) {
                val response = oppfolgingsoppgaveClient.getActiveOppfolgingsoppgaver(
                    callId = callId,
                    token = token,
                    personidenter = personidenterWithOppfolgingsoppgave,
                )
                if (response == null) {
                    log.warn("Did not find any oppfolgingsoppgaver for enhet ${personStatuser[0].enhet}")
                }
                response
            } else {
                null
            }
        }

    private fun getActiveAktivitetskravForPersons(
        callId: String,
        token: String,
        personStatuser: List<PersonOversiktStatus>,
    ): Deferred<GetAktivitetskravForPersonsResponseDTO?> =
        CoroutineScope(Dispatchers.IO).async {
            val personidenterWithActiveAktivitetskrav = personStatuser
                .filter { it.isAktivAktivitetskravvurdering }
                .map { PersonIdent(it.fnr) }
            if (personidenterWithActiveAktivitetskrav.isNotEmpty()) {
                aktivitetskravClient.getAktivitetskravForPersons(
                    callId = callId,
                    token = token,
                    personidenter = personidenterWithActiveAktivitetskrav,
                )
            } else {
                null
            }
        }

    private fun getSenOppfolgingKandidaterForPersons(
        callId: String,
        token: String,
        personStatuser: List<PersonOversiktStatus>,
    ): Deferred<SenOppfolgingKandidaterResponseDTO?> =
        CoroutineScope(Dispatchers.IO).async {
            val personidenterWithActiveSenOppfolgingKandidat = personStatuser
                .filter { it.isAktivSenOppfolgingKandidat }
                .map { PersonIdent(it.fnr) }
            if (personidenterWithActiveSenOppfolgingKandidat.isNotEmpty()) {
                merOppfolgingClient.getSenOppfolgingKandidater(
                    callId = callId,
                    token = token,
                    personidenter = personidenterWithActiveSenOppfolgingKandidat,
                )
            } else {
                null
            }
        }
}
