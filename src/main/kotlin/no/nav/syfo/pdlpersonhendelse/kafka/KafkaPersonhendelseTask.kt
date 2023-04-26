package no.nav.syfo.pdlpersonhendelse.kafka

import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.Environment
import no.nav.syfo.application.database.database
import no.nav.syfo.kafka.launchKafkaTask
import no.nav.syfo.pdlpersonhendelse.PdlPersonhendelseService

const val PDL_LEESAH_TOPIC = "pdl.leesah-v1"

fun launchKafkaTaskPersonhendelse(
    applicationState: ApplicationState,
    environment: Environment,
) {
    val pdlPersonhendelseService = PdlPersonhendelseService(
        database = database,
    )

    val kafkaPersonhendelseConsumerService = KafkaPersonhendelseConsumerService(
        pdlPersonhendelseService = pdlPersonhendelseService,
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
