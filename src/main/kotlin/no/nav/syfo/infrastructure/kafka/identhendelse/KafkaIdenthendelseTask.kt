package no.nav.syfo.infrastructure.kafka.identhendelse

import no.nav.syfo.ApplicationState
import no.nav.syfo.Environment
import no.nav.syfo.application.IPersonOversiktStatusRepository
import no.nav.syfo.application.IdenthendelseService
import no.nav.syfo.infrastructure.clients.azuread.AzureAdClient
import no.nav.syfo.infrastructure.clients.pdl.PdlClient
import no.nav.syfo.infrastructure.database.database
import no.nav.syfo.infrastructure.kafka.launchKafkaTask

const val PDL_AKTOR_TOPIC = "pdl.aktor-v2"

// Dryrun: leser hele topicet på nytt og logger hvilke forekomster som ville blitt endret.
// Ved reell kjøring: sett til false og bytt group id i KafkaIdenthendelseConsumerConfig.
const val IDENTHENDELSE_DRY_RUN = true

fun launchKafkaTaskIdenthendelse(
    applicationState: ApplicationState,
    environment: Environment,
    azureAdClient: AzureAdClient,
    personOversiktStatusRepository: IPersonOversiktStatusRepository,
) {
    val pdlClient = PdlClient(
        azureAdClient = azureAdClient,
        clientEnvironment = environment.clients.pdl,
    )

    val identhendelseService = IdenthendelseService(
        database = database,
        pdlClient = pdlClient,
        personOversiktStatusRepository = personOversiktStatusRepository,
        dryRun = IDENTHENDELSE_DRY_RUN,
    )

    val kafkaIdenthendelseConsumerService = IdenthendelseConsumerService(
        identhendelseService = identhendelseService,
        dryRun = IDENTHENDELSE_DRY_RUN,
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
