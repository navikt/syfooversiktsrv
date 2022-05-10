package no.nav.syfo.dialogmotestatusendring.kafka

import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.database.database
import no.nav.syfo.application.kafka.KafkaEnvironment
import no.nav.syfo.kafka.launchKafkaTask

const val DIALOGMOTE_STATUSENDRING_TOPIC = "teamsykefravr.isdialogmote-dialogmote-statusendring"

fun launchKafkaTaskDialogmoteStatusendring(
    applicationState: ApplicationState,
    kafkaEnvironment: KafkaEnvironment,
) {
    val kafkaDialogmoteStatusendringService = KafkaDialogmoteStatusendringService(
        database = database,
    )
    val consumerProperties = kafkaDialogmoteStatusendringConsumerConfig(
        kafkaEnvironment = kafkaEnvironment,
    )

    launchKafkaTask(
        applicationState = applicationState,
        topic = DIALOGMOTE_STATUSENDRING_TOPIC,
        consumerProperties = consumerProperties,
        kafkaConsumerService = kafkaDialogmoteStatusendringService
    )
}
