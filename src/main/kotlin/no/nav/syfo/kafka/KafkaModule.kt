package no.nav.syfo.kafka

import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.Environment
import no.nav.syfo.dialogmotekandidat.kafka.launchKafkaTaskDialogmotekandidatEndring
import no.nav.syfo.dialogmotestatusendring.kafka.launchKafkaTaskDialogmoteStatusendring
import no.nav.syfo.oppfolgingstilfelle.kafka.launchKafkaTaskOppfolgingstilfellePerson
import no.nav.syfo.personoppgavehendelse.kafka.launchKafkaTaskPersonoppgavehendelse
import no.nav.syfo.personstatus.kafka.launchOversiktHendelseKafkaTask

fun launchKafkaModule(
    applicationState: ApplicationState,
    environment: Environment,
) {
    launchOversiktHendelseKafkaTask(
        applicationState = applicationState,
        environment = environment,
    )
    if (environment.kafkaPersonoppgavehendelseProcessingEnabled) {
        launchKafkaTaskPersonoppgavehendelse(
            applicationState = applicationState,
            kafkaEnvironment = environment.kafka,
        )
    }
    if (environment.kafkaOppfolgingstilfellePersonProcessingEnabled) {
        launchKafkaTaskOppfolgingstilfellePerson(
            applicationState = applicationState,
            kafkaEnvironment = environment.kafka,
        )
    }
    if (environment.kafkaDialogmotekandidatProcessingEnabled) {
        launchKafkaTaskDialogmotekandidatEndring(
            applicationState = applicationState,
            kafkaEnvironment = environment.kafka,
        )
    }
    if (environment.kafkaDialogmoteStatusendringProcessingEnabled) {
        launchKafkaTaskDialogmoteStatusendring(
            applicationState = applicationState,
            kafkaEnvironment = environment.kafka,
        )
    }
}
