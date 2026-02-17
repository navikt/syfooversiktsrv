package no.nav.syfo.personstatus.infrastructure.kafka.dialogmotekandidat

import no.nav.syfo.personstatus.application.IPersonOversiktStatusRepository
import no.nav.syfo.personstatus.application.ITransactionManager
import no.nav.syfo.personstatus.domain.PersonIdent
import no.nav.syfo.personstatus.infrastructure.database.queries.createPersonOversiktStatus
import no.nav.syfo.personstatus.infrastructure.database.queries.updatePersonOversiktStatusKandidat
import no.nav.syfo.personstatus.infrastructure.kafka.KafkaConsumerService
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.SQLException
import java.time.Duration

class DialogmotekandidatEndringConsumer(
    private val transactionManager: ITransactionManager,
    private val personoversiktStatusRepository: IPersonOversiktStatusRepository,
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

        transactionManager.transaction { connection ->
            validRecords.forEach { record ->
                COUNT_KAFKA_CONSUMER_DIALOGMOTEKANDIDAT_READ.increment()
                log.info("Received ${KafkaDialogmotekandidatEndring::class.java.simpleName} with key=${record.key()}, ready to process.")
                receiveKafkaDialogmotekandidatEndring(
                    connection = connection,
                    kafkaDialogmotekandidatEndring = record.value()
                )
            }
        }
    }

    private fun receiveKafkaDialogmotekandidatEndring(
        connection: Connection,
        kafkaDialogmotekandidatEndring: KafkaDialogmotekandidatEndring,
    ) {
        val existingPersonOversiktStatus =
            personoversiktStatusRepository.getPersonOversiktStatus(
                personident = PersonIdent(kafkaDialogmotekandidatEndring.personIdentNumber),
                connection = connection,
            )

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
                        personident = PersonIdent(existingPersonOversiktStatus.fnr),
                        kandidat = kafkaDialogmotekandidatEndring.kandidat,
                        generatedAt = kafkaDialogmotekandidatEndring.createdAt,
                    )
                } catch (sqlException: SQLException) {
                    // retry once before giving up (could be database concurrency conflict)
                    log.info("Got sqlException when receiveKafkaDialogmotekandidatEndring, try again")
                    connection.updatePersonOversiktStatusKandidat(
                        personident = PersonIdent(existingPersonOversiktStatus.fnr),
                        kandidat = kafkaDialogmotekandidatEndring.kandidat,
                        generatedAt = kafkaDialogmotekandidatEndring.createdAt,
                    )
                }
                COUNT_KAFKA_CONSUMER_DIALOGMOTEKANDIDAT_UPDATED_PERSONOVERSIKT_STATUS.increment()
            }
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(DialogmotekandidatEndringConsumer::class.java)
    }
}
