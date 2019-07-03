package no.nav.syfo.kafka

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.util.KtorExperimentalAPI
import io.netty.util.internal.StringUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.*
import no.nav.syfo.metric.COUNT_OVERSIKTHENDELSE_MOTEBEHOVSSVAR_MOTTATT
import no.nav.syfo.personstatus.OversiktHendelseService
import no.nav.syfo.personstatus.domain.KOversikthendelse
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import java.time.Duration
import java.util.*

private val objectMapper: ObjectMapper = ObjectMapper().apply {
    registerKotlinModule()
    registerModule(JavaTimeModule())
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
}


fun setupKafka(vaultSecrets: KafkaCredentials, oversiktHendelseService: OversiktHendelseService) = runBlocking {

    // Kafka
    val kafkaBaseConfig = loadBaseConfig(env, vaultSecrets)
            .envOverrides()
    val consumerProperties = kafkaBaseConfig.toConsumerConfig(
            "${env.applicationName}-consumer", valueDeserializer = StringDeserializer::class
    )

    launchListeners(consumerProperties, state, oversiktHendelseService)
}


@KtorExperimentalAPI
suspend fun blockingApplicationLogic(
        applicationState: ApplicationState,
        kafkaConsumer: KafkaConsumer<String, String>,
        oversiktHendelseService: OversiktHendelseService
) {
    while (applicationState.running) {
        var logValues = arrayOf(
                StructuredArguments.keyValue("oversikthendelseId", "missing"),
                StructuredArguments.keyValue("Harfnr", "missing"),
                StructuredArguments.keyValue("enhetId", "missing"),
                StructuredArguments.keyValue("hendelseId", "missing")
        )

        val logKeys = logValues.joinToString(prefix = "(", postfix = ")", separator = ",") {
            "{}"
        }

        kafkaConsumer.poll(Duration.ofMillis(0)).forEach {
            val oversiktHendelse: KOversikthendelse =
                    objectMapper.readValue(it.value())
            logValues = arrayOf(
                    StructuredArguments.keyValue("oversikthendelseId", it.key()),
                    StructuredArguments.keyValue("harFnr", (!StringUtil.isNullOrEmpty(oversiktHendelse.fnr)).toString()),
                    StructuredArguments.keyValue("enhetId", oversiktHendelse.enhetId),
                    StructuredArguments.keyValue("hendelseId", oversiktHendelse.hendelseId)
            )
            LOG.info("Mottatt oversikthendelse, klar for oppdatering, $logKeys", *logValues)

            oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelse)

            COUNT_OVERSIKTHENDELSE_MOTEBEHOVSSVAR_MOTTATT.inc()
        }
        delay(100)
    }
}

@KtorExperimentalAPI
suspend fun CoroutineScope.launchListeners(
        consumerProperties: Properties,
        applicationState: ApplicationState,
        oversiktHendelseService: OversiktHendelseService
) {

    val kafkaconsumerOppgave = KafkaConsumer<String, String>(consumerProperties)

    kafkaconsumerOppgave.subscribe(
            listOf("aapen-syfo-oversikthendelse-v1")
    )
    createListener(applicationState) {
        blockingApplicationLogic(applicationState, kafkaconsumerOppgave, oversiktHendelseService)
    }


    launchListeners(consumerProperties, applicationState, oversiktHendelseService)

    applicationState.initialized = true
}