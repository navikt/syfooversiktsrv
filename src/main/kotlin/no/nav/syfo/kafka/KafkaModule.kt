package no.nav.syfo.kafka

import no.nav.syfo.aktivitetskravvurdering.kafka.launchKafkaTaskAktivitetskravVurdering
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.Environment
import no.nav.syfo.application.cache.RedisStore
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.dialogmotekandidat.kafka.launchKafkaTaskDialogmotekandidatEndring
import no.nav.syfo.dialogmotestatusendring.kafka.launchKafkaTaskDialogmoteStatusendring
import no.nav.syfo.identhendelse.kafka.launchKafkaTaskIdenthendelse
import no.nav.syfo.oppfolgingstilfelle.kafka.launchKafkaTaskOppfolgingstilfellePerson
import no.nav.syfo.pdlpersonhendelse.kafka.launchKafkaTaskPersonhendelse
import no.nav.syfo.personoppgavehendelse.kafka.launchKafkaTaskPersonoppgavehendelse
import no.nav.syfo.personstatus.kafka.launchOversiktHendelseKafkaTask

fun launchKafkaModule(
    applicationState: ApplicationState,
    environment: Environment,
    redisStore: RedisStore,
    azureAdClient: AzureAdClient,
) {
    launchOversiktHendelseKafkaTask(
        applicationState = applicationState,
        environment = environment,
    )
    launchKafkaTaskPersonoppgavehendelse(
        applicationState = applicationState,
        kafkaEnvironment = environment.kafka,
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

    if (environment.kafkaAktivitetskravVurderingProcessingEnabled) {
        launchKafkaTaskAktivitetskravVurdering(
            applicationState = applicationState,
            kafkaEnvironment = environment.kafka,
        )
    }

    if (environment.kafkaIdenthendelseUpdatesEnabled) {
        launchKafkaTaskIdenthendelse(
            applicationState = applicationState,
            environment = environment,
            redisStore = redisStore,
            azureAdClient = azureAdClient,
        )
    }

    if (environment.kafkaPersonhendelseUpdatesEnabled) {
        launchKafkaTaskPersonhendelse(
            applicationState = applicationState,
            environment = environment,
        )
    }
}
