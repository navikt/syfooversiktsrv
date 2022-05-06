package no.nav.syfo.personstatus.kafka

import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.Environment
import no.nav.syfo.application.database.database
import no.nav.syfo.kafka.*

const val OVERSIKT_HENDELSE_TOPIC = "aapen-syfo-oversikthendelse-v1"

fun launchOversiktHendelseKafkaTask(
    applicationState: ApplicationState,
    environment: Environment,
) {
    val kafkaOversiktHendelseService = KafkaOversiktHendelseService(
        database = database,
    )
    val consumerProperties = kafkaConsumerConfig(
        environment = environment,
    )
    launchKafkaTask(
        applicationState = applicationState,
        topic = OVERSIKT_HENDELSE_TOPIC,
        consumerProperties = consumerProperties,
        kafkaConsumerService = kafkaOversiktHendelseService
    )
}
