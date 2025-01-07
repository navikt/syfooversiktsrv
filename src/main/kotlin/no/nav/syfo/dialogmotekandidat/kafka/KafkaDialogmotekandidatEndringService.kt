package no.nav.syfo.dialogmotekandidat.kafka

import no.nav.syfo.personstatus.infrastructure.database.DatabaseInterface
import no.nav.syfo.personstatus.infrastructure.database.queries.createPersonOversiktStatus
import no.nav.syfo.personstatus.infrastructure.kafka.KafkaConsumerService
import no.nav.syfo.personstatus.infrastructure.database.queries.getPersonOversiktStatusList
import no.nav.syfo.personstatus.infrastructure.database.queries.updatePersonOversiktStatusKandidat
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.SQLException
import java.time.Duration

class KafkaDialogmotekandidatEndringService(
    private val database: DatabaseInterface,
) : KafkaConsumerService<KafkaDialogmotekandidatEndring> {

    override val pollDurationInMillis: Long = 1000

    override suspend fun pollAndProcessRecords(kafkaConsumer: KafkaConsumer<String, KafkaDialogmotekandidatEndring>) {
        val records = kafkaConsumer.poll(Duration.ofMillis(pollDurationInMillis))
        if (records.count() > 0) {
            processRecords(records)
            kafkaConsumer.commitSync()
        }
    }

    private fun processRecords(
        consumerRecords: ConsumerRecords<String, KafkaDialogmotekandidatEndring>,
    ) {
        val (tombstoneRecords, validRecords) = consumerRecords.partition { it.value() == null }

        if (tombstoneRecords.isNotEmpty()) {
            val numberOfTombstones = tombstoneRecords.size
            log.error("Value of $numberOfTombstones ConsumerRecord are null, most probably due to a tombstone. Contact the owner of the topic if an error is suspected")
            COUNT_KAFKA_CONSUMER_DIALOGMOTEKANDIDAT_TOMBSTONE.increment(numberOfTombstones.toDouble())
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
            COUNT_KAFKA_CONSUMER_DIALOGMOTEKANDIDAT_CREATED_PERSONOVERSIKT_STATUS.increment()
        } else {
            val shouldUpdateKandidat = existingPersonOversiktStatus.dialogmotekandidatGeneratedAt?.let {
                kafkaDialogmotekandidatEndring.createdAt.isAfter(it)
            } ?: true
            if (shouldUpdateKandidat) {
                try {
                    connection.updatePersonOversiktStatusKandidat(
                        pPersonOversiktStatus = existingPersonOversiktStatus,
                        kandidat = kafkaDialogmotekandidatEndring.kandidat,
                        generatedAt = kafkaDialogmotekandidatEndring.createdAt,
                    )
                } catch (sqlException: SQLException) {
                    // retry once before giving up (could be database concurrency conflict)
                    log.info("Got sqlException when receiveKafkaDialogmotekandidatEndring, try again")
                    connection.updatePersonOversiktStatusKandidat(
                        pPersonOversiktStatus = existingPersonOversiktStatus,
                        kandidat = kafkaDialogmotekandidatEndring.kandidat,
                        generatedAt = kafkaDialogmotekandidatEndring.createdAt,
                    )
                }
                COUNT_KAFKA_CONSUMER_DIALOGMOTEKANDIDAT_UPDATED_PERSONOVERSIKT_STATUS.increment()
            }
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(KafkaDialogmotekandidatEndringService::class.java)
    }
}
