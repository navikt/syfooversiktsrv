package no.nav.syfo.aktivitetskravvurdering.kafka

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.kafka.KafkaConsumerService
import no.nav.syfo.personstatus.db.getPersonOversiktStatusList
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.time.Duration

class KafkaAktivitetskravVurderingService(
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
                receiveKafkaAktivitetskravVurdering(
                    connection = connection,
                    kafkaAktivitetskravVurdering = record.value()
                )
            }
            connection.commit()
        }
    }

    private fun receiveKafkaAktivitetskravVurdering(
        connection: Connection,
        kafkaAktivitetskravVurdering: KafkaAktivitetskravVurdering,
    ) {
        val existingPersonOversiktStatus = connection.getPersonOversiktStatusList(
            fnr = kafkaAktivitetskravVurdering.personIdent,
        ).firstOrNull()

        if (existingPersonOversiktStatus == null) {
            // TODO: Create new oppgave
        } else {
            // TODO: Update oppgave
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(KafkaAktivitetskravVurdering::class.java)
    }
}
