package no.nav.syfo.personstatus.infrastructure.kafka.kartleggingssporsmal

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

class KartleggingssporsmalKandidatConsumer(
    private val personoversiktStatusService: PersonoversiktStatusService,
) : KafkaConsumerService<KartleggingssporsmalKandidatRecord> {

    override val pollDurationInMillis: Long = 1000

    override suspend fun pollAndProcessRecords(kafkaConsumer: KafkaConsumer<String, KartleggingssporsmalKandidatRecord>) {
        val records = kafkaConsumer.poll(Duration.ofMillis(pollDurationInMillis))
        if (records.count() > 0) {
            log.info("KartleggingssporsmalKandidatConsumer trace: Received ${records.count()} records")
            processRecords(records = records)
            kafkaConsumer.commitSync()
        }
    }

    // TODO: Legg til hvordan dette blir en "aktiv" vurdering
    private fun processRecords(records: ConsumerRecords<String, KartleggingssporsmalKandidatRecord>): List<Result<Int>> {
        val validRecords = records.requireNoNulls()
        return validRecords.map { record ->
            val recordValue = record.value()
            personoversiktStatusService.upsertKartleggingssporsmalVurdering(
                personident = PersonIdent(recordValue.personident),
                isAktivVurdering = false,
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
            topic = KARTLEGGINGSSPORSMAL_KANDIDAT_STATUS_TOPIC,
        )
    }

    companion object {
        const val KARTLEGGINGSSPORSMAL_KANDIDAT_STATUS_TOPIC = "teamsykefravr.ismeroppfolging-kartleggingssporsmal-kandidat"
        private val log: Logger = LoggerFactory.getLogger(this::class.java)
    }
}

data class KartleggingssporsmalKandidatRecord(
    val uuid: UUID,
    val personident: String,
)

class KandidatStatusRecordDeserializer : Deserializer<KartleggingssporsmalKandidatRecord> {
    private val mapper = configuredJacksonMapper()
    override fun deserialize(topic: String, data: ByteArray): KartleggingssporsmalKandidatRecord =
        mapper.readValue(data, KartleggingssporsmalKandidatRecord::class.java)
}
