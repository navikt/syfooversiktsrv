package no.nav.syfo.trengeroppfolging

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import no.nav.syfo.personstatus.infrastructure.database.DatabaseInterface
import no.nav.syfo.personstatus.infrastructure.cronjob.behandlendeenhet.PersonBehandlendeEnhetService
import no.nav.syfo.personstatus.domain.PersonIdent
import no.nav.syfo.trengeroppfolging.domain.TrengerOppfolging
import no.nav.syfo.trengeroppfolging.kafka.COUNT_KAFKA_CONSUMER_TRENGER_OPPFOLGING_READ
import no.nav.syfo.personstatus.db.createPersonOversiktStatus
import no.nav.syfo.personstatus.db.getPersonOversiktStatusList
import no.nav.syfo.personstatus.db.updateTrengerOppfolging
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class TrengerOppfolgingService(
    private val database: DatabaseInterface,
    private val personBehandlendeEnhetService: PersonBehandlendeEnhetService,
) {
    fun processTrengerOppfolging(records: List<TrengerOppfolging>) {
        createOrUpdatePersonOversiktStatus(records)
        records.filter { it.isActive }.forEach {
            val personOversiktStatus = database.getPersonOversiktStatusList(
                fnr = it.personIdent.value,
            ).first()
            updateBehandlendeEnhet(it.personIdent, personOversiktStatus.enhet)
        }
    }

    private fun createOrUpdatePersonOversiktStatus(records: List<TrengerOppfolging>) {
        database.connection.use { connection ->
            records.forEach { trengerOppfolging ->
                val existingPersonOversiktStatus = connection.getPersonOversiktStatusList(
                    fnr = trengerOppfolging.personIdent.value,
                ).firstOrNull()

                if (existingPersonOversiktStatus == null) {
                    val personoversiktStatus = trengerOppfolging.toPersonoversiktStatus()
                    connection.createPersonOversiktStatus(
                        commit = false,
                        personOversiktStatus = personoversiktStatus,
                    )
                } else {
                    connection.updateTrengerOppfolging(trengerOppfolging = trengerOppfolging)
                }

                log.info("Received trengerOppfolging with uuid=${trengerOppfolging.uuid}")
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
