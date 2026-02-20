package no.nav.syfo.infrastructure.kafka.dialogmotekandidat

import no.nav.syfo.ApplicationState
import no.nav.syfo.application.IPersonOversiktStatusRepository
import no.nav.syfo.application.ITransactionManager
import no.nav.syfo.infrastructure.kafka.KafkaEnvironment
import no.nav.syfo.infrastructure.kafka.kafkaAivenConsumerConfig
import no.nav.syfo.infrastructure.kafka.launchKafkaTask
import no.nav.syfo.util.configuredJacksonMapper
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.Deserializer
import java.util.*

const val DIALOGMOTEKANDIDAT_TOPIC = "teamsykefravr.isdialogmotekandidat-dialogmotekandidat"

fun launchKafkaTaskDialogmotekandidatEndring(
    applicationState: ApplicationState,
    kafkaEnvironment: KafkaEnvironment,
    personOversiktStatusRepository: IPersonOversiktStatusRepository,
    transactionManager: ITransactionManager,
) {
    val dialogmotekandidatEndringConsumer = DialogmotekandidatEndringConsumer(
        transactionManager = transactionManager,
        personoversiktStatusRepository = personOversiktStatusRepository,
    )
    val consumerProperties = Properties().apply {
        putAll(kafkaAivenConsumerConfig(kafkaEnvironment = kafkaEnvironment))
        this[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = KafkaDialogmotekandidatEndringDeserializer::class.java
    }

    launchKafkaTask(
        applicationState = applicationState,
        topic = DIALOGMOTEKANDIDAT_TOPIC,
        consumerProperties = consumerProperties,
        kafkaConsumerService = dialogmotekandidatEndringConsumer,
    )
}

class KafkaDialogmotekandidatEndringDeserializer : Deserializer<KafkaDialogmotekandidatEndring> {
    private val mapper = configuredJacksonMapper()
    override fun deserialize(topic: String, data: ByteArray): KafkaDialogmotekandidatEndring =
        mapper.readValue(data, KafkaDialogmotekandidatEndring::class.java)
}
