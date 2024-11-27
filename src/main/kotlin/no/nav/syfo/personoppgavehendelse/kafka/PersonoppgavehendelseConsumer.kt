package no.nav.syfo.personoppgavehendelse.kafka

import no.nav.syfo.personstatus.infrastructure.kafka.KafkaConsumerService
import no.nav.syfo.personstatus.application.PersonoversiktStatusService
import org.apache.kafka.clients.consumer.KafkaConsumer
import java.time.Duration

class PersonoppgavehendelseConsumer(
    private val personoversiktStatusService: PersonoversiktStatusService,
) : KafkaConsumerService<KPersonoppgavehendelse> {
    override val pollDurationInMillis: Long = 100

    override suspend fun pollAndProcessRecords(kafkaConsumer: KafkaConsumer<String, KPersonoppgavehendelse>) {
        val records = kafkaConsumer.poll(Duration.ofMillis(pollDurationInMillis))

        if (records.count() > 0) {
            personoversiktStatusService.createOrUpdatePersonoversiktStatuser(
                personoppgavehendelser = records.mapNotNull { it.value() }
            )
            kafkaConsumer.commitSync()
        }
    }
}
