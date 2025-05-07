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
        val behandlendeEnhetResponseDTO = behandlendeEnhetClient.getEnhet(
            callId = UUID.randomUUID().toString(),
            personIdent = personIdent,
        )

        val behandlendeEnhetId = behandlendeEnhetResponseDTO?.oppfolgingsenhet?.enhetId
            ?: behandlendeEnhetResponseDTO?.geografiskEnhet?.enhetId

        val tildeltEnhetId = personoversiktStatusRepository.getPersonOversiktStatus(personIdent)?.enhet

        val isEnhetChanged = behandlendeEnhetId != tildeltEnhetId
        if (isEnhetChanged && behandlendeEnhetId != null) {
            personoversiktStatusRepository.updatePersonTildeltEnhetAndRemoveTildeltVeileder(
                personIdent = personIdent,
                enhetId = behandlendeEnhetId,
            )
        } else {
            personoversiktStatusRepository.updatePersonTildeltEnhetUpdatedAt(
                personIdent = personIdent,
            )
        }
        return behandlendeEnhetId
    }
}
