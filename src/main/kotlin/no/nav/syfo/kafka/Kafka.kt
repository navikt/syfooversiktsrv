package no.nav.syfo.kafka

import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.application.*
import io.netty.util.internal.StringUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.*
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.launchBackgroundTask
import no.nav.syfo.oversikthendelsetilfelle.OversikthendelstilfelleService
import no.nav.syfo.oversikthendelsetilfelle.domain.KOversikthendelsetilfelle
import no.nav.syfo.personstatus.OversiktHendelseService
import no.nav.syfo.personstatus.domain.KOversikthendelse
import no.nav.syfo.util.*
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

fun Application.kafkaModule(
    applicationState: ApplicationState,
    environment: Environment,
) {
    val oversiktHendelseService = OversiktHendelseService(
        database = database,
    )
    val oversikthendelstilfelleService = OversikthendelstilfelleService(
        database = database,
    )

    launch(backgroundTasksContext) {
        val vaultSecrets = VaultSecrets(
            serviceuserPassword = getFileAsString("/secrets/serviceuser/syfooversiktsrv/password"),
            serviceuserUsername = getFileAsString("/secrets/serviceuser/syfooversiktsrv/username"),
        )
        setupKafka(
            applicationState = applicationState,
            environment = environment,
            oversiktHendelseService = oversiktHendelseService,
            oversikthendelstilfelleService = oversikthendelstilfelleService,
            vaultSecrets = vaultSecrets,
        )
    }
}

suspend fun setupKafka(
    applicationState: ApplicationState,
    environment: Environment,
    oversiktHendelseService: OversiktHendelseService,
    oversikthendelstilfelleService: OversikthendelstilfelleService,
    vaultSecrets: KafkaCredentials,
) {
    LOG.info("Setting up kafka consumer")

    // Kafka
    val kafkaBaseConfig = loadBaseConfig(environment, vaultSecrets)
        .envOverrides()
    val consumerProperties = kafkaBaseConfig.toConsumerConfig(
        "${environment.applicationName}-consumer", valueDeserializer = StringDeserializer::class
    )

    launchListeners(
        applicationState = applicationState,
        environment = environment,
        consumerProperties = consumerProperties,
        oversiktHendelseService = oversiktHendelseService,
        oversikthendelstilfelleService = oversikthendelstilfelleService,
    )
}

suspend fun blockingApplicationLogic(
    applicationState: ApplicationState,
    kafkaConsumer: KafkaConsumer<String, String>,
    oversiktHendelseService: OversiktHendelseService,
) {
    while (applicationState.alive) {
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
            LOG.info("Mottatt oversikthendelse, klar for oppdatering, $logKeys, {}", *logValues, callIdArgument(callId))

            oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelse, callId)
        }
        delay(100)
    }
}

suspend fun blockingApplicationLogic(
    applicationState: ApplicationState,
    kafkaConsumer: KafkaConsumer<String, String>,
    oversikthendelstilfelleService: OversikthendelstilfelleService,
) {
    while (applicationState.alive) {
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
                StructuredArguments.keyValue(
                    "harFnr",
                    (!StringUtil.isNullOrEmpty(oversikthendelsetilfelle.fnr)).toString()
                ),
                StructuredArguments.keyValue("navn", oversikthendelsetilfelle.navn),
                StructuredArguments.keyValue("enhetId", oversikthendelsetilfelle.enhetId),
                StructuredArguments.keyValue("virksomhetsnummer", oversikthendelsetilfelle.virksomhetsnummer),
                StructuredArguments.keyValue("gradert", oversikthendelsetilfelle.gradert),
                StructuredArguments.keyValue("fom", oversikthendelsetilfelle.fom),
                StructuredArguments.keyValue("tom", oversikthendelsetilfelle.tom),
                StructuredArguments.keyValue("virksomhetsnavn", oversikthendelsetilfelle.virksomhetsnavn)
            )

            oversikthendelstilfelleService.oppdaterPersonMedHendelse(oversikthendelsetilfelle, callId)
        }
        delay(100)
    }
}

suspend fun launchListeners(
    applicationState: ApplicationState,
    environment: Environment,
    consumerProperties: Properties,
    oversiktHendelseService: OversiktHendelseService,
    oversikthendelstilfelleService: OversikthendelstilfelleService,
) {

    val kafkaconsumerOversikthendelse = KafkaConsumer<String, String>(consumerProperties)
    kafkaconsumerOversikthendelse.subscribe(
        listOf(
            "aapen-syfo-oversikthendelse-v1",
        )
    )

    val kafkaconsumerTilfelle = KafkaConsumer<String, String>(consumerProperties)
    kafkaconsumerTilfelle.subscribe(
        listOf(
            environment.oversikthendelseOppfolgingstilfelleTopic,
        )
    )

    launchBackgroundTask(
        applicationState = applicationState,
    ) {
        blockingApplicationLogic(
            applicationState,
            kafkaconsumerOversikthendelse,
            oversiktHendelseService,
        )
    }

    launchBackgroundTask(
        applicationState = applicationState,
    ) {
        blockingApplicationLogic(
            applicationState,
            kafkaconsumerTilfelle,
            oversikthendelstilfelleService,
        )
    }

    applicationState.ready = true
}
