package no.nav.syfo.kafka

import no.nav.syfo.aktivitetskravvurdering.kafka.launchKafkaTaskAktivitetskravVurdering
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.Environment
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.dialogmotekandidat.kafka.launchKafkaTaskDialogmotekandidatEndring
import no.nav.syfo.dialogmotestatusendring.kafka.launchKafkaTaskDialogmoteStatusendring
import no.nav.syfo.huskelapp.kafka.launchHuskelappConsumer
import no.nav.syfo.identhendelse.kafka.launchKafkaTaskIdenthendelse
import no.nav.syfo.oppfolgingstilfelle.kafka.launchKafkaTaskOppfolgingstilfellePerson
import no.nav.syfo.pdlpersonhendelse.kafka.launchKafkaTaskPersonhendelse
import no.nav.syfo.personoppgavehendelse.kafka.launchKafkaTaskPersonoppgavehendelse
import no.nav.syfo.personstatus.PersonoversiktStatusService

fun launchKafkaModule(
    applicationState: ApplicationState,
    environment: Environment,
    azureAdClient: AzureAdClient,
    personoversiktStatusService: PersonoversiktStatusService,
) {
    launchKafkaTaskPersonoppgavehendelse(
        applicationState = applicationState,
        kafkaEnvironment = environment.kafka,
        personoversiktStatusService = personoversiktStatusService,
    )
    launchKafkaTaskOppfolgingstilfellePerson(
        applicationState = applicationState,
        kafkaEnvironment = environment.kafka,
    )
    launchKafkaTaskDialogmotekandidatEndring(
        applicationState = applicationState,
        kafkaEnvironment = environment.kafka,
    )
    launchKafkaTaskDialogmoteStatusendring(
        applicationState = applicationState,
        kafkaEnvironment = environment.kafka,
    )

    launchKafkaTaskAktivitetskravVurdering(
        applicationState = applicationState,
        kafkaEnvironment = environment.kafka,
    )

    launchKafkaTaskIdenthendelse(
        applicationState = applicationState,
        environment = environment,
        azureAdClient = azureAdClient,
    )

    launchKafkaTaskPersonhendelse(
        applicationState = applicationState,
        environment = environment,
    )

    launchHuskelappConsumer(
        applicationState = applicationState,
        kafkaEnvironment = environment.kafka,
    )
}
