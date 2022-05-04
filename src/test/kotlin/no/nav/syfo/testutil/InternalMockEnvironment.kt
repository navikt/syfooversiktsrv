package no.nav.syfo.testutil

import io.mockk.mockk
import no.nav.syfo.application.cache.RedisStore
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.client.behandlendeenhet.BehandlendeEnhetClient
import no.nav.syfo.client.ereg.EregClient
import no.nav.syfo.cronjob.behandlendeenhet.PersonBehandlendeEnhetCronjob
import no.nav.syfo.cronjob.behandlendeenhet.PersonBehandlendeEnhetService
import no.nav.syfo.cronjob.virksomhetsnavn.PersonOppfolgingstilfelleVirksomhetnavnCronjob
import no.nav.syfo.cronjob.virksomhetsnavn.PersonOppfolgingstilfelleVirksomhetsnavnService
import no.nav.syfo.dialogmotekandidat.kafka.KafkaDialogmotekandidatEndring
import no.nav.syfo.dialogmotekandidat.kafka.KafkaDialogmotekandidatEndringService
import no.nav.syfo.oppfolgingstilfelle.kafka.KafkaOppfolgingstilfellePerson
import no.nav.syfo.oppfolgingstilfelle.kafka.KafkaOppfolgingstilfellePersonService
import no.nav.syfo.personstatus.OversiktHendelseService
import org.apache.kafka.clients.consumer.KafkaConsumer

class InternalMockEnvironment private constructor() {
    private val externalMockEnvironment: ExternalMockEnvironment = ExternalMockEnvironment.instance
    private val database = externalMockEnvironment.database
    private val environment = externalMockEnvironment.environment

    private val redisStore = RedisStore(
        redisEnvironment = environment.redis,
    )
    private val azureAdClient = AzureAdClient(
        azureEnviroment = environment.azure,
        redisStore = redisStore,
    )
    private val behandlendeEnhetClient = BehandlendeEnhetClient(
        azureAdClient = azureAdClient,
        clientEnvironment = environment.clients.syfobehandlendeenhet,
    )
    private val eregClient = EregClient(
        azureAdClient = azureAdClient,
        clientEnvironment = environment.clients.isproxy,
        redisStore = redisStore,
    )

    val kafkaOppfolgingstilfellePersonService = KafkaOppfolgingstilfellePersonService(
        database = database,
    )
    val kafkaConsumerOppfolgingstilfellePerson = mockk<KafkaConsumer<String, KafkaOppfolgingstilfellePerson>>()

    val oversiktHendelseService = OversiktHendelseService(
        database = database,
    )

    private val personBehandlendeEnhetService = PersonBehandlendeEnhetService(
        database = database,
        behandlendeEnhetClient = behandlendeEnhetClient,
    )
    val personBehandlendeEnhetCronjob = PersonBehandlendeEnhetCronjob(
        personBehandlendeEnhetService = personBehandlendeEnhetService,
    )

    private val personOppfolgingstilfelleVirksomhetsnavnService = PersonOppfolgingstilfelleVirksomhetsnavnService(
        database = database,
        eregClient = eregClient,
    )
    val personOppfolgingstilfelleVirksomhetnavnCronjob = PersonOppfolgingstilfelleVirksomhetnavnCronjob(
        personOppfolgingstilfelleVirksomhetsnavnService = personOppfolgingstilfelleVirksomhetsnavnService,
    )

    val kafkaDialogmotekandidatEndringService = KafkaDialogmotekandidatEndringService(
        database = database
    )
    val kafkaConsumerDialogmotekandidatEndring = mockk<KafkaConsumer<String, KafkaDialogmotekandidatEndring>>()

    companion object {
        val instance: InternalMockEnvironment by lazy {
            InternalMockEnvironment()
        }
    }
}
