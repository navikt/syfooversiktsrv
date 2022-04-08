package no.nav.syfo.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.netty.util.internal.StringUtil
import kotlinx.coroutines.delay
import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.application.*
import no.nav.syfo.application.backgroundtask.launchBackgroundTask
import no.nav.syfo.application.database.database
import no.nav.syfo.oversikthendelsetilfelle.OversikthendelstilfelleService
import no.nav.syfo.oversikthendelsetilfelle.domain.KOversikthendelsetilfelle
import no.nav.syfo.personstatus.OversiktHendelseService
import no.nav.syfo.personstatus.domain.KOversikthendelse
import no.nav.syfo.util.*
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*

private val objectMapper: ObjectMapper = configuredJacksonMapper()

private val log: Logger = LoggerFactory.getLogger("no.nav.syfo.Kafka")

fun launchKafkaTask(
    applicationState: ApplicationState,
    environment: Environment,
) {
    log.info("Setting up kafka consumer")

    val oversiktHendelseService = OversiktHendelseService(
        database = database,
    )
    val oversikthendelstilfelleService = OversikthendelstilfelleService(
        database = database,
    )

    // Kafka
    val consumerProperties = kafkaConsumerConfig(
        environment = environment,
    )

    launchListeners(
        applicationState = applicationState,
        environment = environment,
        consumerProperties = consumerProperties,
        oversiktHendelseService = oversiktHendelseService,
        oversikthendelstilfelleService = oversikthendelstilfelleService,
    )
}

fun launchListeners(
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
}

suspend fun blockingApplicationLogic(
    applicationState: ApplicationState,
    kafkaConsumer: KafkaConsumer<String, String>,
    oversiktHendelseService: OversiktHendelseService,
) {
    while (applicationState.ready) {
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
            log.info("Mottatt oversikthendelse, klar for oppdatering, $logKeys, {}", *logValues, callIdArgument(callId))

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
    while (applicationState.ready) {
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
