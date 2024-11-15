package no.nav.syfo.personstatus.infrastructure.cronjob.behandlendeenhet

import no.nav.syfo.personstatus.infrastructure.clients.behandlendeenhet.BehandlendeEnhetClient
import no.nav.syfo.personstatus.domain.PersonIdent
import no.nav.syfo.personstatus.infrastructure.database.repository.PersonOversiktStatusRepository
import java.util.*

class PersonBehandlendeEnhetService(
    private val personoversiktStatusRepository: PersonOversiktStatusRepository,
    private val behandlendeEnhetClient: BehandlendeEnhetClient,
) {
    fun getAktivePersonerWithOutdatedEnhet(): List<Pair<PersonIdent, String?>> =
        personoversiktStatusRepository.getAktivePersonerWithOutdatedEnhet()

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
