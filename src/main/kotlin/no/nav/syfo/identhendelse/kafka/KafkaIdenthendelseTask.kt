package no.nav.syfo.identhendelse.kafka

import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.Environment
import no.nav.syfo.application.database.database
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.client.pdl.PdlClient
import no.nav.syfo.identhendelse.IdenthendelseService
import no.nav.syfo.kafka.launchKafkaTask

const val PDL_AKTOR_TOPIC = "pdl.aktor-v2"

fun launchKafkaTaskIdenthendelse(
    applicationState: ApplicationState,
    environment: Environment,
    azureAdClient: AzureAdClient,
) {
    val pdlClient = PdlClient(
        azureAdClient = azureAdClient,
        clientEnvironment = environment.clients.pdl,
    )

    val identhendelseService = IdenthendelseService(
        database = database,
        pdlClient = pdlClient,
    )

    val kafkaIdenthendelseConsumerService = IdenthendelseConsumerService(
        identhendelseService = identhendelseService,
    )

    val consumerProperties = kafkaIdenthendelseConsumerConfig(
        kafkaEnvironment = environment.kafka,
    )

    launchKafkaTask(
        applicationState = applicationState,
        topic = PDL_AKTOR_TOPIC,
        consumerProperties = consumerProperties,
        kafkaConsumerService = kafkaIdenthendelseConsumerService,
    )
}
