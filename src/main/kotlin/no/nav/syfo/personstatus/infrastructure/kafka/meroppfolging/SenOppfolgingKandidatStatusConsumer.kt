package no.nav.syfo.personstatus.infrastructure.kafka.meroppfolging

import no.nav.syfo.ApplicationState
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.personstatus.PersonoversiktStatusService
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

class SenOppfolgingKandidatStatusConsumer(
    private val personoversiktStatusService: PersonoversiktStatusService,
) : KafkaConsumerService<KandidatStatusRecord> {

    override val pollDurationInMillis: Long = 1000

    override fun pollAndProcessRecords(kafkaConsumer: KafkaConsumer<String, KandidatStatusRecord>) {
        val records = kafkaConsumer.poll(Duration.ofMillis(pollDurationInMillis))
        if (records.count() > 0) {
            log.info("SenOppfolgingKandidatStatusConsumer trace: Received ${records.count()} records")
            processRecords(records = records)
            kafkaConsumer.commitSync()
        }
    }

    private fun processRecords(records: ConsumerRecords<String, KandidatStatusRecord>): List<Result<Int>> {
        val validRecords = records.requireNoNulls()
        return validRecords.map { record ->
            val recordValue = record.value()
            personoversiktStatusService.upsertSenOppfolgingKandidat(
                personident = PersonIdent(recordValue.personident),
                isAktivKandidat = recordValue.status.isActive,
            )
        }
    }

    fun start(applicationState: ApplicationState, kafkaEnvironment: KafkaEnvironment) {
        val consumerProperties = Properties().apply {
            putAll(kafkaAivenConsumerConfig(kafkaEnvironment = kafkaEnvironment))
            this[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] =
                KandidatStatusRecordDeserializer::class.java.canonicalName
        }
        launchKafkaTask(
            applicationState = applicationState,
            kafkaConsumerService = this,
            consumerProperties = consumerProperties,
            topic = SEN_OPPFOLGING_KANDIDAT_STATUS_TOPIC,
        )
    }

    companion object {
        const val SEN_OPPFOLGING_KANDIDAT_STATUS_TOPIC = "teamsykefravr.ismeroppfolging-senoppfolging-kandidat-status"
        private val log: Logger = LoggerFactory.getLogger(this::class.java)
    }
}

class KandidatStatusRecordDeserializer : Deserializer<KandidatStatusRecord> {
    private val mapper = configuredJacksonMapper()
    override fun deserialize(topic: String, data: ByteArray): KandidatStatusRecord =
        mapper.readValue(data, KandidatStatusRecord::class.java)
}
