package no.nav.syfo.personstatus.infrastructure.kafka.personhendelse

import no.nav.syfo.ApplicationState
import no.nav.syfo.Environment
import no.nav.syfo.personstatus.infrastructure.database.database
import no.nav.syfo.personstatus.infrastructure.kafka.launchKafkaTask
import no.nav.syfo.personstatus.application.PdlPersonhendelseService

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
