package no.nav.syfo.trengeroppfolging.kafka

import no.nav.syfo.trengeroppfolging.TrengerOppfolgingService
import no.nav.syfo.personstatus.infrastructure.kafka.KafkaConsumerService
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import java.time.Duration

class TrengerOppfolgingConsumer(
    private val trengerOppfolgingService: TrengerOppfolgingService
) : KafkaConsumerService<KafkaHuskelapp> {

    override val pollDurationInMillis: Long = 1000

    override fun pollAndProcessRecords(kafkaConsumer: KafkaConsumer<String, KafkaHuskelapp>) {
        val records = kafkaConsumer.poll(Duration.ofMillis(pollDurationInMillis))
        if (records.count() > 0) {
            processRecords(records)
            kafkaConsumer.commitSync()
        }
    }

    private fun processRecords(
        consumerRecords: ConsumerRecords<String, KafkaHuskelapp>,
    ) {
        val validRecords = consumerRecords.requireNoNulls()
        trengerOppfolgingService.processTrengerOppfolging(
            records = validRecords.map { it.value().toTrengerOppfolging() }
        )
    }
}
