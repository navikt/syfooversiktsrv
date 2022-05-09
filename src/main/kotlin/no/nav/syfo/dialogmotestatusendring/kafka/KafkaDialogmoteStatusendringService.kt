package no.nav.syfo.dialogmotestatusendring.kafka

import no.nav.syfo.dialogmote.avro.KDialogmoteStatusEndring
import no.nav.syfo.kafka.KafkaConsumerService
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory
import java.time.Duration

class KafkaDialogmoteStatusendringService() : KafkaConsumerService<KDialogmoteStatusEndring> {
    override val pollDurationInMillis: Long = 1000

    override fun pollAndProcessRecords(kafkaConsumer: KafkaConsumer<String, KDialogmoteStatusEndring>) {
        val records = kafkaConsumer.poll(Duration.ofMillis(pollDurationInMillis))
        if (records.count() > 0) {
            processRecords(records)
            kafkaConsumer.commitSync()
        }
    }

    private fun processRecords(records: ConsumerRecords<String, KDialogmoteStatusEndring>) {
        records.forEach { consumerRecord ->
            log.info("Received ${KDialogmoteStatusEndring::class.java.simpleName} record with key: ${consumerRecord.key()}")
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(KafkaDialogmoteStatusendringService::class.java)
    }
}
