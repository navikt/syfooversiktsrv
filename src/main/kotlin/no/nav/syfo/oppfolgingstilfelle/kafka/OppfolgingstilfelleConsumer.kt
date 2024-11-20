package no.nav.syfo.oppfolgingstilfelle.kafka

import no.nav.syfo.personstatus.application.OppfolgingstilfelleService
import no.nav.syfo.personstatus.infrastructure.kafka.KafkaConsumerService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory
import java.time.Duration

class OppfolgingstilfelleConsumer(
    val oppfolgingstilfelleService: OppfolgingstilfelleService,
) : KafkaConsumerService<OppfolgingstilfellePersonRecord> {

    override val pollDurationInMillis: Long = 1000

    override fun pollAndProcessRecords(kafkaConsumer: KafkaConsumer<String, OppfolgingstilfellePersonRecord>) {
        val records = kafkaConsumer.poll(Duration.ofMillis(pollDurationInMillis))
        if (records.count() > 0) {
            processRecords(records)
            kafkaConsumer.commitSync()
        }
    }

    private fun processRecords(records: ConsumerRecords<String, OppfolgingstilfellePersonRecord>) {
        val (tombstoneRecordList, recordsValid) = records.partition { it.value() == null }
        processTombstoneRecordList(tombstoneRecordList)

        val recordsRelevant = recordsValid
            .sortedWith(
                compareByDescending<ConsumerRecord<String, OppfolgingstilfellePersonRecord>> { record ->
                    record.value().referanseTilfelleBitInntruffet
                }.thenByDescending { it.value().createdAt }
            )
            .groupBy { it.value().personIdentNumber }
            .map { it.value.firstOrNull() }
            .filterNotNull()

        recordsRelevant.forEach { record ->
            COUNT_KAFKA_CONSUMER_OPPFOLGINGSTILFELLE_PERSON_READ.increment()
            receiveOppfolgingstilfellePerson(record.value())
        }
    }

    private fun processTombstoneRecordList(tombstoneRecordList: List<ConsumerRecord<String, OppfolgingstilfellePersonRecord>>) {
        if (tombstoneRecordList.isNotEmpty()) {
            val numberOfTombstones = tombstoneRecordList.size
            log.error("Value of $numberOfTombstones ConsumerRecord are null, most probably due to a tombstone. Contact the owner of the topic if an error is suspected")
            COUNT_KAFKA_CONSUMER_OPPFOLGINGSTILFELLE_PERSON_TOMBSTONE.increment(numberOfTombstones.toDouble())
        }
    }

    private fun receiveOppfolgingstilfellePerson(record: OppfolgingstilfellePersonRecord) {
        val latestTilfelle = record.oppfolgingstilfelleList.maxByOrNull { it.start }
        if (latestTilfelle == null) {
            log.warn("SKipped processing of record: No latest Oppfolgingstilfelle found in record.")
            COUNT_KAFKA_CONSUMER_OPPFOLGINGSTILFELLE_PERSON_SKIPPED_NO_TILFELLE.increment()
            return
        }
        if (latestTilfelle.virksomhetsnummerList.isEmpty()) COUNT_KAFKA_CONSUMER_OPPFOLGINGSTILFELLE_PERSON_NOT_ARBEIDSTAKER.increment()

        val personOversiktStatus = record.toPersonOversiktStatus(latestKafkaOppfolgingstilfelle = latestTilfelle)
        val latestPersonOppfolgingstilfelle = record.toPersonOppfolgingstilfelle(latestKafkaOppfolgingstilfelle = latestTilfelle)
        oppfolgingstilfelleService.upsertPersonOversiktStatus(personOversiktStatus, latestPersonOppfolgingstilfelle)
    }

    companion object {
        private val log = LoggerFactory.getLogger(OppfolgingstilfelleConsumer::class.java)
    }
}
