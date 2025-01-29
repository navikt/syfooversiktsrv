package no.nav.syfo.personstatus.infrastructure.kafka.oppfolgingsoppgave

import no.nav.syfo.ApplicationState
import no.nav.syfo.personstatus.infrastructure.database.database
import no.nav.syfo.personstatus.infrastructure.kafka.KafkaEnvironment
import no.nav.syfo.personstatus.infrastructure.kafka.kafkaAivenConsumerConfig
import no.nav.syfo.personstatus.application.PersonBehandlendeEnhetService
import no.nav.syfo.personstatus.application.oppfolgingsoppgave.OppfolgingsoppgaveRecord
import no.nav.syfo.personstatus.application.oppfolgingsoppgave.OppfolgingsoppgaveService
import no.nav.syfo.personstatus.infrastructure.kafka.launchKafkaTask
import no.nav.syfo.util.configuredJacksonMapper
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.Deserializer
import java.util.Properties
import kotlin.apply
import kotlin.collections.set
import kotlin.jvm.java

const val HUSKELAPP_TOPIC =
    "teamsykefravr.huskelapp"

fun launchOppfolgingsoppgaveConsumer(
    applicationState: ApplicationState,
    kafkaEnvironment: KafkaEnvironment,
    personBehandlendeEnhetService: PersonBehandlendeEnhetService,
) {
    val oppfolgingsoppgaveService = OppfolgingsoppgaveService(
        database = database,
        personBehandlendeEnhetService = personBehandlendeEnhetService,
    )
    val oppfolgingsoppgaveConsumer = OppfolgingsoppgaveConsumer(
        oppfolgingsoppgaveService = oppfolgingsoppgaveService,
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
        kafkaConsumerService = oppfolgingsoppgaveConsumer
    )
}

class KafkaHuskelappDeserializer : Deserializer<OppfolgingsoppgaveRecord> {
    private val mapper = configuredJacksonMapper()
    override fun deserialize(topic: String, data: ByteArray): OppfolgingsoppgaveRecord =
        mapper.readValue(data, OppfolgingsoppgaveRecord::class.java)
}
