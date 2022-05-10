package no.nav.syfo.dialogmotestatusendring.kafka

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.dialogmote.avro.KDialogmoteStatusEndring
import no.nav.syfo.dialogmotestatusendring.domain.DialogmoteStatusendring
import no.nav.syfo.kafka.KafkaConsumerService
import no.nav.syfo.personstatus.*
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.time.Duration

class KafkaDialogmoteStatusendringService(
    private val database: DatabaseInterface,
) : KafkaConsumerService<KDialogmoteStatusEndring> {
    override val pollDurationInMillis: Long = 1000

    override fun pollAndProcessRecords(kafkaConsumer: KafkaConsumer<String, KDialogmoteStatusEndring>) {
        val records = kafkaConsumer.poll(Duration.ofMillis(pollDurationInMillis))
        if (records.count() > 0) {
            processRecords(records)
            kafkaConsumer.commitSync()
        }
    }

    private fun processRecords(records: ConsumerRecords<String, KDialogmoteStatusEndring>) {
        val (tombstoneRecords, validRecords) = records.partition { it.value() == null }

        if (tombstoneRecords.isNotEmpty()) {
            val numberOfTombstones = tombstoneRecords.size
            log.error("Value of $numberOfTombstones ConsumerRecord are null, most probably due to a tombstone. Contact the owner of the topic if an error is suspected")
            COUNT_KAFKA_CONSUMER_DIALOGMOTE_STATUSENDRING_TOMBSTONE.increment(numberOfTombstones.toDouble())
        }

        database.connection.use { connection ->
            validRecords.forEach { record ->
                COUNT_KAFKA_CONSUMER_DIALOGMOTE_STATUSENDRING_READ.increment()
                log.info("Received ${KDialogmoteStatusEndring::class.java.simpleName} record with key: ${record.key()}")
                receiveKafkaDialogmoteStatusEndring(
                    connection = connection,
                    kafkaDialogmoteStatusEndring = record.value(),
                )
            }
            connection.commit()
        }
    }

    private fun receiveKafkaDialogmoteStatusEndring(
        connection: Connection,
        kafkaDialogmoteStatusEndring: KDialogmoteStatusEndring,
    ) {
        val dialogmoteStatusEndring = DialogmoteStatusendring.create(kafkaDialogmoteStatusEndring)
        val existingPersonOversiktStatus = connection.getPersonOversiktStatusList(
            fnr = kafkaDialogmoteStatusEndring.getPersonIdent(),
        ).firstOrNull()

        if (existingPersonOversiktStatus == null) {
            val personOversiktStatus = dialogmoteStatusEndring.toPersonOversiktStatus()
            connection.createPersonOversiktStatus(
                commit = false,
                personOversiktStatus = personOversiktStatus,
            )
            COUNT_KAFKA_CONSUMER_DIALOGMOTE_STATUSENDRING_CREATED_PERSONOVERSIKT_STATUS.increment()
        } else {
            val shouldUpdateMotestatus = existingPersonOversiktStatus.motestatusGeneratedAt?.let {
                dialogmoteStatusEndring.endringTidspunkt.isAfter(it)
            } ?: true
            if (shouldUpdateMotestatus) {
                connection.updatePersonOversiktStatusMotestatus(
                    pPersonOversiktStatus = existingPersonOversiktStatus,
                    dialogmoteStatusendring = dialogmoteStatusEndring,
                )
                COUNT_KAFKA_CONSUMER_DIALOGMOTE_STATUSENDRING_UPDATED_PERSONOVERSIKT_STATUS.increment()
            }
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(KafkaDialogmoteStatusendringService::class.java)
    }
}
