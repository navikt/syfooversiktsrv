package no.nav.syfo.personstatus.infrastructure.cronjob.behandlendeenhet

import no.nav.syfo.personstatus.infrastructure.database.DatabaseInterface
import no.nav.syfo.personstatus.infrastructure.clients.behandlendeenhet.BehandlendeEnhetClient
import no.nav.syfo.domain.PersonIdent
import java.util.*

class PersonBehandlendeEnhetService(
    private val database: DatabaseInterface,
    private val behandlendeEnhetClient: BehandlendeEnhetClient,
) {
    fun getPersonerToCheckForUpdatedEnhet(): List<Pair<PersonIdent, String?>> =
        database.getPersonerWithOppgaveAndOldEnhet()

    suspend fun updateBehandlendeEnhet(
        personIdent: PersonIdent,
        tildeltEnhet: String?,
    ) {
        val maybeNewBehandlendeEnhet = behandlendeEnhetClient.getEnhet(
            callId = UUID.randomUUID().toString(),
            personIdent = personIdent,
        )
        if (maybeNewBehandlendeEnhet != null && maybeNewBehandlendeEnhet.enhetId != tildeltEnhet) {
            database.updatePersonTildeltEnhetAndRemoveTildeltVeileder(
                personIdent = personIdent,
                enhetId = maybeNewBehandlendeEnhet.enhetId,
            )
        } else {
            database.updatePersonTildeltEnhetUpdatedAt(
                personIdent = personIdent,
            )
        }
    }
}
