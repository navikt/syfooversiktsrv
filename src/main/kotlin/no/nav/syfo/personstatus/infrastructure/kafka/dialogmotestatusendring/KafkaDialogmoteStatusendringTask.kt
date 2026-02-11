package no.nav.syfo.personstatus.infrastructure.kafka.dialogmotestatusendring

import no.nav.syfo.ApplicationState
import no.nav.syfo.personstatus.application.IPersonOversiktStatusRepository
import no.nav.syfo.personstatus.infrastructure.database.database
import no.nav.syfo.personstatus.infrastructure.kafka.KafkaEnvironment
import no.nav.syfo.personstatus.infrastructure.kafka.launchKafkaTask

const val DIALOGMOTE_STATUSENDRING_TOPIC = "teamsykefravr.isdialogmote-dialogmote-statusendring"

fun launchKafkaTaskDialogmoteStatusendring(
    applicationState: ApplicationState,
    kafkaEnvironment: KafkaEnvironment,
    personOversiktStatusRepository: IPersonOversiktStatusRepository,
) {
    val dialogmoteStatusendringConsumer = DialogmoteStatusendringConsumer(
        database = database,
        personOversiktStatusRepository = personOversiktStatusRepository,
    )
    val consumerProperties = kafkaDialogmoteStatusendringConsumerConfig(
        kafkaEnvironment = kafkaEnvironment,
    )

    launchKafkaTask(
        applicationState = applicationState,
        topic = DIALOGMOTE_STATUSENDRING_TOPIC,
        consumerProperties = consumerProperties,
        kafkaConsumerService = dialogmoteStatusendringConsumer
    )
}
