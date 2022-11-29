package no.nav.syfo.pdlpersonhendelse.kafka

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.kafka.KafkaConsumerService
import org.apache.avro.generic.GenericData
import org.apache.avro.generic.GenericRecord
import org.apache.avro.util.Utf8
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDate

class KafkaPersonhendelseConsumerService(
    private val database: DatabaseInterface,
) : KafkaConsumerService<GenericRecord> {
    override val pollDurationInMillis: Long = 1000

    override fun pollAndProcessRecords(kafkaConsumer: KafkaConsumer<String, GenericRecord>) {
        val records = kafkaConsumer.poll(Duration.ofMillis(pollDurationInMillis))
        if (records.count() > 0) {
            processRecords(records)
            kafkaConsumer.commitSync()
        }
    }

    private fun processRecords(records: ConsumerRecords<String, GenericRecord>) {
        val (tombstoneRecords, validRecords) = records.partition { it.value() == null }

        if (tombstoneRecords.isNotEmpty()) {
            val numberOfTombstones = tombstoneRecords.size
            log.error("Value of $numberOfTombstones ConsumerRecord are null, most probably due to a tombstone. Contact the owner of the topic if an error is suspected")
            // TODO: Add counter
        }
        validRecords.forEach { record ->
            record.value().toKafkaPersonhendelseDTO()
            // TODO: Process valid records
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(KafkaPersonhendelseConsumerService::class.java)
    }
}

fun GenericRecord.toKafkaPersonhendelseDTO(): KafkaPersonhendelseDTO {
    val hendelseId = get("hendelseId").toString()
    val personidenter = (get("personidenter") as GenericData.Array<Utf8>).map { it.toString() }
    val master = get("master").toString()
    val opprettet = get("opprettet").toString() // egentlig timestamp
    val opplysingstype = get("opplysningstype").toString()
    val endringstype = when (get("endringstype").toString()) {
        "OPPRETTET" -> Endringstype.OPPRETTET
        "KORRIGERT" -> Endringstype.KORRIGERT
        "ANNULLERT" -> Endringstype.ANNULLERT
        "OPPHOERT" -> Endringstype.OPPHOERT
        else -> throw IllegalStateException("Har mottatt ukjent endringstype")
    }
    val tidligereHendelseId = if (get("tidligereHendelseId").equals(null)) null else get("tidligereHendelseId").toString()

    val navn = (get("navn") as GenericRecord?)?.let {
        val fornavn = it.get("fornavn").toString()
        val mellomnavn = if (it.get("mellomnavn").equals(null)) null else it.get("mellomnavn").toString()
        val etternavn = it.get("etternavn").toString()
        val gyldigFraOgMed = if (it.get("gyldigFraOgMed").equals(null)) null else it.get("gyldigFraOgMed").toString()
        Navn(
            fornavn = fornavn,
            mellomnavn = mellomnavn,
            etternavn = etternavn,
            gyldigFraOgMed = LocalDate.parse(gyldigFraOgMed) // TODO: sjekk denne
        )
    }

    return KafkaPersonhendelseDTO(
        hendelseId = hendelseId,
        personidenter = personidenter,
        master = master,
        opprettet = opprettet,
        opplysningstype = opplysingstype,
        endringstype = endringstype,
        tidligereHendelseId = tidligereHendelseId,
        navn = navn,
    )
}
