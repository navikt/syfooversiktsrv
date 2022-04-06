package no.nav.syfo.cronjob.virksomhetsnavn

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.client.ereg.EregClient
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
