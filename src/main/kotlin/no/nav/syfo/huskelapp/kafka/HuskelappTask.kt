package no.nav.syfo.huskelapp.kafka

import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.database.database
import no.nav.syfo.application.kafka.KafkaEnvironment
import no.nav.syfo.application.kafka.kafkaAivenConsumerConfig
import no.nav.syfo.huskelapp.HuskelappService
import no.nav.syfo.kafka.launchKafkaTask
import no.nav.syfo.util.configuredJacksonMapper
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.Deserializer
import java.util.*

const val HUSKELAPP_TOPIC =
    "teamsykefravr.huskelapp"

fun launchHuskelappConsumer(
    applicationState: ApplicationState,
    kafkaEnvironment: KafkaEnvironment,
) {
    val huskelappService = HuskelappService(
        database = database
    )
    val huskelappConsumer = HuskelappConsumer(
        huskelappService = huskelappService,
    )
    val consumerProperties = Properties().apply {
        putAll(kafkaAivenConsumerConfig(kafkaEnvironment = kafkaEnvironment))
        this[ConsumerConfig.MAX_POLL_RECORDS_CONFIG] = "10"
        this[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = KafkaHuskelappDeserializer::class.java.canonicalName
    }
    launchKafkaTask(
        applicationState = applicationState,
        topic = HUSKELAPP_TOPIC,
        consumerProperties = consumerProperties,
        kafkaConsumerService = huskelappConsumer
    )
}

class KafkaHuskelappDeserializer : Deserializer<KafkaHuskelapp> {
    private val mapper = configuredJacksonMapper()
    override fun deserialize(topic: String, data: ByteArray): KafkaHuskelapp =
        mapper.readValue(data, KafkaHuskelapp::class.java)
}
