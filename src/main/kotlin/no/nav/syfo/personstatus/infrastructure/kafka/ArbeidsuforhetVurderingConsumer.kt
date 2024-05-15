package no.nav.syfo.personstatus.infrastructure.kafka

import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.kafka.KafkaEnvironment
import no.nav.syfo.application.kafka.kafkaAivenConsumerConfig
import no.nav.syfo.kafka.KafkaConsumerService
import no.nav.syfo.kafka.launchKafkaTask
import no.nav.syfo.personstatus.PersonoversiktStatusService
import no.nav.syfo.util.configuredJacksonMapper
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.Deserializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*

class ArbeidsuforhetVurderingConsumer(
    private val personoversiktStatusService: PersonoversiktStatusService,
) : KafkaConsumerService<ArbeidsuforhetVurderingRecord> {

    override val pollDurationInMillis: Long = 1000

    override fun pollAndProcessRecords(kafkaConsumer: KafkaConsumer<String, ArbeidsuforhetVurderingRecord>) {
        val records = kafkaConsumer.poll(Duration.ofMillis(pollDurationInMillis))
        if (records.count() > 0) {
            log.info("ArbeidsuforhetVurderingConsumer trace: Received ${records.count()} records")
            processRecords(records = records)
            kafkaConsumer.commitSync()
        }
    }

    private fun processRecords(records: ConsumerRecords<String, ArbeidsuforhetVurderingRecord>) {
        val validRecords = records.requireNoNulls()
        validRecords.map { record ->
            val recordValue = record.value()
            log.info("ArbeidsuforhetVurderingConsumer record value:\n$recordValue")
            // personoversiktStatusService.updateArbeidsuforhetVurderingStatus(
            //  personident = PersonIdent(recordValue.personident),
            // isFinalVurdering = recordValue.isFinalVurdering,
            // )
        }
    }

    fun start(applicationState: ApplicationState, kafkaEnvironment: KafkaEnvironment) {
        val consumerProperties = Properties().apply {
            putAll(kafkaAivenConsumerConfig(kafkaEnvironment = kafkaEnvironment))
            this[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] =
                ArbeidsuforhetVurderingRecordDeserializer::class.java.canonicalName
        }
        launchKafkaTask(
            applicationState = applicationState,
            kafkaConsumerService = this,
            consumerProperties = consumerProperties,
            topic = ARBEIDSUFORHET_VURDERING_TOPIC,
        )
    }

    companion object {
        const val ARBEIDSUFORHET_VURDERING_TOPIC = "teamsykefravr.arbeidsuforhet-vurdering"
        val log: Logger = LoggerFactory.getLogger(this::class.java)
    }
}

class ArbeidsuforhetVurderingRecordDeserializer : Deserializer<ArbeidsuforhetVurderingRecord> {
    private val mapper = configuredJacksonMapper()
    override fun deserialize(topic: String, data: ByteArray): ArbeidsuforhetVurderingRecord =
        mapper.readValue(data, ArbeidsuforhetVurderingRecord::class.java)
}
