package no.nav.syfo.dialogmotekandidat.kafka

import no.nav.syfo.application.kafka.*
import org.apache.kafka.clients.consumer.ConsumerConfig
import java.util.Properties

fun kafkaDialogmotekandidatEndringConsumerConfig(
    kafkaEnvironment: KafkaEnvironment,
): Properties {
    return Properties().apply {
        putAll(kafkaConsumerConfig(kafkaEnvironment))
        this[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = KafkaDialogmotekandidatEndringDeserializer::class.java
    }
}
