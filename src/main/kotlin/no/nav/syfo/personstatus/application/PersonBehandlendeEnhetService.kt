package no.nav.syfo.personstatus.application

import no.nav.syfo.personstatus.infrastructure.clients.behandlendeenhet.BehandlendeEnhetClient
import no.nav.syfo.personstatus.domain.PersonIdent
import java.util.*

class PersonBehandlendeEnhetService(
    private val personoversiktStatusRepository: IPersonOversiktStatusRepository,
    private val behandlendeEnhetClient: BehandlendeEnhetClient,
) {
    fun getPersonerToCheckForUpdatedEnhet(): List<PersonIdent> =
        personoversiktStatusRepository.getPersonerWithOppgaveAndOldEnhet()

    suspend fun updateBehandlendeEnhet(personIdent: PersonIdent) {
        val behandlendeEnhet = behandlendeEnhetClient.getEnhet(
            callId = UUID.randomUUID().toString(),
            personIdent = personIdent,
        )
        val tildeltEnhet = personoversiktStatusRepository.getPersonOversiktStatus(personIdent)?.enhet
        val isEnhetUpdate = behandlendeEnhet != null && behandlendeEnhet.enhetId != tildeltEnhet

        if (isEnhetUpdate) {
            personoversiktStatusRepository.updatePersonTildeltEnhetAndRemoveTildeltVeileder(
                personIdent = personIdent,
                enhetId = behandlendeEnhet.enhetId,
            )
        } else {
            personoversiktStatusRepository.updatePersonTildeltEnhetUpdatedAt(
                personIdent = personIdent,
            )
        }
    }
}
