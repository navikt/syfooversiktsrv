package no.nav.syfo.kafka

import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.backgroundtask.launchBackgroundTask
import no.nav.syfo.identhendelse.kafka.PDL_AKTOR_TOPIC
import org.apache.kafka.clients.consumer.KafkaConsumer
import java.util.*

inline fun <reified ConsumerRecordValue> launchKafkaTask(
    applicationState: ApplicationState,
    topic: String,
    consumerProperties: Properties,
    kafkaConsumerService: KafkaConsumerService<ConsumerRecordValue>,
) {
    launchBackgroundTask(
        applicationState = applicationState
    ) {
        val kafkaConsumer = KafkaConsumer<String, ConsumerRecordValue>(consumerProperties)
        kafkaConsumer.subscribe(
            listOf(topic)
        )

        while (applicationState.ready) {
            if (kafkaConsumer.subscription().isEmpty()) {
                kafkaConsumer.subscribe(listOf(PDL_AKTOR_TOPIC))
            }
            kafkaConsumerService.pollAndProcessRecords(kafkaConsumer)
        }
    }
}
