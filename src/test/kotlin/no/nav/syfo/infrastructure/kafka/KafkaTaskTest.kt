package no.nav.syfo.infrastructure.kafka

import io.mockk.every
import io.mockk.mockk
import io.mockk.verifyOrder
import kotlinx.coroutines.runBlocking
import no.nav.syfo.ApplicationState
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class KafkaTaskTest {

    private val topic = "topic"

    @Test
    fun `pollWithRetry unsubscribes on error and subscribes again so records are retried from committed offset`() {
        val applicationState = ApplicationState(alive = true, ready = true)
        val subscription = mutableSetOf<String>()
        val kafkaConsumer = mockk<KafkaConsumer<String, String>>(relaxed = true)
        every { kafkaConsumer.subscription() } answers { subscription.toSet() }
        every { kafkaConsumer.subscribe(any<Collection<String>>()) } answers { subscription.addAll(firstArg()) }
        every { kafkaConsumer.unsubscribe() } answers { subscription.clear() }

        var calls = 0
        val kafkaConsumerService = object : KafkaConsumerService<String> {
            override val pollDurationInMillis: Long = 1000
            override suspend fun pollAndProcessRecords(kafkaConsumer: KafkaConsumer<String, String>) {
                calls++
                if (calls == 1) throw RuntimeException("Processing failed")
                applicationState.ready = false
            }
        }

        runBlocking {
            pollWithRetry(applicationState, topic, kafkaConsumer, kafkaConsumerService)
        }

        assertEquals(2, calls)
        verifyOrder {
            kafkaConsumer.subscribe(listOf(topic))
            kafkaConsumer.unsubscribe()
            kafkaConsumer.subscribe(listOf(topic))
        }
    }
}
