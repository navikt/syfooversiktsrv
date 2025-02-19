package no.nav.syfo.personstatus.infrastructure.kafka.aktivitetskrav

import no.nav.syfo.ApplicationState
import no.nav.syfo.personstatus.application.PersonoversiktStatusService
import no.nav.syfo.personstatus.infrastructure.kafka.KafkaEnvironment
import no.nav.syfo.personstatus.infrastructure.kafka.kafkaAivenConsumerConfig
import no.nav.syfo.personstatus.infrastructure.kafka.launchKafkaTask
import no.nav.syfo.util.configuredJacksonMapper
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.Deserializer
import java.util.Properties
import kotlin.apply
import kotlin.collections.set
import kotlin.jvm.java

const val AKTIVITETSKRAV_VURDERING_TOPIC = "teamsykefravr.aktivitetskrav-vurdering"

fun launchKafkaTaskAktivitetskravVurdering(
    applicationState: ApplicationState,
    kafkaEnvironment: KafkaEnvironment,
    personoversiktStatusService: PersonoversiktStatusService,
) {
    val aktivitetskravVurderingConsumer = AktivitetskravVurderingConsumer(
        personoversiktStatusService = personoversiktStatusService,
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
        kafkaConsumerService = aktivitetskravVurderingConsumer,
    )
}

class KafkaAktivitetskravVurderingDeserializer : Deserializer<AktivitetskravVurderingRecord> {
    private val mapper = configuredJacksonMapper()
    override fun deserialize(topic: String, data: ByteArray): AktivitetskravVurderingRecord =
        mapper.readValue(data, AktivitetskravVurderingRecord::class.java)
}
