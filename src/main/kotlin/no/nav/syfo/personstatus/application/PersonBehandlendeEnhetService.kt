package no.nav.syfo.personstatus.application

import no.nav.syfo.personstatus.infrastructure.clients.behandlendeenhet.BehandlendeEnhetClient
import no.nav.syfo.personstatus.domain.PersonIdent
import java.util.*

class PersonBehandlendeEnhetService(
    private val personoversiktStatusRepository: IPersonOversiktStatusRepository,
    private val behandlendeEnhetClient: BehandlendeEnhetClient,
) {
    fun getPersonerToCheckForUpdatedEnhet(): List<Pair<PersonIdent, String?>> =
        personoversiktStatusRepository.getPersonerWithOppgaveAndOldEnhet()

    suspend fun updateBehandlendeEnhet(
        personIdent: PersonIdent,
        tildeltEnhet: String? = null,
    ) {
        val maybeNewBehandlendeEnhet = behandlendeEnhetClient.getEnhet(
            callId = UUID.randomUUID().toString(),
            personIdent = personIdent,
        )
        if (maybeNewBehandlendeEnhet != null && maybeNewBehandlendeEnhet.enhetId != tildeltEnhet) {
            personoversiktStatusRepository.updatePersonTildeltEnhetAndRemoveTildeltVeileder(
                personIdent = personIdent,
                enhetId = maybeNewBehandlendeEnhet.enhetId,
            )
        } else {
            personoversiktStatusRepository.updatePersonTildeltEnhetUpdatedAt(
                personIdent = personIdent,
            )
        }
    }
}
