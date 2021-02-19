package no.nav.syfo.kafka

import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.common.KafkaEnvironment
import no.nav.syfo.Environment
import no.nav.syfo.VaultSecrets
import no.nav.syfo.personstatus.domain.KOversikthendelse
import no.nav.syfo.testutil.generator.generateOversikthendelse
import org.amshove.kluent.shouldBeEqualTo
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.net.ServerSocket
import java.time.Duration
import java.util.*

private val objectMapper: ObjectMapper = ObjectMapper().apply {
    registerKotlinModule()
    registerModule(JavaTimeModule())
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
}

object KafkaITSpek : Spek({
    fun getRandomPort() = ServerSocket(0).use {
        it.localPort
    }

    val oversiktHendelseTopic = "aapen-syfo-oversikthendelse-v1"

    val embeddedEnvironment = KafkaEnvironment(
            autoStart = false,
            topics = listOf(oversiktHendelseTopic)
    )

    val credentials = VaultSecrets(
            "",
            ""
    )
    val env = Environment(
            applicationPort = getRandomPort(),
            applicationThreads = 1,
            oversikthendelseOppfolgingstilfelleTopic = "topic1",
            kafkaBootstrapServers = embeddedEnvironment.brokersURL,
            syfooversiktsrvDBURL = "12314.adeo.no",
            mountPathVault = "vault.adeo.no",
            databaseName = "syfooversiktsrv",
            applicationName = "syfooversiktsrv",
            jwkKeysUrl = "",
            jwtIssuer = "",
            aadDiscoveryUrl = "",
            syfotilgangskontrollUrl = "",
            clientid = "",
            syfoveilederUrl = ""
    )

    fun Properties.overrideForTest(): Properties = apply {
        remove("security.protocol")
        remove("sasl.mechanism")
    }

    val baseConfig = loadBaseConfig(env, credentials).overrideForTest()

    val producerProperties = baseConfig
            .toProducerConfig("spek.integration", valueSerializer = JacksonKafkaSerializer::class)
    val producer = KafkaProducer<String, KOversikthendelse>(producerProperties)

    val consumerProperties = baseConfig
            .toConsumerConfig("spek.integration-consumer", valueDeserializer = StringDeserializer::class)
    val consumer = KafkaConsumer<String, String>(consumerProperties)

    consumer.subscribe(listOf(oversiktHendelseTopic))

    beforeGroup {
        embeddedEnvironment.start()
    }

    afterGroup {
        embeddedEnvironment.tearDown()
    }

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
