package no.nav.syfo.dialogmotekandidat.kafka

import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import java.time.Duration

class KafkaDialogmotekandidatEndringService {
    fun pollAndProcessRecords(kafkaConsumer: KafkaConsumer<String, KafkaDialogmotekandidatEndring>) {
        val consumerRecords = kafkaConsumer.poll(Duration.ofMillis(1000))
        if (consumerRecords.count() > 0) {
            processRecords(records = consumerRecords)
            kafkaConsumer.commitSync()
        }
    }

    private fun processRecords(records: ConsumerRecords<String, KafkaDialogmotekandidatEndring>) {
        TODO("Not yet implemented")
    }
}
