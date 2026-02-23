package no.nav.syfo.infrastructure.kafka.behandlendeenhet

import no.nav.syfo.ApplicationState
import no.nav.syfo.application.PersonBehandlendeEnhetService
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
import java.time.OffsetDateTime
import java.util.Properties

class BehandlendeEnhetConsumer(
    private val personBehandlendeEnhetService: PersonBehandlendeEnhetService,
) : KafkaConsumerService<BehandlendeEnhetUpdateRecord> {

    override val pollDurationInMillis: Long = 1000

    override suspend fun pollAndProcessRecords(consumer: KafkaConsumer<String, BehandlendeEnhetUpdateRecord>) {
        val records = consumer.poll(Duration.ofMillis(pollDurationInMillis))
        if (records.count() > 0) {
            log.info("BehandlendeEnhetConsumer trace: Received ${records.count()} records")
            processRecords(records = records)
            consumer.commitSync()
        }
    }

    private suspend fun processRecords(records: ConsumerRecords<String, BehandlendeEnhetUpdateRecord>) {
        val (tombstoneRecords, validRecords) = records.partition { it.value() == null }
        if (tombstoneRecords.isNotEmpty()) {
            val numberOfTombstones = tombstoneRecords.size
            log.error("Value of $numberOfTombstones ConsumerRecord are null, most probably due to a tombstone. Contact the owner of the topic if an error is suspected")
        }
        validRecords.map { record ->
            personBehandlendeEnhetService.updateBehandlendeEnhet(
                PersonIdent(record.value().personident)
            )
        }
    }

    fun start(applicationState: ApplicationState, kafkaEnvironment: KafkaEnvironment) {
        val consumerProperties = Properties().apply {
            putAll(kafkaAivenConsumerConfig(kafkaEnvironment = kafkaEnvironment))
            this[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = BehandlendeEnhetUpdateRecordDeserializer::class.java.canonicalName
            this[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "latest"
        }
        launchKafkaTask(
            applicationState = applicationState,
            kafkaConsumerService = this,
            consumerProperties = consumerProperties,
            topic = TOPIC,
        )
    }

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)
        private const val TOPIC = "teamsykefravr.behandlendeenhet"
    }
}

data class BehandlendeEnhetUpdateRecord(
    val personident: String,
    val updatedAt: OffsetDateTime,
)

class BehandlendeEnhetUpdateRecordDeserializer : Deserializer<BehandlendeEnhetUpdateRecord> {
    private val mapper = configuredJacksonMapper()
    override fun deserialize(topic: String, data: ByteArray): BehandlendeEnhetUpdateRecord =
        mapper.readValue(data, BehandlendeEnhetUpdateRecord::class.java)
}
