package no.nav.syfo.infrastructure.kafka.utenlandsopphold

import no.nav.syfo.ApplicationState
import no.nav.syfo.application.PersonoversiktStatusService
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.infrastructure.kafka.KafkaConsumerService
import no.nav.syfo.infrastructure.kafka.KafkaEnvironment
import no.nav.syfo.infrastructure.kafka.kafkaAivenConsumerConfig
import no.nav.syfo.infrastructure.kafka.launchKafkaTask
import no.nav.syfo.util.configuredJacksonMapper
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.Deserializer
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.Properties
import java.util.UUID

class UtenlandsoppholdSoknadstatusConsumer(
    private val personoversiktStatusService: PersonoversiktStatusService,
) : KafkaConsumerService<UtenlandsoppholdSoknadstatusRecord> {

    override val pollDurationInMillis: Long = 1000

    override suspend fun pollAndProcessRecords(
        kafkaConsumer: KafkaConsumer<String, UtenlandsoppholdSoknadstatusRecord>,
    ) {
        val records = kafkaConsumer.poll(Duration.ofMillis(pollDurationInMillis))
        if (records.count() > 0) {
            log.info("Received ${records.count()} utenlandsopphold soknadstatus records")
            processRecords(records)
            kafkaConsumer.commitSync()
        }
    }

    private fun processRecords(records: ConsumerRecords<String, UtenlandsoppholdSoknadstatusRecord>) {
        records.requireNoNulls().forEach { record ->
            val recordValue = record.value()
            val result = when (recordValue.status) {
                UtenlandsoppholdSoknadstatus.MOTTATT ->
                    personoversiktStatusService.addUtenlandsoppholdSoknad(
                        personident = PersonIdent(recordValue.personident),
                        soknadUuid = recordValue.uuid,
                    )
                UtenlandsoppholdSoknadstatus.BEHANDLET ->
                    personoversiktStatusService.removeUtenlandsoppholdSoknad(
                        personident = PersonIdent(recordValue.personident),
                        soknadUuid = recordValue.uuid,
                    )
            }
            result.getOrThrow()
        }
    }

    fun start(applicationState: ApplicationState, kafkaEnvironment: KafkaEnvironment) {
        val consumerProperties = Properties().apply {
            putAll(kafkaAivenConsumerConfig(kafkaEnvironment = kafkaEnvironment))
            this[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] =
                UtenlandsoppholdSoknadstatusRecordDeserializer::class.java.canonicalName
            this[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "latest"
        }
        launchKafkaTask(
            applicationState = applicationState,
            kafkaConsumerService = this,
            consumerProperties = consumerProperties,
            topic = UTENLANDSOPPHOLD_SOKNAD_STATUS_TOPIC,
        )
    }

    companion object {
        const val UTENLANDSOPPHOLD_SOKNAD_STATUS_TOPIC = "teamsykefravr.utenlandsopphold-soknad-status"
        private val log = LoggerFactory.getLogger(UtenlandsoppholdSoknadstatusConsumer::class.java)
    }
}

data class UtenlandsoppholdSoknadstatusRecord(
    val uuid: UUID,
    val personident: String,
    val status: UtenlandsoppholdSoknadstatus,
)

enum class UtenlandsoppholdSoknadstatus {
    MOTTATT,
    BEHANDLET,
}

class UtenlandsoppholdSoknadstatusRecordDeserializer : Deserializer<UtenlandsoppholdSoknadstatusRecord> {
    private val mapper = configuredJacksonMapper()

    override fun deserialize(topic: String, data: ByteArray): UtenlandsoppholdSoknadstatusRecord =
        mapper.readValue(data, UtenlandsoppholdSoknadstatusRecord::class.java)
}
