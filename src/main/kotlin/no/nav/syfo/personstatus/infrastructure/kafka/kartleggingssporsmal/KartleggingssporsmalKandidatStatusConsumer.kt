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
import java.time.OffsetDateTime
import java.util.*
import kotlin.jvm.java

class KartleggingssporsmalKandidatStatusConsumer(
    private val personoversiktStatusService: PersonoversiktStatusService,
) : KafkaConsumerService<KartleggingssporsmalKandidatStatusRecord> {

    override val pollDurationInMillis: Long = 1000

    override suspend fun pollAndProcessRecords(kafkaConsumer: KafkaConsumer<String, KartleggingssporsmalKandidatStatusRecord>) {
        val records = kafkaConsumer.poll(Duration.ofMillis(pollDurationInMillis))
        if (records.count() > 0) {
            log.info("KartleggingssporsmalKandidatStatusConsumer trace: Received ${records.count()} records")
            processRecords(records = records)
            kafkaConsumer.commitSync()
        }
    }

    private fun processRecords(records: ConsumerRecords<String, KartleggingssporsmalKandidatStatusRecord>): List<Result<Int>> {
        val validRecords = records.requireNoNulls()
        return validRecords.map { record ->
            val recordValue = record.value()
            personoversiktStatusService.upsertKartleggingssporsmalKandidatStatus(
                personident = PersonIdent(recordValue.personident),
                isAktivKandidat = recordValue.isAktivKandidat(),
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

data class KartleggingssporsmalKandidatStatusRecord(
    val kandidatUuid: UUID,
    val personident: String,
    val createdAt: OffsetDateTime,
    val status: String, // KANDIDAT, SVAR_MOTTATT, FERDIG_BEHANDLET
)

private fun KartleggingssporsmalKandidatStatusRecord.isAktivKandidat(): Boolean = status == "SVAR_MOTTATT"

class KandidatStatusRecordDeserializer : Deserializer<KartleggingssporsmalKandidatStatusRecord> {
    private val mapper = configuredJacksonMapper()
    override fun deserialize(topic: String, data: ByteArray): KartleggingssporsmalKandidatStatusRecord =
        mapper.readValue(data, KartleggingssporsmalKandidatStatusRecord::class.java)
}
