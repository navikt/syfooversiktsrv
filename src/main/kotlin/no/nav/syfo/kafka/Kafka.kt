package no.nav.syfo.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.netty.util.internal.StringUtil
import kotlinx.coroutines.delay
import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.Environment
import no.nav.syfo.application.backgroundtask.launchBackgroundTask
import no.nav.syfo.application.database.database
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

    // Kafka
    val consumerProperties = kafkaConsumerConfig(
        environment = environment,
    )

    launchListeners(
        applicationState = applicationState,
        consumerProperties = consumerProperties,
        oversiktHendelseService = oversiktHendelseService,
    )
}

fun launchListeners(
    applicationState: ApplicationState,
    consumerProperties: Properties,
    oversiktHendelseService: OversiktHendelseService,
) {
    val kafkaconsumerOversikthendelse = KafkaConsumer<String, String>(consumerProperties)
    kafkaconsumerOversikthendelse.subscribe(
        listOf(
            "aapen-syfo-oversikthendelse-v1",
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
                StructuredArguments.keyValue("hendelseId", oversiktHendelse.hendelseId)
            )
            log.info("Mottatt oversikthendelse, klar for oppdatering, $logKeys, {}", *logValues, callIdArgument(callId))

            oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelse, callId)
        }
        delay(100)
    }
}
