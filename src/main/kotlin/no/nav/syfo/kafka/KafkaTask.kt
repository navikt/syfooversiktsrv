package no.nav.syfo.kafka

import no.nav.syfo.LOG
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.backgroundtask.launchBackgroundTask
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
        LOG.info("Setting up kafka consumer for ${ConsumerRecordValue::class.java.simpleName}")

        val kafkaConsumer = KafkaConsumer<String, ConsumerRecordValue>(consumerProperties)
        kafkaConsumer.subscribe(
            listOf(topic)
        )

        while (applicationState.ready) {
            kafkaConsumerService.pollAndProcessRecords(kafkaConsumer)
        }
    }
}
