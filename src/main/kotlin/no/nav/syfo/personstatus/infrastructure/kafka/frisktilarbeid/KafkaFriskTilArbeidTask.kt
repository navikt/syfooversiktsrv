package no.nav.syfo.personstatus.infrastructure.kafka.frisktilarbeid

import no.nav.syfo.ApplicationState
import no.nav.syfo.personstatus.infrastructure.database.database
import no.nav.syfo.personstatus.infrastructure.kafka.KafkaEnvironment
import no.nav.syfo.personstatus.infrastructure.kafka.kafkaAivenConsumerConfig
import no.nav.syfo.personstatus.infrastructure.kafka.launchKafkaTask
import no.nav.syfo.util.configuredJacksonMapper
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.Deserializer
import java.util.*

const val FRISK_TIL_ARBEID_VEDTAK_TOPIC = "teamsykefravr.isfrisktilarbeid-vedtak-status"

fun launchKafkaTaskFriskTilArbeidVedtak(
    applicationState: ApplicationState,
    kafkaEnvironment: KafkaEnvironment,
) {
    val friskTilArbeidVedtakConsumer = FriskTilArbeidVedtakConsumer(
        database = database,
    )
    val consumerProperties = Properties().apply {
        putAll(kafkaAivenConsumerConfig(kafkaEnvironment = kafkaEnvironment))
        this[ConsumerConfig.MAX_POLL_RECORDS_CONFIG] = "10"
        this[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = VedtakStatusRecordDeserializer::class.java
    }

    launchKafkaTask(
        applicationState = applicationState,
        topic = FRISK_TIL_ARBEID_VEDTAK_TOPIC,
        consumerProperties = consumerProperties,
        kafkaConsumerService = friskTilArbeidVedtakConsumer,
    )
}

class VedtakStatusRecordDeserializer : Deserializer<VedtakStatusRecord> {
    private val mapper = configuredJacksonMapper()
    override fun deserialize(topic: String, data: ByteArray): VedtakStatusRecord =
        mapper.readValue(data, VedtakStatusRecord::class.java)
}
