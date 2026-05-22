package no.nav.syfo.infrastructure.kafka

import kotlinx.coroutines.delay
import no.nav.syfo.ApplicationState
import no.nav.syfo.launchBackgroundTask
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory
import java.util.Properties

@PublishedApi
internal val kafkaTaskLog = LoggerFactory.getLogger("no.nav.syfo.infrastructure.kafka.KafkaTask")

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

        var consecutiveErrors = 0
        while (applicationState.ready) {
            try {
                if (kafkaConsumer.subscription().isEmpty()) {
                    kafkaConsumer.subscribe(listOf(topic))
                }
                kafkaConsumerService.pollAndProcessRecords(kafkaConsumer)
                consecutiveErrors = 0
            } catch (ex: Exception) {
                consecutiveErrors++
                val delayMs = minOf(consecutiveErrors * 2000L, 120_000L)
                kafkaTaskLog.error(
                    "Exception in kafka consumer for topic $topic (consecutive errors: $consecutiveErrors). Retrying after ${delayMs}ms.",
                    ex
                )
                delay(delayMs)
            }
        }
    }
}
