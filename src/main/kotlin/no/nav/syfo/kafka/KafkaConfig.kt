package no.nav.syfo.kafka

import no.nav.syfo.application.Environment
import no.nav.syfo.application.VaultSecrets
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.serialization.StringDeserializer
import java.util.*

fun kafkaConsumerConfig(
    environment: Environment,
    vaultSecrets: VaultSecrets
) = Properties().apply {
    this[ConsumerConfig.GROUP_ID_CONFIG] = "${environment.applicationName}-consumer"
    this[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
    this[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = false
    this[ConsumerConfig.MAX_POLL_RECORDS_CONFIG] = "1"
    this[CommonClientConfigs.RETRIES_CONFIG] = "2"
    this[CommonClientConfigs.SECURITY_PROTOCOL_CONFIG] = "SASL_SSL"
    this[SaslConfigs.SASL_MECHANISM] = "PLAIN"
    this["schema.registry.url"] = environment.kafkaSchemaRegistryUrl
    this[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.canonicalName
    this[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.canonicalName
    this[SaslConfigs.SASL_JAAS_CONFIG] = "org.apache.kafka.common.security.plain.PlainLoginModule required " +
        "username=\"${vaultSecrets.serviceuserUsername}\" password=\"${vaultSecrets.serviceuserPassword}\";"
    this[CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG] = environment.kafkaBootstrapServers
}
