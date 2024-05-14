package no.nav.syfo.frisktilarbeid.kafka

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.kafka.KafkaConsumerService
import no.nav.syfo.personstatus.db.createPersonOversiktStatus
import no.nav.syfo.personstatus.db.getPersonOversiktStatusList
import no.nav.syfo.personstatus.db.updatePersonOversiktStatusFriskmeldtTilArbeid
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.time.Duration

class KafkaFriskTilArbeidService(
    private val database: DatabaseInterface,
) : KafkaConsumerService<VedtakStatusRecord> {

    override val pollDurationInMillis: Long = 1000

    override fun pollAndProcessRecords(kafkaConsumer: KafkaConsumer<String, VedtakStatusRecord>) {
        val records = kafkaConsumer.poll(Duration.ofMillis(pollDurationInMillis))
        if (records.count() > 0) {
            processRecords(records)
            kafkaConsumer.commitSync()
        }
    }

    private fun processRecords(
        consumerRecords: ConsumerRecords<String, VedtakStatusRecord>,
    ) {
        val (tombstoneRecords, validRecords) = consumerRecords.partition { it.value() == null }

        if (tombstoneRecords.isNotEmpty()) {
            val numberOfTombstones = tombstoneRecords.size
            COUNT_KAFKA_CONSUMER_FRISKTILARBEID_TOMBSTONE.increment(numberOfTombstones.toDouble())
            log.error("Value of $numberOfTombstones ConsumerRecord are null, most probably due to a tombstone. Contact the owner of the topic if an error is suspected")
        }

        database.connection.use { connection ->
            validRecords.forEach { record ->
                log.info("Received ${VedtakStatusRecord::class.java.simpleName} with key=${record.key()}, ready to process.")
                val vedtak = record.value()
                receiveKafkaFriskTilArbeidVedtak(
                    connection = connection,
                    vedtakStatusRecord = vedtak,
                )
                COUNT_KAFKA_CONSUMER_FRISKTILARBEID_READ.increment()
            }
            connection.commit()
        }
    }

    private fun receiveKafkaFriskTilArbeidVedtak(
        connection: Connection,
        vedtakStatusRecord: VedtakStatusRecord,
    ) {
        val existingPersonOversiktStatus = connection.getPersonOversiktStatusList(
            fnr = vedtakStatusRecord.personident,
        ).firstOrNull()

        if (existingPersonOversiktStatus == null) {
            val personOversiktStatus = vedtakStatusRecord.toPersonOversiktStatus()
            connection.createPersonOversiktStatus(
                commit = false,
                personOversiktStatus = personOversiktStatus,
            )
            COUNT_KAFKA_CONSUMER_FRISKTILARBEID_CREATED_PERSONOVERSIKT_STATUS.increment()
        } else {
            connection.updatePersonOversiktStatusFriskmeldtTilArbeid(
                pPersonOversiktStatus = existingPersonOversiktStatus,
                friskTilArbeidFom = if (vedtakStatusRecord.status == Status.FATTET)
                    vedtakStatusRecord.fom
                else null
            )
            COUNT_KAFKA_CONSUMER_FRISKTILARBEID_UPDATED_PERSONOVERSIKT_STATUS.increment()
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(KafkaFriskTilArbeidService::class.java)
    }
}
