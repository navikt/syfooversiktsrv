package no.nav.syfo.aktivitetskravvurdering.kafka

import no.nav.syfo.ApplicationState
import no.nav.syfo.personstatus.infrastructure.database.database
import no.nav.syfo.personstatus.infrastructure.kafka.KafkaEnvironment
import no.nav.syfo.personstatus.infrastructure.kafka.kafkaAivenConsumerConfig
import no.nav.syfo.personstatus.infrastructure.kafka.launchKafkaTask
import no.nav.syfo.util.configuredJacksonMapper
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.Deserializer
import java.util.*

const val AKTIVITETSKRAV_VURDERING_TOPIC = "teamsykefravr.aktivitetskrav-vurdering"

fun launchKafkaTaskAktivitetskravVurdering(
    applicationState: ApplicationState,
    kafkaEnvironment: KafkaEnvironment,
) {
    val kafkaAktivitetskravVurderingConsumer = KafkaAktivitetskravVurderingConsumer(
        database = database,
    )
    val consumerProperties = Properties().apply {
        putAll(kafkaAivenConsumerConfig(kafkaEnvironment = kafkaEnvironment))
        this[ConsumerConfig.MAX_POLL_RECORDS_CONFIG] = "10"
        this[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = KafkaAktivitetskravVurderingDeserializer::class.java
    }

    launchKafkaTask(
        applicationState = applicationState,
        topic = AKTIVITETSKRAV_VURDERING_TOPIC,
        consumerProperties = consumerProperties,
        kafkaConsumerService = kafkaAktivitetskravVurderingConsumer,
    )
}

class KafkaAktivitetskravVurderingDeserializer : Deserializer<KafkaAktivitetskravVurdering> {
    private val mapper = configuredJacksonMapper()
    override fun deserialize(topic: String, data: ByteArray): KafkaAktivitetskravVurdering =
        mapper.readValue(data, KafkaAktivitetskravVurdering::class.java)
}
