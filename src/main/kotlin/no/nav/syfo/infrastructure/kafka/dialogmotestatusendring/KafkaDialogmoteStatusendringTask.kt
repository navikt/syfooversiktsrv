package no.nav.syfo.infrastructure.kafka.dialogmotestatusendring

import no.nav.syfo.ApplicationState
import no.nav.syfo.application.IPersonOversiktStatusRepository
import no.nav.syfo.application.ITransactionManager
import no.nav.syfo.infrastructure.kafka.KafkaEnvironment
import no.nav.syfo.infrastructure.kafka.launchKafkaTask

const val DIALOGMOTE_STATUSENDRING_TOPIC = "teamsykefravr.isdialogmote-dialogmote-statusendring"

fun launchKafkaTaskDialogmoteStatusendring(
    applicationState: ApplicationState,
    kafkaEnvironment: KafkaEnvironment,
    personOversiktStatusRepository: IPersonOversiktStatusRepository,
    transactionManager: ITransactionManager,
) {
    val dialogmoteStatusendringConsumer = DialogmoteStatusendringConsumer(
        transactionManager = transactionManager,
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
