package no.nav.syfo.pdlpersonhendelse.kafka

import no.nav.person.pdl.leesah.Personhendelse
import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.kafka.KafkaConsumerService
import no.nav.syfo.personstatus.db.getPersonOversiktStatusList
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory
import java.time.Duration

class KafkaPersonhendelseConsumerService(
    private val database: DatabaseInterface,
) : KafkaConsumerService<Personhendelse> {
    override val pollDurationInMillis: Long = 1000

    override fun pollAndProcessRecords(kafkaConsumer: KafkaConsumer<String, Personhendelse>) {
        val records = kafkaConsumer.poll(Duration.ofMillis(pollDurationInMillis))
        if (records.count() > 0) {
            processRecords(records)
            kafkaConsumer.commitSync()
        }
    }

    private fun processRecords(records: ConsumerRecords<String, Personhendelse>) {
        val (tombstoneRecords, validRecords) = records.partition { it.value() == null }

        if (tombstoneRecords.isNotEmpty()) {
            val numberOfTombstones = tombstoneRecords.size
            log.error("Value of $numberOfTombstones ConsumerRecord are null, most probably due to a tombstone. Contact the owner of the topic if an error is suspected")
            // TODO: Add counter
        }
        validRecords.forEach { record ->
            handlePersonhendelse(record.value())
        }
    }

    private fun handlePersonhendelse(personhendelse: Personhendelse) {
        if (personhendelse.navn != null) {
            personhendelse.personidenter.forEach {
                val personOversiktStatusList = database.getPersonOversiktStatusList(it)
                if (personOversiktStatusList.isNotEmpty()) {
                    // TODO, Alternativ1: kall PDL for navn og oppdater databasen
                    // TODO, Alternativ2: slett navnet i databasen, og så populeres det neste gang uansett
                    log.info("Personhendelse: Endring av navn på person vi har i databasen")
                }
            }
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(KafkaPersonhendelseConsumerService::class.java)
    }
}
