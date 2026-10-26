package no.nav.syfo.infrastructure.kafka.identhendelse

import no.nav.syfo.application.IdenthendelseService
import no.nav.syfo.infrastructure.COUNT_KAFKA_CONSUMER_PDL_AKTOR_TOMBSTONE
import no.nav.syfo.infrastructure.kafka.KafkaConsumerService
import org.apache.avro.generic.GenericData
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration

class IdenthendelseConsumerService(
    private val identhendelseService: IdenthendelseService,
    private val dryRun: Boolean = false,
) : KafkaConsumerService<GenericRecord> {
    override val pollDurationInMillis: Long = 1000

    override suspend fun pollAndProcessRecords(kafkaConsumer: KafkaConsumer<String, GenericRecord>) {
        val records = kafkaConsumer.poll(Duration.ofMillis(pollDurationInMillis))
        if (records.count() > 0) {
            var tombstones = 0
            var failed = 0
            records.forEach { record ->
                val value = record.value()
                if (value == null) {
                    tombstones++
                    COUNT_KAFKA_CONSUMER_PDL_AKTOR_TOMBSTONE.increment()
                } else if (dryRun) {
                    try {
                        identhendelseService.handleIdenthendelse(value.toKafkaIdenthendelseDTO())
                    } catch (ex: Exception) {
                        failed++
                        log.warn("Identhendelse dryrun: failed to process record at offset ${record.offset()}: ${ex.message}")
                    }
                } else {
                    identhendelseService.handleIdenthendelse(value.toKafkaIdenthendelseDTO())
                }
            }
            kafkaConsumer.commitSync()
            if (tombstones > 0 || failed > 0) {
                log.info("Identhendelse: processed ${records.count()} records from $PDL_AKTOR_TOPIC, tombstones=$tombstones, failed=$failed")
            }
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger("no.nav.syfo.identhendelse")
    }
}

fun GenericRecord.toKafkaIdenthendelseDTO(): KafkaIdenthendelseDTO {
    val identifikatorer = (get("identifikatorer") as GenericData.Array<GenericRecord>).map {
        Identifikator(
            idnummer = it.get("idnummer").toString(),
            gjeldende = it.get("gjeldende").toString().toBoolean(),
            type = when (it.get("type").toString()) {
                "FOLKEREGISTERIDENT" -> IdentType.FOLKEREGISTERIDENT
                "AKTORID" -> IdentType.AKTORID
                "NPID" -> IdentType.NPID
                else -> throw IllegalStateException("Har mottatt ident med ukjent type")
            }
        )
    }
    return KafkaIdenthendelseDTO(identifikatorer)
}
