package no.nav.syfo.pdlpersonhendelse.kafka

import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.Environment
import no.nav.syfo.application.database.database
import no.nav.syfo.kafka.launchKafkaTask

const val PDL_LEESAH_TOPIC = "aapen-person-pdl-leesah-v1"

fun launchKafkaTaskPersonhendelse(
    applicationState: ApplicationState,
    environment: Environment,
) {

    val kafkaPersonhendelseConsumerService = KafkaPersonhendelseConsumerService(
        database = database,
    )

    val consumerProperties = kafkaPersonhendelseConsumerConfig(
        kafkaEnvironment = environment.kafka,
    )

    launchKafkaTask(
        applicationState = applicationState,
        topic = PDL_LEESAH_TOPIC,
        consumerProperties = consumerProperties,
        kafkaConsumerService = kafkaPersonhendelseConsumerService,
    )
}
