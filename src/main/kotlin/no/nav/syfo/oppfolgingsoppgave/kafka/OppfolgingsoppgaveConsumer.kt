package no.nav.syfo.oppfolgingsoppgave.kafka

import no.nav.syfo.oppfolgingsoppgave.OppfolgingsoppgaveService
import no.nav.syfo.personstatus.infrastructure.kafka.KafkaConsumerService
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import java.time.Duration

class OppfolgingsoppgaveConsumer(
    private val oppfolgingsoppgaveService: OppfolgingsoppgaveService,
) : KafkaConsumerService<OppfolgingsoppgaveRecord> {

    override val pollDurationInMillis: Long = 1000

    override suspend fun pollAndProcessRecords(kafkaConsumer: KafkaConsumer<String, OppfolgingsoppgaveRecord>) {
        val records = kafkaConsumer.poll(Duration.ofMillis(pollDurationInMillis))
        if (records.count() > 0) {
            processRecords(records)
            kafkaConsumer.commitSync()
        }
    }

    private fun processRecords(
        consumerRecords: ConsumerRecords<String, OppfolgingsoppgaveRecord>,
    ) {
        val validRecords = consumerRecords.requireNoNulls()
        oppfolgingsoppgaveService.processOppfolgingsoppgave(
            records = validRecords.map { it.value().toOppfolgingsoppgave() }
        )
    }
}
