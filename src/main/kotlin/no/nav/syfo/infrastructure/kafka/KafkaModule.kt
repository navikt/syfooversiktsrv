package no.nav.syfo.infrastructure.kafka

import no.nav.syfo.ApplicationState
import no.nav.syfo.infrastructure.kafka.aktivitetskrav.launchKafkaTaskAktivitetskravVurdering
import no.nav.syfo.Environment
import no.nav.syfo.application.IPersonOversiktStatusRepository
import no.nav.syfo.application.ITransactionManager
import no.nav.syfo.infrastructure.clients.azuread.AzureAdClient
import no.nav.syfo.application.PersonBehandlendeEnhetService
import no.nav.syfo.infrastructure.kafka.dialogmotekandidat.launchKafkaTaskDialogmotekandidatEndring
import no.nav.syfo.infrastructure.kafka.dialogmotestatusendring.launchKafkaTaskDialogmoteStatusendring
import no.nav.syfo.infrastructure.kafka.frisktilarbeid.launchKafkaTaskFriskTilArbeidVedtak
import no.nav.syfo.infrastructure.kafka.identhendelse.launchKafkaTaskIdenthendelse
import no.nav.syfo.infrastructure.kafka.oppfolgingstilfelle.launchKafkaTaskOppfolgingstilfellePerson
import no.nav.syfo.infrastructure.kafka.personhendelse.launchKafkaTaskPersonhendelse
import no.nav.syfo.infrastructure.kafka.personoppgavehendelse.launchKafkaTaskPersonoppgavehendelse
import no.nav.syfo.application.PersonoversiktStatusService
import no.nav.syfo.application.OppfolgingstilfelleService
import no.nav.syfo.application.oppfolgingsoppgave.OppfolgingsoppgaveService
import no.nav.syfo.infrastructure.kafka.behandlendeenhet.BehandlendeEnhetConsumer
import no.nav.syfo.infrastructure.kafka.kartleggingssporsmal.KartleggingssporsmalKandidatStatusConsumer
import no.nav.syfo.infrastructure.kafka.manglendemedvirkning.ManglendeMedvirkningVurderingConsumer
import no.nav.syfo.infrastructure.kafka.oppfolgingsenfase.SenOppfolgingKandidatStatusConsumer
import no.nav.syfo.infrastructure.kafka.oppfolgingsoppgave.launchOppfolgingsoppgaveConsumer

fun launchKafkaModule(
    applicationState: ApplicationState,
    environment: Environment,
    azureAdClient: AzureAdClient,
    personoversiktStatusService: PersonoversiktStatusService,
    personBehandlendeEnhetService: PersonBehandlendeEnhetService,
    oppfolgingstilfelleService: OppfolgingstilfelleService,
    oppfolgingsoppgaveService: OppfolgingsoppgaveService,
    personOversiktStatusRepository: IPersonOversiktStatusRepository,
    transactionManager: ITransactionManager,
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
        personOversiktStatusRepository = personOversiktStatusRepository,
        transactionManager = transactionManager,
    )
    launchKafkaTaskDialogmoteStatusendring(
        applicationState = applicationState,
        kafkaEnvironment = environment.kafka,
        personOversiktStatusRepository = personOversiktStatusRepository,
        transactionManager = transactionManager,
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
        personOversiktStatusRepository = personOversiktStatusRepository,
    )

    launchKafkaTaskPersonhendelse(
        applicationState = applicationState,
        environment = environment,
    )

    launchOppfolgingsoppgaveConsumer(
        applicationState = applicationState,
        kafkaEnvironment = environment.kafka,
        oppfolgingsoppgaveService = oppfolgingsoppgaveService,
    )
    launchKafkaTaskFriskTilArbeidVedtak(
        applicationState = applicationState,
        kafkaEnvironment = environment.kafka,
        personOversiktStatusRepository = personOversiktStatusRepository,
        transactionManager = transactionManager,
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

    BehandlendeEnhetConsumer(personBehandlendeEnhetService = personBehandlendeEnhetService)
        .start(
            applicationState = applicationState,
            kafkaEnvironment = environment.kafka,
        )

    KartleggingssporsmalKandidatStatusConsumer(personoversiktStatusService = personoversiktStatusService)
        .start(
            applicationState = applicationState,
            kafkaEnvironment = environment.kafka,
        )
}
