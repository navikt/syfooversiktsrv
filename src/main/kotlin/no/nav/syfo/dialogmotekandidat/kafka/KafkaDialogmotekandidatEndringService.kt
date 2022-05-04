package no.nav.syfo.dialogmotekandidat.kafka

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.personstatus.*
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.time.Duration

class KafkaDialogmotekandidatEndringService(
    private val database: DatabaseInterface,
) {
    fun pollAndProcessRecords(kafkaConsumer: KafkaConsumer<String, KafkaDialogmotekandidatEndring>) {
        val consumerRecords = kafkaConsumer.poll(Duration.ofSeconds(POLL_DURATION_SECONDS))
        if (consumerRecords.count() > 0) {
            processRecords(consumerRecords = consumerRecords)
            kafkaConsumer.commitSync()
        }
    }

    private fun processRecords(consumerRecords: ConsumerRecords<String, KafkaDialogmotekandidatEndring>) {
        val (tombstoneRecords, validRecords) = consumerRecords.partition { it.value() == null }

        if (tombstoneRecords.isNotEmpty()) {
            log.error("Value of ${tombstoneRecords.size} ConsumerRecord are null, most probably due to a tombstone. Contact the owner of the topic if an error is suspected")
            COUNT_KAFKA_CONSUMER_DIALOGMOTEKANDIDAT_TOMBSTONE.increment()
        }

        database.connection.use { connection ->
            validRecords.forEach { record ->
                COUNT_KAFKA_CONSUMER_DIALOGMOTEKANDIDAT_READ.increment()
                log.info("Received ${KafkaDialogmotekandidatEndring::class.java.simpleName} with key=${record.key()}, ready to process.")
                receiveKafkaDialogmotekandidatEndring(
                    connection = connection,
                    kafkaDialogmotekandidatEndring = record.value()
                )
            }
            connection.commit()
        }
    }

    private fun receiveKafkaDialogmotekandidatEndring(
        connection: Connection,
        kafkaDialogmotekandidatEndring: KafkaDialogmotekandidatEndring,
    ) {
        val existingPersonOversiktStatus = connection.getPersonOversiktStatusList(
            fnr = kafkaDialogmotekandidatEndring.personIdentNumber,
        ).firstOrNull()

        if (existingPersonOversiktStatus == null) {
            val personOversiktStatus = kafkaDialogmotekandidatEndring.toPersonOversiktStatus()
            connection.createPersonOversiktStatus(
                commit = false,
                personOversiktStatus = personOversiktStatus,
            )
        } else {
            val shouldUpdateKandidat = existingPersonOversiktStatus.dialogmotekandidatGeneratedAt?.let {
                kafkaDialogmotekandidatEndring.createdAt.isAfter(it)
            } ?: true
            if (shouldUpdateKandidat) {
                connection.updatePersonOversiktStatusKandidat(
                    pPersonOversiktStatus = existingPersonOversiktStatus,
                    kandidat = kafkaDialogmotekandidatEndring.kandidat,
                    generatedAt = kafkaDialogmotekandidatEndring.createdAt,
                )
            }
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(KafkaDialogmotekandidatEndringService::class.java)
        private const val POLL_DURATION_SECONDS = 1L
    }
}
