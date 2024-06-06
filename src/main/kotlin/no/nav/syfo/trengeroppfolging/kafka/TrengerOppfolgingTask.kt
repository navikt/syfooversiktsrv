package no.nav.syfo.trengeroppfolging.kafka

import no.nav.syfo.ApplicationState
import no.nav.syfo.personstatus.infrastructure.database.database
import no.nav.syfo.personstatus.infrastructure.kafka.KafkaEnvironment
import no.nav.syfo.personstatus.infrastructure.kafka.kafkaAivenConsumerConfig
import no.nav.syfo.cronjob.behandlendeenhet.PersonBehandlendeEnhetService
import no.nav.syfo.trengeroppfolging.TrengerOppfolgingService
import no.nav.syfo.personstatus.infrastructure.kafka.launchKafkaTask
import no.nav.syfo.util.configuredJacksonMapper
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.Deserializer
import java.util.*

const val HUSKELAPP_TOPIC =
    "teamsykefravr.huskelapp"

fun launchTrengerOppfolgingConsumer(
    applicationState: ApplicationState,
    kafkaEnvironment: KafkaEnvironment,
    personBehandlendeEnhetService: PersonBehandlendeEnhetService,
) {
    val trengerOppfolgingService = TrengerOppfolgingService(
        database = database,
        personBehandlendeEnhetService = personBehandlendeEnhetService,
    )
    val trengerOppfolgingConsumer = TrengerOppfolgingConsumer(
        trengerOppfolgingService = trengerOppfolgingService,
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
        kafkaConsumerService = trengerOppfolgingConsumer
    )
}

class KafkaHuskelappDeserializer : Deserializer<KafkaHuskelapp> {
    private val mapper = configuredJacksonMapper()
    override fun deserialize(topic: String, data: ByteArray): KafkaHuskelapp =
        mapper.readValue(data, KafkaHuskelapp::class.java)
}
