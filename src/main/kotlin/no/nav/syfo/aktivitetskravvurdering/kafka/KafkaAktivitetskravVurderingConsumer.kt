package no.nav.syfo.aktivitetskravvurdering.kafka

import no.nav.syfo.aktivitetskravvurdering.persistAktivitetskrav
import no.nav.syfo.personstatus.infrastructure.database.DatabaseInterface
import no.nav.syfo.personstatus.infrastructure.kafka.KafkaConsumerService
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory
import java.time.Duration

class KafkaAktivitetskravVurderingConsumer(
    private val database: DatabaseInterface,
) : KafkaConsumerService<KafkaAktivitetskravVurdering> {

    override val pollDurationInMillis: Long = 1000

    override fun pollAndProcessRecords(kafkaConsumer: KafkaConsumer<String, KafkaAktivitetskravVurdering>) {
        val records = kafkaConsumer.poll(Duration.ofMillis(pollDurationInMillis))
        if (records.count() > 0) {
            processRecords(records)
            kafkaConsumer.commitSync()
        }
    }

    private fun processRecords(
        consumerRecords: ConsumerRecords<String, KafkaAktivitetskravVurdering>,
    ) {
        val (tombstoneRecords, validRecords) = consumerRecords.partition { it.value() == null }

        if (tombstoneRecords.isNotEmpty()) {
            val numberOfTombstones = tombstoneRecords.size
            log.error("Value of $numberOfTombstones ConsumerRecord are null, most probably due to a tombstone. Contact the owner of the topic if an error is suspected")
        }

        database.connection.use { connection ->
            validRecords.forEach { record ->
                log.info("Received ${KafkaAktivitetskravVurdering::class.java.simpleName} with key=${record.key()}, ready to process.")
                val aktivitetskrav = record.value().toAktivitetskrav()
                persistAktivitetskrav(
                    connection = connection,
                    aktivitetskrav = aktivitetskrav
                )
            }
            connection.commit()
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(KafkaAktivitetskravVurdering::class.java)
    }
}
