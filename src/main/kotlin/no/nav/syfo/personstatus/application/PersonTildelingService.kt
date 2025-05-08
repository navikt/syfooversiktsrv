package no.nav.syfo.personstatus.application

import no.nav.syfo.personstatus.domain.PersonIdent
import no.nav.syfo.personstatus.domain.PersonOversiktStatus
import no.nav.syfo.personstatus.domain.VeilederBrukerKnytning
import no.nav.syfo.personstatus.infrastructure.clients.veileder.VeilederClient
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

            val arbeidstakerEnhet = if (personoversiktStatus.enhet == null) {
                personBehandlendeEnhetService.updateBehandlendeEnhet(personident)
            } else {
                personoversiktStatus.enhet
            } ?: throw IllegalStateException("Enhet for arbeidstaker er null")

            val veiledereForArbeidstakerEnhet = veilederClient.getVeiledereForEnhet(
                callId = callId,
                enhetId = arbeidstakerEnhet,
                token = token,
            )
            val isVeilederInArbeidstakerEnhet = veiledereForArbeidstakerEnhet.any { it.enabled && it.ident == veilederBrukerKnytning.veilederIdent }
            if (!isVeilederInArbeidstakerEnhet) {
                val message = "Kan ikke tildele veileder ${veilederBrukerKnytning.veilederIdent} som ikke er tilknyttet enhet $arbeidstakerEnhet. "
                log.warn("$message Tildelt av $tildeltAv, PersonOversiktStatus uuid: ${personoversiktStatus.uuid}")
                // throw IllegalStateException(message)
            }
            // TODO: Flytt inn i else-blokken når vi har fått rullet ut vo-ruting til alle
            personoversiktStatusRepository.lagreVeilederForBruker(
                veilederBrukerKnytning = veilederBrukerKnytning,
                tildeltAv = tildeltAv,
            )
        }
    }

    fun getVeilederTilknytningHistorikk(personident: PersonIdent) =
        personoversiktStatusRepository.getVeilederTilknytningHistorikk(personident)

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)
    }
}
