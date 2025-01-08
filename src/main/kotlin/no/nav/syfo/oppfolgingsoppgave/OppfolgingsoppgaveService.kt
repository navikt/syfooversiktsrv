package no.nav.syfo.oppfolgingsoppgave

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import no.nav.syfo.personstatus.infrastructure.database.DatabaseInterface
import no.nav.syfo.personstatus.application.PersonBehandlendeEnhetService
import no.nav.syfo.personstatus.domain.PersonIdent
import no.nav.syfo.personstatus.infrastructure.database.queries.createPersonOversiktStatus
import no.nav.syfo.oppfolgingsoppgave.domain.Oppfolgingsoppgave
import no.nav.syfo.oppfolgingsoppgave.kafka.COUNT_KAFKA_CONSUMER_TRENGER_OPPFOLGING_READ
import no.nav.syfo.personstatus.infrastructure.database.queries.getPersonOversiktStatusList
import no.nav.syfo.personstatus.infrastructure.database.queries.updateOppfolgingsoppgave
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class OppfolgingsoppgaveService(
    private val database: DatabaseInterface,
    private val personBehandlendeEnhetService: PersonBehandlendeEnhetService,
) {
    fun processOppfolgingsoppgave(records: List<Oppfolgingsoppgave>) {
        createOrUpdatePersonOversiktStatus(records)
        records.filter { it.isActive }.forEach {
            val personOversiktStatus = database.getPersonOversiktStatusList(
                fnr = it.personIdent.value,
            ).first()
            updateBehandlendeEnhet(it.personIdent, personOversiktStatus.enhet)
        }
    }

    private fun createOrUpdatePersonOversiktStatus(records: List<Oppfolgingsoppgave>) {
        database.connection.use { connection ->
            records.forEach { oppfolgingsOppgave ->
                val existingPersonOversiktStatus = connection.getPersonOversiktStatusList(
                    fnr = oppfolgingsOppgave.personIdent.value,
                ).firstOrNull()

                if (existingPersonOversiktStatus == null) {
                    val personoversiktStatus = oppfolgingsOppgave.toPersonoversiktStatus()
                    connection.createPersonOversiktStatus(
                        commit = false,
                        personOversiktStatus = personoversiktStatus,
                    )
                } else {
                    connection.updateOppfolgingsoppgave(oppfolgingsoppgave = oppfolgingsOppgave)
                }

                log.info("Received oppfolgingsOppgave with uuid=${oppfolgingsOppgave.uuid}")
                COUNT_KAFKA_CONSUMER_TRENGER_OPPFOLGING_READ.increment()
            }
            connection.commit()
        }
    }

    private fun updateBehandlendeEnhet(
        personIdent: PersonIdent,
        existingEnhet: String?,
    ) {
        try {
            runBlocking {
                launch(Dispatchers.IO) {
                    personBehandlendeEnhetService.updateBehandlendeEnhet(
                        personIdent = personIdent,
                        tildeltEnhet = existingEnhet
                    )
                    log.info("Updated Behandlende Enhet of person after received active oppfolgingsoppgave")
                }
            }
        } catch (ex: Exception) {
            log.error(
                "Exception caught while attempting to update Behandlende Enhet of person after received active oppfolgingsoppgave",
                ex
            )
        }
    }

    companion object {
        val log: Logger = LoggerFactory.getLogger(this::class.java)
    }
}
