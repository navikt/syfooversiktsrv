package no.nav.syfo.personoppgavehendelse.kafka

import no.nav.syfo.kafka.KafkaConsumerService
import no.nav.syfo.personstatus.PersonoversiktStatusService
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory
import java.time.Duration

class PersonoppgavehendelseConsumer(
    private val personoversiktStatusService: PersonoversiktStatusService,
) : KafkaConsumerService<KPersonoppgavehendelse> {
    override val pollDurationInMillis: Long = 100

    override fun pollAndProcessRecords(kafkaConsumer: KafkaConsumer<String, KPersonoppgavehendelse>) {
        val records = kafkaConsumer.poll(Duration.ofMillis(pollDurationInMillis))

        if (records.count() > 0) {
            log.info("Received ${records.count()} records")
            personoversiktStatusService.createOrUpdatePersonoversiktStatuser(
                personoppgavehendelser = records.mapNotNull { it.value() }
            )
            kafkaConsumer.commitSync()
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(PersonoppgavehendelseConsumer::class.java)
    }
}
