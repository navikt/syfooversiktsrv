package no.nav.syfo.dialogmotekandidat.kafka

import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.backgroundtask.launchBackgroundTask
import no.nav.syfo.application.kafka.KafkaEnvironment
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val log: Logger = LoggerFactory.getLogger("no.nav.syfo")

const val DIALOGMOTEKANDIDAT_TOPIC = "teamsykefravr.isdialogmotekandidat-dialogmotekandidat"

fun launchKafkaTaskDialogmotekandidatEndring(
    applicationState: ApplicationState,
    kafkaEnvironment: KafkaEnvironment,
    kafkaDialogmotekandidatEndringService: KafkaDialogmotekandidatEndringService,
) {
    launchBackgroundTask(
        applicationState = applicationState,
    ) {
        blockingApplicationLogicDialogmotekandidatEndring(
            applicationState = applicationState,
            kafkaEnvironment = kafkaEnvironment,
            kafkaDialogmotekandidatEndringService = kafkaDialogmotekandidatEndringService
        )
    }
}

fun blockingApplicationLogicDialogmotekandidatEndring(
    applicationState: ApplicationState,
    kafkaEnvironment: KafkaEnvironment,
    kafkaDialogmotekandidatEndringService: KafkaDialogmotekandidatEndringService,
) {
    log.info("Setting up kafka consumer for ${KafkaDialogmotekandidatEndring::class.java.simpleName}")

    val kafkaConsumer = KafkaConsumer<String, KafkaDialogmotekandidatEndring>(
        kafkaDialogmotekandidatEndringConsumerConfig(kafkaEnvironment = kafkaEnvironment)
    )

    kafkaConsumer.subscribe(listOf(DIALOGMOTEKANDIDAT_TOPIC))
    while (applicationState.ready) {
        kafkaDialogmotekandidatEndringService.pollAndProcessRecords(
            kafkaConsumer = kafkaConsumer
        )
    }
}
