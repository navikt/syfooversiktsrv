package no.nav.syfo.identhendelse.kafka

import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import no.nav.syfo.personstatus.infrastructure.kafka.KafkaEnvironment
import no.nav.syfo.personstatus.infrastructure.kafka.kafkaAivenConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerConfig
import java.util.*

fun kafkaIdenthendelseConsumerConfig(
    kafkaEnvironment: KafkaEnvironment,
): Properties {
    return Properties().apply {
        putAll(kafkaAivenConsumerConfig(kafkaEnvironment))
        this[ConsumerConfig.GROUP_ID_CONFIG] = "syfooversiktsrv-v2"
        this[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = KafkaAvroDeserializer::class.java.canonicalName

        this[KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG] = kafkaEnvironment.aivenSchemaRegistryUrl
        this[KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG] = false
        this[KafkaAvroDeserializerConfig.USER_INFO_CONFIG] =
            "${kafkaEnvironment.aivenRegistryUser}:${kafkaEnvironment.aivenRegistryPassword}"
        this[KafkaAvroDeserializerConfig.BASIC_AUTH_CREDENTIALS_SOURCE] = "USER_INFO"
    }
}
