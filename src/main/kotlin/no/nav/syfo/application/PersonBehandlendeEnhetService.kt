package no.nav.syfo.application

import no.nav.syfo.infrastructure.clients.behandlendeenhet.BehandlendeEnhetClient
import no.nav.syfo.domain.PersonIdent
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

        val behandlendeEnhetId = behandlendeEnhetResponseDTO?.oppfolgingsenhetDTO?.enhet?.enhetId
            ?: behandlendeEnhetResponseDTO?.geografiskEnhet?.enhetId

        val tildeltEnhetId = personoversiktStatusRepository.getPersonOversiktStatus(personIdent)?.enhet

        if (behandlendeEnhetId != tildeltEnhetId && behandlendeEnhetId != null) {
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
