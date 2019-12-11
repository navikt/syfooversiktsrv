package no.nav.syfo.kafka

import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.util.KtorExperimentalAPI
import io.netty.util.internal.StringUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.*
import no.nav.syfo.oversikthendelsetilfelle.OversikthendelstilfelleService
import no.nav.syfo.oversikthendelsetilfelle.domain.KOversikthendelsetilfelle
import no.nav.syfo.personstatus.OversiktHendelseService
import no.nav.syfo.personstatus.domain.KOversikthendelse
import no.nav.syfo.util.CallIdArgument
import no.nav.syfo.util.kafkaCallId
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*

private val objectMapper: ObjectMapper = ObjectMapper().apply {
    registerKotlinModule()
    registerModule(JavaTimeModule())
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
}


private val LOG: Logger = LoggerFactory.getLogger("no.nav.syfo.Kafka")

suspend fun CoroutineScope.setupKafka(
        vaultSecrets: KafkaCredentials,
        oversiktHendelseService: OversiktHendelseService,
        oversikthendelstilfelleService: OversikthendelstilfelleService
) {

    LOG.info("Setting up kafka consumer")

    // Kafka
    val kafkaBaseConfig = loadBaseConfig(env, vaultSecrets)
            .envOverrides()
    val consumerProperties = kafkaBaseConfig.toConsumerConfig(
            "${env.applicationName}-consumer", valueDeserializer = StringDeserializer::class
    )

    launchListeners(
            consumerProperties,
            state,
            oversiktHendelseService,
            oversikthendelstilfelleService
    )
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
            val callId = kafkaCallId()
            val oversiktHendelse: KOversikthendelse =
                    objectMapper.readValue(it.value())
            logValues = arrayOf(
                    StructuredArguments.keyValue("oversikthendelseId", it.key()),
                    StructuredArguments.keyValue("harFnr", (!StringUtil.isNullOrEmpty(oversiktHendelse.fnr)).toString()),
                    StructuredArguments.keyValue("enhetId", oversiktHendelse.enhetId),
                    StructuredArguments.keyValue("hendelseId", oversiktHendelse.hendelseId)
            )
            LOG.info("Mottatt oversikthendelse, klar for oppdatering, $logKeys, {}", *logValues, CallIdArgument(callId))

            oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelse, callId)
        }
        delay(100)
    }
}

@KtorExperimentalAPI
suspend fun blockingApplicationLogic(
        applicationState: ApplicationState,
        kafkaConsumer: KafkaConsumer<String, String>,
        oversikthendelstilfelleService: OversikthendelstilfelleService
) {
    while (applicationState.running) {
        var logValues = arrayOf(
                StructuredArguments.keyValue("oversikthendelsetilfelleId", "missing"),
                StructuredArguments.keyValue("harFnr", "missing"),
                StructuredArguments.keyValue("navn", "missing"),
                StructuredArguments.keyValue("enhetId", "missing"),
                StructuredArguments.keyValue("virksomhetsnummer", "missing"),
                StructuredArguments.keyValue("gradert", "missing"),
                StructuredArguments.keyValue("fom", "missing"),
                StructuredArguments.keyValue("tom", "missing"),
                StructuredArguments.keyValue("virksomhetsnavn", "missing")
        )

        val logKeys = logValues.joinToString(prefix = "(", postfix = ")", separator = ",") {
            "{}"
        }

        kafkaConsumer.poll(Duration.ofMillis(0)).forEach {
            val callId = kafkaCallId()
            val oversikthendelsetilfelle: KOversikthendelsetilfelle =
                    objectMapper.readValue(it.value())
            logValues = arrayOf(
                    StructuredArguments.keyValue("oversikthendelsetilfelleId", it.key()),
                    StructuredArguments.keyValue("harFnr", (!StringUtil.isNullOrEmpty(oversikthendelsetilfelle.fnr)).toString()),
                    StructuredArguments.keyValue("navn", oversikthendelsetilfelle.navn),
                    StructuredArguments.keyValue("enhetId", oversikthendelsetilfelle.enhetId),
                    StructuredArguments.keyValue("virksomhetsnummer", oversikthendelsetilfelle.virksomhetsnummer),
                    StructuredArguments.keyValue("gradert", oversikthendelsetilfelle.gradert),
                    StructuredArguments.keyValue("fom", oversikthendelsetilfelle.fom),
                    StructuredArguments.keyValue("tom", oversikthendelsetilfelle.tom),
                    StructuredArguments.keyValue("virksomhetsnavn", oversikthendelsetilfelle.virksomhetsnavn)
            )
            LOG.info("Mottatt oversikthendelsetilfelle, klar for oppdatering, $logKeys, {}", *logValues, CallIdArgument(callId))

            oversikthendelstilfelleService.oppdaterPersonMedHendelse(oversikthendelsetilfelle, callId)
        }
        delay(100)
    }
}

@KtorExperimentalAPI
suspend fun CoroutineScope.launchListeners(
        consumerProperties: Properties,
        applicationState: ApplicationState,
        oversiktHendelseService: OversiktHendelseService,
        oversikthendelstilfelleService: OversikthendelstilfelleService
) {

    val kafkaconsumerOversikthendelse = KafkaConsumer<String, String>(consumerProperties)
    kafkaconsumerOversikthendelse.subscribe(
            listOf(
                    "aapen-syfo-oversikthendelse-v1"
            )
    )

    val kafkaconsumerTilfelle = KafkaConsumer<String, String>(consumerProperties)
    kafkaconsumerTilfelle.subscribe(
            listOf(
                    env.oversikthendelseOppfolgingstilfelleTopic
            )
    )

    createListener(applicationState) {
        blockingApplicationLogic(
                applicationState,
                kafkaconsumerOversikthendelse,
                oversiktHendelseService
        )
    }

    createListener(applicationState) {
        blockingApplicationLogic(
                applicationState,
                kafkaconsumerTilfelle,
                oversikthendelstilfelleService
        )
    }


    applicationState.initialized = true
}
