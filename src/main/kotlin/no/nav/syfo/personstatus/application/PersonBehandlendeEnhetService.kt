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

    suspend fun updateBehandlendeEnhet(personIdent: PersonIdent): String? {
        val behandlendeEnhet = behandlendeEnhetClient.getEnhet(
            callId = UUID.randomUUID().toString(),
            personIdent = personIdent,
        )?.oppfolgingsenhet?.enhetId

        val tildeltEnhet = personoversiktStatusRepository.getPersonOversiktStatus(personIdent)?.enhet

        val isEnhetChanged = behandlendeEnhet != tildeltEnhet
        if (isEnhetChanged && behandlendeEnhet != null) {
            personoversiktStatusRepository.updatePersonTildeltEnhetAndRemoveTildeltVeileder(
                personIdent = personIdent,
                enhetId = behandlendeEnhet,
            )
        } else {
            personoversiktStatusRepository.updatePersonTildeltEnhetUpdatedAt(
                personIdent = personIdent,
            )
        }
        return behandlendeEnhet
    }
}
