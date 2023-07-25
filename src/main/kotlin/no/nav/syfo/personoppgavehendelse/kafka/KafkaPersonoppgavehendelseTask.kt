package no.nav.syfo.personoppgavehendelse.kafka

import no.nav.syfo.application.kafka.KafkaEnvironment
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.kafka.kafkaAivenConsumerConfig
import no.nav.syfo.kafka.launchKafkaTask
import no.nav.syfo.personstatus.PersonoversiktStatusService
import no.nav.syfo.util.configuredJacksonMapper
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.Deserializer
import java.util.*

val PERSONOPPGAVEHENDELSE_TOPIC = "teamsykefravr.personoppgavehendelse"

fun launchKafkaTaskPersonoppgavehendelse(
    applicationState: ApplicationState,
    kafkaEnvironment: KafkaEnvironment,
    personoversiktStatusService: PersonoversiktStatusService,
) {
    val personoppgavehendelseConsumerService = PersonoppgavehendelseConsumerService(personoversiktStatusService)
    val consumerProperties = Properties().apply {
        putAll(kafkaAivenConsumerConfig(kafkaEnvironment = kafkaEnvironment))
        this[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] =
            KPersonoppgavehendelseDeserializer::class.java.canonicalName
    }
    launchKafkaTask(
        applicationState = applicationState,
        topic = PERSONOPPGAVEHENDELSE_TOPIC,
        consumerProperties = consumerProperties,
        kafkaConsumerService = personoppgavehendelseConsumerService,
    )
}

class KPersonoppgavehendelseDeserializer : Deserializer<KPersonoppgavehendelse> {
    private val mapper = configuredJacksonMapper()
    override fun deserialize(topic: String, data: ByteArray): KPersonoppgavehendelse =
        mapper.readValue(data, KPersonoppgavehendelse::class.java)
}
