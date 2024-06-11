package no.nav.syfo.pdlpersonhendelse.kafka

import no.nav.person.pdl.leesah.Personhendelse
import no.nav.syfo.personstatus.infrastructure.kafka.KafkaConsumerService
import no.nav.syfo.metric.COUNT_KAFKA_CONSUMER_PDL_PERSONHENDELSE_TOMBSTONE
import no.nav.syfo.pdlpersonhendelse.PdlPersonhendelseService
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory
import java.time.Duration

class KafkaPersonhendelseConsumerService(
    private val pdlPersonhendelseService: PdlPersonhendelseService,
) : KafkaConsumerService<Personhendelse> {
    override val pollDurationInMillis: Long = 1000

    override fun pollAndProcessRecords(kafkaConsumer: KafkaConsumer<String, Personhendelse>) {
        val records = kafkaConsumer.poll(Duration.ofMillis(pollDurationInMillis))
        if (records.count() > 0) {
            processRecords(records)
            kafkaConsumer.commitSync()
        }
    }

    private fun processRecords(records: ConsumerRecords<String, Personhendelse>) {
        val (tombstoneRecords, validRecords) = records.partition { it.value() == null }

        if (tombstoneRecords.isNotEmpty()) {
            val numberOfTombstones = tombstoneRecords.size
            log.error("Value of $numberOfTombstones ConsumerRecord are null, most probably due to a tombstone. Contact the owner of the topic if an error is suspected")
            COUNT_KAFKA_CONSUMER_PDL_PERSONHENDELSE_TOMBSTONE.increment()
        }
        validRecords.forEach { record ->
            pdlPersonhendelseService.handlePersonhendelse(record.value())
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(KafkaPersonhendelseConsumerService::class.java)
    }
}
