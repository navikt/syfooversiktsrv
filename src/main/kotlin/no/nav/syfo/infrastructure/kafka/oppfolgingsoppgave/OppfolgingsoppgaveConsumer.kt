package no.nav.syfo.infrastructure.kafka.oppfolgingsoppgave

import no.nav.syfo.application.oppfolgingsoppgave.OppfolgingsoppgaveRecord
import no.nav.syfo.application.oppfolgingsoppgave.OppfolgingsoppgaveService
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.infrastructure.kafka.KafkaConsumerService
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory
import java.time.Duration

class OppfolgingsoppgaveConsumer(
    private val oppfolgingsoppgaveService: OppfolgingsoppgaveService,
) : KafkaConsumerService<OppfolgingsoppgaveRecord> {

    override val pollDurationInMillis: Long = 1000

    override suspend fun pollAndProcessRecords(consumer: KafkaConsumer<String, OppfolgingsoppgaveRecord>) {
        val records = consumer.poll(Duration.ofMillis(pollDurationInMillis))
        if (records.count() > 0) {
            processRecords(records)
            consumer.commitSync()
        }
    }

    private suspend fun processRecords(records: ConsumerRecords<String, OppfolgingsoppgaveRecord>) {
        val (tombstoneRecords, validRecords) = records.partition { it.value() == null }
        if (tombstoneRecords.isNotEmpty()) {
            val numberOfTombstones = tombstoneRecords.size
            log.error("Value of $numberOfTombstones ConsumerRecord are null, most probably due to a tombstone. Contact the owner of the topic if an error is suspected")
        }
        validRecords.forEach { record ->
            val personident = PersonIdent(record.value().personIdent)
            oppfolgingsoppgaveService.createOrUpdatePersonOversiktStatus(
                personIdent = personident,
                isActiveOppfolgingsoppgave = record.value().isActive
            )
            if (record.value().isActive) {
                oppfolgingsoppgaveService.updateBehandlendeEnhet(
                    personIdent = personident
                )
            }
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)
    }
}
