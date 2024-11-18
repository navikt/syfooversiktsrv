package no.nav.syfo.oppfolgingstilfelle.kafka

import no.nav.syfo.ApplicationState
import no.nav.syfo.personstatus.application.OppfolgingstilfelleService
import no.nav.syfo.personstatus.infrastructure.kafka.KafkaEnvironment
import no.nav.syfo.personstatus.infrastructure.kafka.kafkaAivenConsumerConfig
import no.nav.syfo.personstatus.infrastructure.kafka.launchKafkaTask
import no.nav.syfo.util.configuredJacksonMapper
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.Deserializer
import java.util.*

const val OPPFOLGINGSTILFELLE_PERSON_TOPIC =
    "teamsykefravr.isoppfolgingstilfelle-oppfolgingstilfelle-person"

fun launchKafkaTaskOppfolgingstilfellePerson(
    applicationState: ApplicationState,
    kafkaEnvironment: KafkaEnvironment,
    oppfolgingstilfelleService: OppfolgingstilfelleService,
) {
    val oppfolgingstilfelleConsumer = OppfolgingstilfelleConsumer(
        oppfolgingstilfelleService = oppfolgingstilfelleService
    )
    val consumerProperties = Properties().apply {
        putAll(kafkaAivenConsumerConfig(kafkaEnvironment = kafkaEnvironment))
        this[ConsumerConfig.MAX_POLL_RECORDS_CONFIG] = "10"
        this[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] =
            KafkaOppfolgingstilfellePersonDeserializer::class.java.canonicalName
    }
    launchKafkaTask(
        applicationState = applicationState,
        topic = OPPFOLGINGSTILFELLE_PERSON_TOPIC,
        consumerProperties = consumerProperties,
        kafkaConsumerService = oppfolgingstilfelleConsumer
    )
}

class KafkaOppfolgingstilfellePersonDeserializer : Deserializer<OppfolgingstilfellePersonRecord> {
    private val mapper = configuredJacksonMapper()
    override fun deserialize(topic: String, data: ByteArray): OppfolgingstilfellePersonRecord =
        mapper.readValue(data, OppfolgingstilfellePersonRecord::class.java)
}
