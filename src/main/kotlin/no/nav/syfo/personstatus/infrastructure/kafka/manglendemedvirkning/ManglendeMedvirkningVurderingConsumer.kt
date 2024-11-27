package no.nav.syfo.personstatus.infrastructure.kafka.manglendemedvirkning

import no.nav.syfo.ApplicationState
import no.nav.syfo.personstatus.application.PersonoversiktStatusService
import no.nav.syfo.personstatus.domain.PersonIdent
import no.nav.syfo.personstatus.infrastructure.kafka.*
import no.nav.syfo.util.configuredJacksonMapper
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.Deserializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*

class ManglendeMedvirkningVurderingConsumer(
    private val personoversiktStatusService: PersonoversiktStatusService,
) : KafkaConsumerService<VurderingRecord> {
    override val pollDurationInMillis: Long = 1000

    override suspend fun pollAndProcessRecords(kafkaConsumer: KafkaConsumer<String, VurderingRecord>) {
        val records = kafkaConsumer.poll(Duration.ofMillis(pollDurationInMillis))
        if (records.count() > 0) {
            log.info("ManglendeMedvirkningVurderingConsumer trace: Received ${records.count()} records")
            processRecords(records = records)
            kafkaConsumer.commitSync()
        }
    }

    fun start(applicationState: ApplicationState, kafkaEnvironment: KafkaEnvironment) {
        val consumerProperties = Properties().apply {
            putAll(kafkaAivenConsumerConfig(kafkaEnvironment = kafkaEnvironment))
            this[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] =
                ManglendeMedvirkningVurderingRecordDeserializer::class.java.canonicalName
        }
        launchKafkaTask(
            applicationState = applicationState,
            kafkaConsumerService = this,
            consumerProperties = consumerProperties,
            topic = MANGLENDE_MEDVIRKNING_VURDERING_TOPIC,
        )
    }

    private fun processRecords(records: ConsumerRecords<String, VurderingRecord>): List<Result<Int>> {
        val validRecords = records.requireNoNulls()
        return validRecords.map { record ->
            val recordValue = record.value()
            personoversiktStatusService.upsertManglendeMedvirkningStatus(
                personident = PersonIdent(recordValue.personident),
                isAktivVurdering = recordValue.vurderingType.isActive,
            )
        }
    }

    companion object {
        private const val MANGLENDE_MEDVIRKNING_VURDERING_TOPIC = "teamsykefravr.manglende-medvirkning-vurdering"
        private val log: Logger = LoggerFactory.getLogger(this::class.java)
    }
}

class ManglendeMedvirkningVurderingRecordDeserializer : Deserializer<VurderingRecord> {
    private val mapper = configuredJacksonMapper()
    override fun deserialize(topic: String, data: ByteArray): VurderingRecord =
        mapper.readValue(data, VurderingRecord::class.java)
}
