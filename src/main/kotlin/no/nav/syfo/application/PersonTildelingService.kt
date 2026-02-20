package no.nav.syfo.application

import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.domain.PersonOversiktStatus
import no.nav.syfo.domain.VeilederBrukerKnytning
import no.nav.syfo.infrastructure.clients.veileder.VeilederClient
import org.slf4j.LoggerFactory
import kotlin.also
import kotlin.collections.forEach

class PersonTildelingService(
    private val personoversiktStatusRepository: IPersonOversiktStatusRepository,
    private val personBehandlendeEnhetService: PersonBehandlendeEnhetService,
    private val veilederClient: VeilederClient,
) {
    suspend fun lagreKnytningMellomVeilederOgBruker(
        veilederBrukerKnytninger: List<VeilederBrukerKnytning>,
        tildeltAv: String,
        token: String,
        callId: String,
    ) {
        veilederBrukerKnytninger.forEach { veilederBrukerKnytning ->
            val personident = PersonIdent(veilederBrukerKnytning.fnr)
            val personoversiktStatus = personoversiktStatusRepository.getPersonOversiktStatus(personident)
                ?: PersonOversiktStatus(fnr = veilederBrukerKnytning.fnr).also { personOversiktStatus ->
                    personoversiktStatusRepository.createPersonOversiktStatus(personOversiktStatus)
                }
            if (veilederBrukerKnytning.veilederIdent != null) {
                validateVeilederInArbeidstakersEnhet(
                    callId = callId,
                    token = token,
                    tildeltAv = tildeltAv,
                    veilederBrukerKnytning = veilederBrukerKnytning,
                    personoversiktStatus = personoversiktStatus
                )
            }
            personoversiktStatusRepository.lagreVeilederForBruker(
                veilederBrukerKnytning = veilederBrukerKnytning,
                tildeltAv = tildeltAv,
            )
        }
    }

    fun getVeilederTilknytningHistorikk(personident: PersonIdent) =
        personoversiktStatusRepository.getVeilederTilknytningHistorikk(personident)

    private suspend fun validateVeilederInArbeidstakersEnhet(
        callId: String,
        token: String,
        tildeltAv: String,
        veilederBrukerKnytning: VeilederBrukerKnytning,
        personoversiktStatus: PersonOversiktStatus,
    ) {
        val arbeidstakerEnhet = personoversiktStatus.enhet
            ?: personBehandlendeEnhetService.updateBehandlendeEnhet(PersonIdent(veilederBrukerKnytning.fnr))
            ?: throw IllegalStateException("Enhet for arbeidstaker er null")
        val veiledereForArbeidstakerEnhet = veilederClient.getVeiledereForEnhet(
            callId = callId,
            enhetId = arbeidstakerEnhet,
            token = token,
        )
        val isVeilederInArbeidstakerEnhet =
            veiledereForArbeidstakerEnhet.any { it.enabled && it.ident == veilederBrukerKnytning.veilederIdent }
        if (!isVeilederInArbeidstakerEnhet) {
            val message =
                "Kan ikke tildele veileder ${veilederBrukerKnytning.veilederIdent} som ikke er tilknyttet enhet $arbeidstakerEnhet. "
            log.warn("$message Tildelt av $tildeltAv, PersonOversiktStatus uuid: ${personoversiktStatus.uuid}")
            // throw IllegalStateException(message)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)
    }
}
