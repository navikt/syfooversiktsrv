package no.nav.syfo.personstatus.infrastructure.kafka

import no.nav.syfo.aktivitetskravvurdering.kafka.launchKafkaTaskAktivitetskravVurdering
import no.nav.syfo.ApplicationState
import no.nav.syfo.Environment
import no.nav.syfo.personstatus.infrastructure.clients.azuread.AzureAdClient
import no.nav.syfo.personstatus.application.PersonBehandlendeEnhetService
import no.nav.syfo.dialogmotekandidat.kafka.launchKafkaTaskDialogmotekandidatEndring
import no.nav.syfo.dialogmotestatusendring.kafka.launchKafkaTaskDialogmoteStatusendring
import no.nav.syfo.frisktilarbeid.kafka.launchKafkaTaskFriskTilArbeidVedtak
import no.nav.syfo.identhendelse.kafka.launchKafkaTaskIdenthendelse
import no.nav.syfo.oppfolgingstilfelle.kafka.launchKafkaTaskOppfolgingstilfellePerson
import no.nav.syfo.pdlpersonhendelse.kafka.launchKafkaTaskPersonhendelse
import no.nav.syfo.personoppgavehendelse.kafka.launchKafkaTaskPersonoppgavehendelse
import no.nav.syfo.personstatus.application.PersonoversiktStatusService
import no.nav.syfo.personstatus.application.OppfolgingstilfelleService
import no.nav.syfo.personstatus.infrastructure.kafka.manglendemedvirkning.ManglendeMedvirkningVurderingConsumer
import no.nav.syfo.personstatus.infrastructure.kafka.meroppfolging.SenOppfolgingKandidatStatusConsumer
import no.nav.syfo.oppfolgingsoppgave.kafka.launchOppfolgingsoppgaveConsumer

fun launchKafkaModule(
    applicationState: ApplicationState,
    environment: Environment,
    azureAdClient: AzureAdClient,
    personoversiktStatusService: PersonoversiktStatusService,
    personBehandlendeEnhetService: PersonBehandlendeEnhetService,
    oppfolgingstilfelleService: OppfolgingstilfelleService,
) {
    launchKafkaTaskPersonoppgavehendelse(
        applicationState = applicationState,
        kafkaEnvironment = environment.kafka,
        personoversiktStatusService = personoversiktStatusService,
    )
    launchKafkaTaskOppfolgingstilfellePerson(
        applicationState = applicationState,
        kafkaEnvironment = environment.kafka,
        oppfolgingstilfelleService = oppfolgingstilfelleService,
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
        personoversiktStatusService = personoversiktStatusService,
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

    launchOppfolgingsoppgaveConsumer(
        applicationState = applicationState,
        kafkaEnvironment = environment.kafka,
        personBehandlendeEnhetService = personBehandlendeEnhetService,
    )
    launchKafkaTaskFriskTilArbeidVedtak(
        applicationState = applicationState,
        kafkaEnvironment = environment.kafka,
    )
    ArbeidsuforhetvurderingConsumer(personoversiktStatusService = personoversiktStatusService)
        .start(applicationState = applicationState, kafkaEnvironment = environment.kafka)

    SenOppfolgingKandidatStatusConsumer(personoversiktStatusService = personoversiktStatusService)
        .start(
            applicationState = applicationState,
            kafkaEnvironment = environment.kafka,
        )

    ManglendeMedvirkningVurderingConsumer(personoversiktStatusService = personoversiktStatusService)
        .start(
            applicationState = applicationState,
            kafkaEnvironment = environment.kafka,
        )
}
