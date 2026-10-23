package no.nav.syfo.infrastructure.kafka

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.TopicPartition
import org.junit.jupiter.api.Test

class KafkaTaskTest {

    @Test
    fun `seekToCommittedOffsets rewinds to committed offset so failed records are retried`() {
        val partition0 = TopicPartition("topic", 0)
        val partition1 = TopicPartition("topic", 1)
        val kafkaConsumer = mockk<KafkaConsumer<String, String>>(relaxed = true)
        every { kafkaConsumer.assignment() } returns setOf(partition0, partition1)
        every { kafkaConsumer.committed(setOf(partition0, partition1)) } returns mapOf(
            partition0 to OffsetAndMetadata(42),
            partition1 to null,
        )

        kafkaConsumer.seekToCommittedOffsets()

        verify(exactly = 1) { kafkaConsumer.seek(partition0, 42L) }
        verify(exactly = 1) { kafkaConsumer.seekToBeginning(listOf(partition1)) }
    }

    @Test
    fun `seekToCommittedOffsets does nothing when no partitions are assigned`() {
        val kafkaConsumer = mockk<KafkaConsumer<String, String>>(relaxed = true)
        every { kafkaConsumer.assignment() } returns emptySet()

        kafkaConsumer.seekToCommittedOffsets()

        verify(exactly = 0) { kafkaConsumer.committed(any<Set<TopicPartition>>()) }
        verify(exactly = 0) { kafkaConsumer.seek(any<TopicPartition>(), any<Long>()) }
    }
}
