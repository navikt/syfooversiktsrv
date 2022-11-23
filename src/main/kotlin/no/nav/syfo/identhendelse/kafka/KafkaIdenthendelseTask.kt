package no.nav.syfo.identhendelse.kafka

import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.Environment
import no.nav.syfo.application.cache.RedisStore
import no.nav.syfo.application.database.database
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.client.pdl.PdlClient
import no.nav.syfo.identhendelse.IdenthendelseService
import no.nav.syfo.kafka.launchKafkaTask

const val PDL_AKTOR_TOPIC = "pdl.aktor-v2"

fun launchKafkaTaskIdenthendelse(
    applicationState: ApplicationState,
    environment: Environment,
) {
    val redisStore = RedisStore(
        redisEnvironment = environment.redis,
    )

    val azureAdClient = AzureAdClient(
        azureEnviroment = environment.azure,
        redisStore = redisStore,
    )

    val pdlClient = PdlClient(
        azureAdClient = azureAdClient,
        clientEnvironment = environment.clients.pdl,
        redisStore = redisStore,
    )

    val identhendelseService = IdenthendelseService(
        database = database,
        pdlClient = pdlClient,
    )

    val kafkaIdenthendelseConsumerService = IdenthendelseConsumerService(
        applicationState = applicationState,
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
