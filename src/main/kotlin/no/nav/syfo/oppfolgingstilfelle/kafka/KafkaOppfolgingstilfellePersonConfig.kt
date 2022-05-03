package no.nav.syfo.oppfolgingstilfelle.kafka

import no.nav.syfo.application.kafka.*
import org.apache.kafka.clients.consumer.ConsumerConfig
import java.util.*

fun kafkaOppfolgingstilfellePersonConsumerConfig(
    kafkaEnvironment: KafkaEnvironment,
): Properties {
    return Properties().apply {
        putAll(kafkaConsumerConfig(kafkaEnvironment))
        this[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] =
            KafkaOppfolgingstilfellePersonDeserializer::class.java.canonicalName
    }
}
