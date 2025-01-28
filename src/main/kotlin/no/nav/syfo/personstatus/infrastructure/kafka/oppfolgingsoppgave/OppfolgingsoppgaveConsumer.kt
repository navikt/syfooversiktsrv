package no.nav.syfo.personstatus.infrastructure.kafka.oppfolgingsoppgave

import no.nav.syfo.personstatus.application.oppfolgingsoppgave.OppfolgingsoppgaveRecord
import no.nav.syfo.personstatus.application.oppfolgingsoppgave.OppfolgingsoppgaveService
import no.nav.syfo.personstatus.infrastructure.kafka.KafkaConsumerService
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import java.time.Duration
import kotlin.collections.map
import kotlin.collections.requireNoNulls

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
            records = validRecords.map { it.value() }
        )
    }
}
