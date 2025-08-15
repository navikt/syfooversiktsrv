package no.nav.syfo.personstatus.infrastructure.kafka.aktivitetskrav

import no.nav.syfo.personstatus.application.PersonoversiktStatusService
import no.nav.syfo.personstatus.domain.PersonIdent
import no.nav.syfo.personstatus.infrastructure.kafka.KafkaConsumerService
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory
import java.time.Duration
import kotlin.collections.forEach
import kotlin.collections.isNotEmpty
import kotlin.collections.partition
import kotlin.jvm.java

class AktivitetskravVurderingConsumer(
    private val personoversiktStatusService: PersonoversiktStatusService,
) : KafkaConsumerService<AktivitetskravVurderingRecord> {

    override val pollDurationInMillis: Long = 1000

    override suspend fun pollAndProcessRecords(kafkaConsumer: KafkaConsumer<String, AktivitetskravVurderingRecord>) {
        val records = kafkaConsumer.poll(Duration.ofMillis(pollDurationInMillis))
        if (records.count() > 0) {
            processRecords(records)
            kafkaConsumer.commitSync()
        }
    }

    private fun processRecords(records: ConsumerRecords<String, AktivitetskravVurderingRecord>) {
        val (tombstoneRecords, validRecords) = records.partition { it.value() == null }
        if (tombstoneRecords.isNotEmpty()) {
            val numberOfTombstones = tombstoneRecords.size
            log.error("Value of $numberOfTombstones ConsumerRecord are null, most probably due to a tombstone. Contact the owner of the topic if an error is suspected")
        }
        validRecords.forEach { record ->
            log.info("Received ${AktivitetskravVurderingRecord::class.java.simpleName} with key=${record.key()}, ready to process.")
            val vurdering = record.value()
            personoversiktStatusService.upsertAktivitetskravvurderingStatus(
                personident = PersonIdent(vurdering.personIdent),
                isAktivVurdering = !vurdering.isFinal,
            )
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(AktivitetskravVurderingRecord::class.java)
    }
}

data class AktivitetskravVurderingRecord(
    val uuid: String,
    val personIdent: String,
    val isFinal: Boolean,
)
