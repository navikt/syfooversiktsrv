package no.nav.syfo.oppfolgingstilfelle.kafka

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.personstatus.*
import no.nav.syfo.personstatus.domain.PPersonOversiktStatus
import no.nav.syfo.personstatus.domain.PersonOppfolgingstilfelle
import org.apache.kafka.clients.consumer.*
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.time.Duration

class KafkaOppfolgingstilfellePersonService(
    val database: DatabaseInterface,
) {
    fun pollAndProcessRecords(
        kafkaConsumerOppfolgingstilfellePerson: KafkaConsumer<String, KafkaOppfolgingstilfellePerson>,
    ) {
        val records = kafkaConsumerOppfolgingstilfellePerson.poll(Duration.ofMillis(1000))
        if (records.count() > 0) {
            processRecords(
                consumerRecords = records,
            )
            kafkaConsumerOppfolgingstilfellePerson.commitSync()
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
                compareBy({ it.value().referanseTilfelleBitInntruffet }, { it.value().createdAt })
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
            log.error("Value of ${tombstoneRecordList.size} ConsumerRecord are null, most probably due to a tombstone. Contact the owner of the topic if an error is suspected")
            COUNT_KAFKA_CONSUMER_OPPFOLGINGSTILFELLE_PERSON_TOMBSTONE.increment()
        }
    }

    private fun processRelevantRecordList(
        recordsRelevant: List<ConsumerRecord<String, KafkaOppfolgingstilfellePerson>>,
    ) {
        database.connection.use { connection ->
            recordsRelevant.forEach { consumerRecord ->
                COUNT_KAFKA_CONSUMER_OPPFOLGINGSTILFELLE_PERSON_READ.increment()
                log.info("Received ${KafkaOppfolgingstilfellePerson::class.java.simpleName}, ready to process. id=${consumerRecord.key()}, timestamp=${consumerRecord.timestamp()}")

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
            COUNT_KAFKA_CONSUMER_OPPFOLGINGSTILFELLE_PERSON_SKIPPED_NOT_ARBEIDSTAKER.increment()
            return
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
                newPersonOppfolgingstilfelle = latestPersonOppfolgingstilfelle,
                exisitingPPersonOversiktStatus = maybePPersonOversiktStatus
            )
            if (shouldUpdatePersonOppfolgingstilfelle) {
                connection.updatePersonOversiktStatusOppfolgingstilfelle(
                    pPersonOversiktStatus = maybePPersonOversiktStatus,
                    personOppfolgingstilfelle = latestPersonOppfolgingstilfelle,
                )
            }
        }
    }

    fun shouldUpdatePersonOppfolgingstilfelle(
        newPersonOppfolgingstilfelle: PersonOppfolgingstilfelle,
        exisitingPPersonOversiktStatus: PPersonOversiktStatus
    ): Boolean {
        return if (newPersonOppfolgingstilfelle.oppfolgingstilfelleBitReferanseUuid == exisitingPPersonOversiktStatus.oppfolgingstilfelleBitReferanseUuid) {
            exisitingPPersonOversiktStatus.oppfolgingstilfelleGeneratedAt?.let {
                newPersonOppfolgingstilfelle.oppfolgingstilfelleGeneratedAt.isAfter(it)
            } ?: true
        } else {
            exisitingPPersonOversiktStatus.oppfolgingstilfelleBitReferanseInntruffet?.let {
                newPersonOppfolgingstilfelle.oppfolgingstilfelleBitReferanseInntruffet.isAfter(it)
            } ?: true
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(KafkaOppfolgingstilfellePersonService::class.java)
    }
}
