package no.nav.syfo.oppfolgingstilfelle.kafka

import no.nav.syfo.personstatus.infrastructure.database.DatabaseInterface
import no.nav.syfo.personstatus.infrastructure.kafka.KafkaConsumerService
import no.nav.syfo.oppfolgingstilfelle.domain.Oppfolgingstilfelle
import no.nav.syfo.personstatus.db.createPersonOversiktStatus
import no.nav.syfo.personstatus.db.getPersonOversiktStatusList
import no.nav.syfo.personstatus.db.updatePersonOversiktStatusOppfolgingstilfelle
import no.nav.syfo.personstatus.domain.PPersonOversiktStatus
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.SQLException
import java.time.Duration
import java.time.temporal.ChronoUnit

class KafkaOppfolgingstilfellePersonService(
    val database: DatabaseInterface,
) : KafkaConsumerService<KafkaOppfolgingstilfellePerson> {

    override val pollDurationInMillis: Long = 1000

    override fun pollAndProcessRecords(kafkaConsumer: KafkaConsumer<String, KafkaOppfolgingstilfellePerson>) {
        val records = kafkaConsumer.poll(Duration.ofMillis(pollDurationInMillis))
        if (records.count() > 0) {
            processRecords(records)
            kafkaConsumer.commitSync()
        }
    }

    private fun processRecords(
        consumerRecords: ConsumerRecords<String, KafkaOppfolgingstilfellePerson>,
    ) {
        val (tombstoneRecordList, recordsValid) = consumerRecords.partition {
            it.value() == null
        }

        processTombstoneRecordList(
            tombstoneRecordList = tombstoneRecordList,
        )

        val recordsRelevant = recordsValid
            .sortedWith(
                compareByDescending<ConsumerRecord<String, KafkaOppfolgingstilfellePerson>> { record ->
                    record.value().referanseTilfelleBitInntruffet
                }.thenByDescending { it.value().createdAt }
            ).groupBy { record ->
                record.value().personIdentNumber
            }.map { record ->
                record.value.firstOrNull()
            }.filterNotNull()

        processRelevantRecordList(
            recordsRelevant = recordsRelevant,
        )
    }

    private fun processTombstoneRecordList(
        tombstoneRecordList: List<ConsumerRecord<String, KafkaOppfolgingstilfellePerson>>,
    ) {
        if (tombstoneRecordList.isNotEmpty()) {
            val numberOfTombstones = tombstoneRecordList.size
            log.error("Value of $numberOfTombstones ConsumerRecord are null, most probably due to a tombstone. Contact the owner of the topic if an error is suspected")
            COUNT_KAFKA_CONSUMER_OPPFOLGINGSTILFELLE_PERSON_TOMBSTONE.increment(numberOfTombstones.toDouble())
        }
    }

    private fun processRelevantRecordList(
        recordsRelevant: List<ConsumerRecord<String, KafkaOppfolgingstilfellePerson>>,
    ) {
        database.connection.use { connection ->
            recordsRelevant.forEach { consumerRecord ->
                COUNT_KAFKA_CONSUMER_OPPFOLGINGSTILFELLE_PERSON_READ.increment()
                receiveKafkaOppfolgingstilfellePerson(
                    connection = connection,
                    kafkaOppfolgingstilfellePerson = consumerRecord.value(),
                )
            }
            connection.commit()
        }
    }

    private fun receiveKafkaOppfolgingstilfellePerson(
        connection: Connection,
        kafkaOppfolgingstilfellePerson: KafkaOppfolgingstilfellePerson,
    ) {
        val latestTilfelle = kafkaOppfolgingstilfellePerson.oppfolgingstilfelleList.maxByOrNull {
            it.start
        }
        if (latestTilfelle == null) {
            log.warn("SKipped processing of record: No latest Oppfolgingstilfelle found in record.")
            COUNT_KAFKA_CONSUMER_OPPFOLGINGSTILFELLE_PERSON_SKIPPED_NO_TILFELLE.increment()
            return
        }
        if (latestTilfelle.virksomhetsnummerList.isEmpty()) {
            COUNT_KAFKA_CONSUMER_OPPFOLGINGSTILFELLE_PERSON_NOT_ARBEIDSTAKER.increment()
        }

        val maybePPersonOversiktStatus: PPersonOversiktStatus? = connection.getPersonOversiktStatusList(
            fnr = kafkaOppfolgingstilfellePerson.personIdentNumber,
        ).firstOrNull()
        if (maybePPersonOversiktStatus == null) {
            val personOversiktStatus = kafkaOppfolgingstilfellePerson.toPersonOversiktStatus(
                latestKafkaOppfolgingstilfelle = latestTilfelle,
            )
            connection.createPersonOversiktStatus(
                commit = false,
                personOversiktStatus = personOversiktStatus,
            )
        } else {
            val latestPersonOppfolgingstilfelle = kafkaOppfolgingstilfellePerson.toPersonOppfolgingstilfelle(
                latestKafkaOppfolgingstilfelle = latestTilfelle,
            )
            val shouldUpdatePersonOppfolgingstilfelle = shouldUpdatePersonOppfolgingstilfelle(
                newOppfolgingstilfelle = latestPersonOppfolgingstilfelle,
                exisitingPPersonOversiktStatus = maybePPersonOversiktStatus
            )
            if (shouldUpdatePersonOppfolgingstilfelle) {
                try {
                    connection.updatePersonOversiktStatusOppfolgingstilfelle(
                        pPersonOversiktStatus = maybePPersonOversiktStatus,
                        oppfolgingstilfelle = latestPersonOppfolgingstilfelle,
                    )
                } catch (sqlException: SQLException) {
                    // retry once before giving up (could be database concurrency conflict)
                    log.info("Got sqlException when receiveKafkaOppfolgingstilfellePerson, try again")
                    connection.updatePersonOversiktStatusOppfolgingstilfelle(
                        pPersonOversiktStatus = maybePPersonOversiktStatus,
                        oppfolgingstilfelle = latestPersonOppfolgingstilfelle,
                    )
                }
            }
        }
    }

    private fun shouldUpdatePersonOppfolgingstilfelle(
        newOppfolgingstilfelle: Oppfolgingstilfelle,
        exisitingPPersonOversiktStatus: PPersonOversiktStatus,
    ): Boolean {
        return if (newOppfolgingstilfelle.oppfolgingstilfelleBitReferanseUuid == exisitingPPersonOversiktStatus.oppfolgingstilfelleBitReferanseUuid) {
            exisitingPPersonOversiktStatus.oppfolgingstilfelleGeneratedAt?.let {
                newOppfolgingstilfelle.generatedAt.isAfter(it)
            } ?: true
        } else {
            exisitingPPersonOversiktStatus.oppfolgingstilfelleBitReferanseInntruffet?.let {
                val existingInntruffet = it.truncatedTo(ChronoUnit.MILLIS)
                val newInntruffet =
                    newOppfolgingstilfelle.oppfolgingstilfelleBitReferanseInntruffet.truncatedTo(ChronoUnit.MILLIS)
                if (newInntruffet == existingInntruffet) {
                    exisitingPPersonOversiktStatus.oppfolgingstilfelleGeneratedAt?.let {
                        newOppfolgingstilfelle.generatedAt.isAfter(it)
                    } ?: true
                } else {
                    newInntruffet.isAfter(existingInntruffet)
                }
            } ?: true
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(KafkaOppfolgingstilfellePersonService::class.java)
    }
}
