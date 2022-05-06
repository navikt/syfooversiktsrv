package no.nav.syfo.oppfolgingstilfelle.kafka

import no.nav.syfo.application.kafka.KafkaEnvironment
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.database.database
import no.nav.syfo.application.kafka.kafkaAivenConsumerConfig
import no.nav.syfo.kafka.launchKafkaTask
import no.nav.syfo.util.configuredJacksonMapper
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.Deserializer
import java.util.*

const val OPPFOLGINGSTILFELLE_PERSON_TOPIC =
    "teamsykefravr.isoppfolgingstilfelle-oppfolgingstilfelle-person"

fun launchKafkaTaskOppfolgingstilfellePerson(
    applicationState: ApplicationState,
    kafkaEnvironment: KafkaEnvironment,
) {
    val kafkaOppfolgingstilfellePersonService = KafkaOppfolgingstilfellePersonService(
        database = database,
    )
    val consumerProperties = Properties().apply {
        putAll(kafkaAivenConsumerConfig(kafkaEnvironment = kafkaEnvironment))
        this[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] =
            KafkaOppfolgingstilfellePersonDeserializer::class.java.canonicalName
    }
    launchKafkaTask(
        applicationState = applicationState,
        topic = OPPFOLGINGSTILFELLE_PERSON_TOPIC,
        consumerProperties = consumerProperties,
        kafkaConsumerService = kafkaOppfolgingstilfellePersonService
    )
}

class KafkaOppfolgingstilfellePersonDeserializer : Deserializer<KafkaOppfolgingstilfellePerson> {
    private val mapper = configuredJacksonMapper()
    override fun deserialize(topic: String, data: ByteArray): KafkaOppfolgingstilfellePerson =
        mapper.readValue(data, KafkaOppfolgingstilfellePerson::class.java)
}
