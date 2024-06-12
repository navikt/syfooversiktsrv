package no.nav.syfo.personstatus.infrastructure.cronjob.virksomhetsnavn

import no.nav.syfo.personstatus.infrastructure.database.DatabaseInterface
import no.nav.syfo.personstatus.infrastructure.clients.ereg.EregClient
import no.nav.syfo.domain.Virksomhetsnummer
import java.util.*

class PersonOppfolgingstilfelleVirksomhetsnavnService(
    private val database: DatabaseInterface,
    private val eregClient: EregClient,
) {
    fun getMissingVirksomhetsnavnList(): List<Pair<Int, Virksomhetsnummer>> =
        database.getPersonOppfolgingstilfelleVirksomhetMissingVirksomhetsnavnList()

    suspend fun updateVirksomhetsnavn(
        idVirksomhetsnummerPair: Pair<Int, Virksomhetsnummer>,
    ) {
        val (id, virksomhetsnummer) = idVirksomhetsnummerPair
        eregClient.organisasjonVirksomhetsnavn(
            callId = UUID.randomUUID().toString(),
            virksomhetsnummer = virksomhetsnummer,
        )?.let { eregOrganisasjonVirksomhetsnavn ->
            database.updatePersonOppfolgingstilfelleVirksomhetVirksomhetsnavn(
                personOppfolgingstilfelleVirksomhetId = id,
                virksomhetsnavn = eregOrganisasjonVirksomhetsnavn.virksomhetsnavn,
            )
        } ?: throw RuntimeException("Failed to store Virksomhetsnavn: response from Ereg missing")
    }
}
