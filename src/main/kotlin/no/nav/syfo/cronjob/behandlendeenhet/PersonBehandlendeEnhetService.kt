package no.nav.syfo.cronjob.behandlendeenhet

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.client.behandlendeenhet.BehandlendeEnhetClient
import no.nav.syfo.domain.PersonIdent
import java.util.*

class PersonBehandlendeEnhetService(
    private val database: DatabaseInterface,
    private val behandlendeEnhetClient: BehandlendeEnhetClient,
) {
    fun getPersonIdentToUpdateTildeltEnhetList(): List<PersonIdent> =
        database.getPersonIdentToUpdateTildeltEnhetList()

    suspend fun updateBehandlendeEnhet(
        personIdent: PersonIdent,
    ) {
        behandlendeEnhetClient.getEnhet(
            callId = UUID.randomUUID().toString(),
            personIdent = personIdent,
        )?.let {
            database.updatePersonTildeltEnhet(
                personIdent = personIdent,
                enhetId = it.enhetId,
            )
        } ?: database.updatePersonTildeltEnhetUpdatedAt(
            personIdent = personIdent,
        )
    }
}
