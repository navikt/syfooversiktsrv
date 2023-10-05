package no.nav.syfo.huskelapp.kafka

import no.nav.syfo.huskelapp.HuskelappService
import no.nav.syfo.kafka.KafkaConsumerService
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory
import java.time.Duration

class HuskelappConsumer(
    private val huskelappService: HuskelappService
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
        val (tombstones, validRecords) = consumerRecords.partition { it.value() == null }

        if (tombstones.isNotEmpty()) {
            val numberOfTombstones = tombstones.size
            log.error("Value of $numberOfTombstones ConsumerRecord are null, most probably due to a tombstone. Contact the owner of the topic if an error is suspected")
            COUNT_KAFKA_CONSUMER_HUSKELAPP_TOMBSTONE.increment(numberOfTombstones.toDouble())
        }

        huskelappService.processHuskelapp(
            records = validRecords.map { it.value().toHuskelapp() }
        )
    }

    companion object {
        private val log = LoggerFactory.getLogger(HuskelappConsumer::class.java)
    }
}
