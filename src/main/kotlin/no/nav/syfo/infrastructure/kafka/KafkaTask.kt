package no.nav.syfo.infrastructure.kafka

import kotlinx.coroutines.delay
import no.nav.syfo.ApplicationState
import no.nav.syfo.launchBackgroundTask
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory
import java.util.Properties
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.milliseconds

@PublishedApi
internal val kafkaTaskLog = LoggerFactory.getLogger("no.nav.syfo.infrastructure.kafka.KafkaTask")

inline fun <reified ConsumerRecordValue> launchKafkaTask(
    applicationState: ApplicationState,
    topic: String,
    consumerProperties: Properties,
    kafkaConsumerService: KafkaConsumerService<ConsumerRecordValue>,
) {
    launchBackgroundTask(
        applicationState = applicationState,
    ) {
        var consecutiveErrors = 0
        while (applicationState.ready) {
            var kafkaConsumer: KafkaConsumer<String, ConsumerRecordValue>? = null
            try {
                kafkaConsumer = KafkaConsumer(consumerProperties)
                consecutiveErrors = 0
                pollWithRetry(applicationState, topic, kafkaConsumer, kafkaConsumerService)
            } catch (ex: CancellationException) {
                throw ex
            } catch (ex: Exception) {
                kafkaTaskLog.error(
                    "Failed to create kafka consumer for topic $topic. Recreating consumer.",
                    ex,
                )
                consecutiveErrors++
                val delayMs = minOf(consecutiveErrors * 2000L, 120_000L)
                if (applicationState.ready) {
                    delay(delayMs.milliseconds)
                }
            } finally {
                kafkaConsumer?.close()
            }
        }
    }
}

@PublishedApi
internal suspend fun <ConsumerRecordValue> pollWithRetry(
    applicationState: ApplicationState,
    topic: String,
    kafkaConsumer: KafkaConsumer<String, ConsumerRecordValue>,
    kafkaConsumerService: KafkaConsumerService<ConsumerRecordValue>,
) {
    var consecutiveErrors = 0
    while (applicationState.ready) {
        try {
            if (kafkaConsumer.subscription().isEmpty()) {
                kafkaConsumer.subscribe(listOf(topic))
            }
            kafkaConsumerService.pollAndProcessRecords(kafkaConsumer)
            consecutiveErrors = 0
        } catch (ex: CancellationException) {
            throw ex
        } catch (ex: Exception) {
            consecutiveErrors++
            val delayMs = minOf(consecutiveErrors * 2000L, 120_000L)
            val message =
                "Exception in kafka consumer for topic $topic (consecutive errors: $consecutiveErrors). Retrying after ${delayMs}ms."
            if (consecutiveErrors == 1) {
                kafkaTaskLog.warn(message, ex)
            } else {
                kafkaTaskLog.error(message, ex)
            }
            // poll() has already advanced the consumer position past the failed records. Unsubscribing discards
            // the position, so the consumer resumes from the last committed offsets when it subscribes again.
            kafkaConsumer.unsubscribe()
            if (applicationState.ready) {
                delay(delayMs.milliseconds)
            }
        }
    }
}
