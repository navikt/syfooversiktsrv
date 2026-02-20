package no.nav.syfo.infrastructure.kafka.frisktilarbeid

import no.nav.syfo.application.IPersonOversiktStatusRepository
import no.nav.syfo.application.ITransactionManager
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.infrastructure.database.queries.createPersonOversiktStatus
import no.nav.syfo.infrastructure.database.queries.updatePersonOversiktStatusFriskmeldtTilArbeid
import no.nav.syfo.infrastructure.kafka.KafkaConsumerService
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.time.Duration

class FriskTilArbeidVedtakConsumer(
    private val transactionManager: ITransactionManager,
    private val personOversiktStatusRepository: IPersonOversiktStatusRepository,
) : KafkaConsumerService<VedtakStatusRecord> {

    override val pollDurationInMillis: Long = 1000

    override suspend fun pollAndProcessRecords(kafkaConsumer: KafkaConsumer<String, VedtakStatusRecord>) {
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

        transactionManager.transaction { connection ->
            validRecords.forEach { record ->
                log.info("Received ${VedtakStatusRecord::class.java.simpleName} with key=${record.key()}, ready to process.")
                val vedtak = record.value()
                receiveKafkaFriskTilArbeidVedtak(
                    connection = connection,
                    vedtakStatusRecord = vedtak,
                )
                COUNT_KAFKA_CONSUMER_FRISKTILARBEID_READ.increment()
            }
        }
    }

    private fun receiveKafkaFriskTilArbeidVedtak(
        connection: Connection,
        vedtakStatusRecord: VedtakStatusRecord,
    ) {
        val existingPersonOversiktStatus =
            personOversiktStatusRepository.getPersonOversiktStatus(
                personident = PersonIdent(vedtakStatusRecord.personident),
                connection = connection,
            )

        if (existingPersonOversiktStatus == null) {
            val personOversiktStatus = vedtakStatusRecord.toPersonOversiktStatus()
            connection.createPersonOversiktStatus(
                commit = false,
                personOversiktStatus = personOversiktStatus,
            )
            COUNT_KAFKA_CONSUMER_FRISKTILARBEID_CREATED_PERSONOVERSIKT_STATUS.increment()
        } else {
            connection.updatePersonOversiktStatusFriskmeldtTilArbeid(
                personident = PersonIdent(existingPersonOversiktStatus.fnr),
                friskTilArbeidFom = if (vedtakStatusRecord.status == Status.FATTET) vedtakStatusRecord.fom else null
            )
            COUNT_KAFKA_CONSUMER_FRISKTILARBEID_UPDATED_PERSONOVERSIKT_STATUS.increment()
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(FriskTilArbeidVedtakConsumer::class.java)
    }
}
