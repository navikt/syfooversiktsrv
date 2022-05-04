package no.nav.syfo.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.syfo.personstatus.domain.KOversikthendelse
import no.nav.syfo.testutil.ExternalMockEnvironment
import no.nav.syfo.testutil.generator.generateOversikthendelse
import no.nav.syfo.util.configuredJacksonMapper
import org.amshove.kluent.shouldBeEqualTo
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.*
import org.apache.kafka.common.serialization.StringSerializer
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.Duration
import java.util.*

private val objectMapper: ObjectMapper = configuredJacksonMapper()

object KafkaITSpek : Spek({
    val oversiktHendelseTopic = "aapen-syfo-oversikthendelse-v1"
    val externalMockEnvironment = ExternalMockEnvironment.instance

    fun Properties.overrideForTest(): Properties = apply {
        remove("security.protocol")
        remove("sasl.mechanism")
    }

    val consumerProperties = kafkaConsumerConfig(
        environment = externalMockEnvironment.environment,
    ).overrideForTest()
    val consumer = KafkaConsumer<String, String>(consumerProperties)

    val producerProperties = consumerProperties.apply {
        this[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java.canonicalName
        this[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = JacksonKafkaSerializer::class.java.canonicalName
    }
    val producer = KafkaProducer<String, KOversikthendelse>(producerProperties)

    consumer.subscribe(listOf(oversiktHendelseTopic))

    describe("Produce and consume messages from topic") {
        it("Topic $oversiktHendelseTopic") {
            val oversikthendelse = generateOversikthendelse.copy()
            producer.send(ProducerRecord(oversiktHendelseTopic, oversikthendelse))

            val messages: ArrayList<KOversikthendelse> = arrayListOf()
            consumer.poll(Duration.ofMillis(5000)).forEach {
                val hendelse: KOversikthendelse = objectMapper.readValue(it.value())
                messages.add(hendelse)
            }
            messages.size shouldBeEqualTo 1
            messages.first() shouldBeEqualTo oversikthendelse
        }
    }
})
