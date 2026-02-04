package no.nav.syfo.personoppgavehendelse.kafka

import io.mockk.every
import kotlinx.coroutines.runBlocking
import no.nav.syfo.personstatus.domain.OversikthendelseType
import no.nav.syfo.personstatus.domain.PersonIdent
import no.nav.syfo.personstatus.domain.PersonOversiktStatus
import no.nav.syfo.personstatus.infrastructure.kafka.personoppgavehendelse.KPersonoppgavehendelse
import no.nav.syfo.personstatus.infrastructure.kafka.personoppgavehendelse.PERSONOPPGAVEHENDELSE_TOPIC
import no.nav.syfo.personstatus.infrastructure.kafka.personoppgavehendelse.PersonoppgavehendelseConsumer
import no.nav.syfo.testutil.*
import no.nav.syfo.testutil.generator.generateKPersonoppgavehendelse
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration

class PersonoppgavehendelseConsumerTest {
    private val internalMockEnvironment = InternalMockEnvironment.instance
    private val database = ExternalMockEnvironment.instance.database
    private val personOversiktStatusRepository = ExternalMockEnvironment.instance.personOversiktStatusRepository
    private val personoversiktStatusService = internalMockEnvironment.personoversiktStatusService
    private val personoppgavehendelseConsumer = PersonoppgavehendelseConsumer(personoversiktStatusService)
    private val mockPersonoppgavehendelseConsumer = TestKafkaModule.kafkaPersonoppgavehendelse

    private val lpsbistandMottatt = KPersonoppgavehendelse(
        personident = UserConstants.ARBEIDSTAKER_FNR,
        hendelsetype = OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_MOTTATT,
    )
    private val lpsbistandBehandlet = KPersonoppgavehendelse(
        personident = UserConstants.ARBEIDSTAKER_FNR,
        hendelsetype = OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_BEHANDLET,
    )

    @BeforeEach
    fun setUp() {
        every { mockPersonoppgavehendelseConsumer.commitSync() } returns Unit
    }

    @AfterEach
    fun tearDown() {
        database.resetDatabase()
    }

    @Test
    fun `Create personoversiktstatus on read from topic personoppgavehendelser`() {
        mockReceiveHendelse(lpsbistandMottatt, mockPersonoppgavehendelseConsumer)
        runBlocking {
            personoppgavehendelseConsumer.pollAndProcessRecords(kafkaConsumer = mockPersonoppgavehendelseConsumer)
        }

        val personoversiktStatus = personOversiktStatusRepository.getPersonOversiktStatus(PersonIdent(UserConstants.ARBEIDSTAKER_FNR))
        assertNotNull(personoversiktStatus)
        val isUbehandlet = personoversiktStatus!!.oppfolgingsplanLPSBistandUbehandlet!!
        assertTrue(isUbehandlet)
    }

    @Test
    fun `Update personoversiktstatus on read from topic personoppgavehendelser`() {
        mockReceiveHendelse(lpsbistandBehandlet, mockPersonoppgavehendelseConsumer)
        val personOversiktStatus = PersonOversiktStatus(fnr = UserConstants.ARBEIDSTAKER_FNR)
            .applyOversikthendelse(OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_MOTTATT)
        database.createPersonOversiktStatus(personOversiktStatus)

        runBlocking {
            personoppgavehendelseConsumer.pollAndProcessRecords(kafkaConsumer = mockPersonoppgavehendelseConsumer)
        }

        val personoversiktStatus = personOversiktStatusRepository.getPersonOversiktStatus(PersonIdent(UserConstants.ARBEIDSTAKER_FNR))
        assertNotNull(personoversiktStatus)
        val isUbehandlet = personoversiktStatus!!.oppfolgingsplanLPSBistandUbehandlet!!
        assertFalse(isUbehandlet)
    }

    @Test
    fun `Update personoversiktstatus with dialogmotesvar`() {
        val dialogmotesvarBehandlet = KPersonoppgavehendelse(
            personident = UserConstants.ARBEIDSTAKER_FNR,
            hendelsetype = OversikthendelseType.DIALOGMOTESVAR_BEHANDLET,
        )
        mockReceiveHendelse(dialogmotesvarBehandlet, mockPersonoppgavehendelseConsumer)
        val personOversiktStatus = PersonOversiktStatus(fnr = UserConstants.ARBEIDSTAKER_FNR)
            .applyOversikthendelse(OversikthendelseType.DIALOGMOTESVAR_MOTTATT)
        database.createPersonOversiktStatus(personOversiktStatus)
        runBlocking {
            personoppgavehendelseConsumer.pollAndProcessRecords(kafkaConsumer = mockPersonoppgavehendelseConsumer)
        }

        val personoversiktStatus = personOversiktStatusRepository.getPersonOversiktStatus(PersonIdent(UserConstants.ARBEIDSTAKER_FNR))
        assertNotNull(personoversiktStatus)
        val isUbehandlet = personoversiktStatus!!.dialogmotesvarUbehandlet
        assertFalse(isUbehandlet)
    }

    @Test
    fun `Create personoversiktstatus from behandlerdialog svar mottatt`() {
        val behandlerdialogSvarMottatt = KPersonoppgavehendelse(
            personident = UserConstants.ARBEIDSTAKER_FNR,
            hendelsetype = OversikthendelseType.BEHANDLERDIALOG_SVAR_MOTTATT,
        )
        mockReceiveHendelse(behandlerdialogSvarMottatt, mockPersonoppgavehendelseConsumer)
        runBlocking {
            personoppgavehendelseConsumer.pollAndProcessRecords(kafkaConsumer = mockPersonoppgavehendelseConsumer)
        }

        val personoversiktStatus = personOversiktStatusRepository.getPersonOversiktStatus(PersonIdent(UserConstants.ARBEIDSTAKER_FNR))
        assertNotNull(personoversiktStatus)
        val isUbehandlet = personoversiktStatus!!.behandlerdialogSvarUbehandlet
        assertTrue(isUbehandlet)
    }

    @Test
    fun `Update personoversiktstatus from behandlerdialog svar behandlet`() {
        val behandlerdialogSvarBehandlet = KPersonoppgavehendelse(
            personident = UserConstants.ARBEIDSTAKER_FNR,
            hendelsetype = OversikthendelseType.BEHANDLERDIALOG_SVAR_BEHANDLET,
        )
        mockReceiveHendelse(behandlerdialogSvarBehandlet, mockPersonoppgavehendelseConsumer)
        val personOversiktStatus = PersonOversiktStatus(fnr = UserConstants.ARBEIDSTAKER_FNR)
            .applyOversikthendelse(OversikthendelseType.BEHANDLERDIALOG_SVAR_MOTTATT)
        database.createPersonOversiktStatus(personOversiktStatus)
        runBlocking {
            personoppgavehendelseConsumer.pollAndProcessRecords(kafkaConsumer = mockPersonoppgavehendelseConsumer)
        }

        val personoversiktStatus = personOversiktStatusRepository.getPersonOversiktStatus(PersonIdent(UserConstants.ARBEIDSTAKER_FNR))
        assertNotNull(personoversiktStatus)
        val isUbehandlet = personoversiktStatus!!.behandlerdialogSvarUbehandlet
        assertFalse(isUbehandlet)
    }

    @Test
    fun `Create personoversiktstatus from behandlerdialog ubesvart mottatt`() {
        val behandlerdialogUbesvartMottatt = KPersonoppgavehendelse(
            personident = UserConstants.ARBEIDSTAKER_FNR,
            hendelsetype = OversikthendelseType.BEHANDLERDIALOG_MELDING_UBESVART_MOTTATT,
        )
        mockReceiveHendelse(behandlerdialogUbesvartMottatt, mockPersonoppgavehendelseConsumer)
        runBlocking {
            personoppgavehendelseConsumer.pollAndProcessRecords(kafkaConsumer = mockPersonoppgavehendelseConsumer)
        }

        val personoversiktStatus = personOversiktStatusRepository.getPersonOversiktStatus(PersonIdent(UserConstants.ARBEIDSTAKER_FNR))
        assertNotNull(personoversiktStatus)
        val isUbehandlet = personoversiktStatus!!.behandlerdialogUbesvartUbehandlet
        assertTrue(isUbehandlet)
    }

    @Test
    fun `Update personoversiktstatus from behandlerdialog ubesvart behandlet`() {
        val behandlerdialogUbesvartBehandlet = KPersonoppgavehendelse(
            personident = UserConstants.ARBEIDSTAKER_FNR,
            hendelsetype = OversikthendelseType.BEHANDLERDIALOG_MELDING_UBESVART_BEHANDLET,
        )
        mockReceiveHendelse(behandlerdialogUbesvartBehandlet, mockPersonoppgavehendelseConsumer)
        val personOversiktStatus = PersonOversiktStatus(fnr = UserConstants.ARBEIDSTAKER_FNR)
            .applyOversikthendelse(OversikthendelseType.BEHANDLERDIALOG_MELDING_UBESVART_MOTTATT)
        database.createPersonOversiktStatus(personOversiktStatus)
        runBlocking {
            personoppgavehendelseConsumer.pollAndProcessRecords(kafkaConsumer = mockPersonoppgavehendelseConsumer)
        }

        val personoversiktStatus = personOversiktStatusRepository.getPersonOversiktStatus(PersonIdent(UserConstants.ARBEIDSTAKER_FNR))
        assertNotNull(personoversiktStatus)
        val isUbehandlet = personoversiktStatus!!.behandlerdialogUbesvartUbehandlet
        assertFalse(isUbehandlet)
    }

    @Test
    fun `Create personoversiktstatus from behandlerdialog avvist mottatt hendelse`() {
        val behandlerdialogAvvistMottatt = KPersonoppgavehendelse(
            personident = UserConstants.ARBEIDSTAKER_FNR,
            hendelsetype = OversikthendelseType.BEHANDLERDIALOG_MELDING_AVVIST_MOTTATT,
        )
        mockReceiveHendelse(behandlerdialogAvvistMottatt, mockPersonoppgavehendelseConsumer)
        runBlocking {
            personoppgavehendelseConsumer.pollAndProcessRecords(kafkaConsumer = mockPersonoppgavehendelseConsumer)
        }

        val personoversiktStatus = personOversiktStatusRepository.getPersonOversiktStatus(PersonIdent(UserConstants.ARBEIDSTAKER_FNR))
        assertNotNull(personoversiktStatus)
        assertTrue(personoversiktStatus!!.behandlerdialogAvvistUbehandlet)
    }

    @Test
    fun `Update personoversiktstatus from behandlerdialog avvist behandlet hendelse`() {
        val behandlerdialogAvvistBehandlet = KPersonoppgavehendelse(
            personident = UserConstants.ARBEIDSTAKER_FNR,
            hendelsetype = OversikthendelseType.BEHANDLERDIALOG_MELDING_AVVIST_BEHANDLET,
        )
        mockReceiveHendelse(behandlerdialogAvvistBehandlet, mockPersonoppgavehendelseConsumer)
        val personOversiktStatus = PersonOversiktStatus(fnr = UserConstants.ARBEIDSTAKER_FNR)
            .applyOversikthendelse(OversikthendelseType.BEHANDLERDIALOG_MELDING_AVVIST_MOTTATT)
        database.createPersonOversiktStatus(personOversiktStatus)
        runBlocking {
            personoppgavehendelseConsumer.pollAndProcessRecords(kafkaConsumer = mockPersonoppgavehendelseConsumer)
        }

        val personoversiktStatus = personOversiktStatusRepository.getPersonOversiktStatus(PersonIdent(UserConstants.ARBEIDSTAKER_FNR))
        assertNotNull(personoversiktStatus)
        assertFalse(personoversiktStatus!!.behandlerdialogAvvistUbehandlet)
    }

    @Test
    fun `Update personoversiktstatus from behandler_ber_om_bistand_mottatt-hendelse`() {
        val behandlerBistandHendelseMottatt =
            generateKPersonoppgavehendelse(OversikthendelseType.BEHANDLER_BER_OM_BISTAND_MOTTATT)
        mockReceiveHendelse(behandlerBistandHendelseMottatt, mockPersonoppgavehendelseConsumer)

        runBlocking {
            personoppgavehendelseConsumer.pollAndProcessRecords(kafkaConsumer = mockPersonoppgavehendelseConsumer)
        }

        val personoversiktStatus = personOversiktStatusRepository.getPersonOversiktStatus(PersonIdent(UserConstants.ARBEIDSTAKER_FNR))
        assertNotNull(personoversiktStatus)
        assertTrue(personoversiktStatus!!.behandlerBerOmBistandUbehandlet)
    }

    @Test
    fun `Update personoversiktstatus from behandler_ber_om_bistand_behandlet-hendelse`() {
        val behandlerBistandHendelseBehandlet =
            generateKPersonoppgavehendelse(OversikthendelseType.BEHANDLER_BER_OM_BISTAND_BEHANDLET)
        mockReceiveHendelse(behandlerBistandHendelseBehandlet, mockPersonoppgavehendelseConsumer)
        val personOversiktStatus = PersonOversiktStatus(fnr = UserConstants.ARBEIDSTAKER_FNR)
            .applyOversikthendelse(OversikthendelseType.BEHANDLER_BER_OM_BISTAND_MOTTATT)
        database.createPersonOversiktStatus(personOversiktStatus)

        runBlocking {
            personoppgavehendelseConsumer.pollAndProcessRecords(kafkaConsumer = mockPersonoppgavehendelseConsumer)
        }

        val personoversiktStatus = personOversiktStatusRepository.getPersonOversiktStatus(PersonIdent(UserConstants.ARBEIDSTAKER_FNR))
        assertNotNull(personoversiktStatus)
        assertFalse(personoversiktStatus!!.behandlerBerOmBistandUbehandlet)
    }
}

fun mockReceiveHendelse(
    hendelse: KPersonoppgavehendelse,
    mockKafkaPersonoppgavehendelse: KafkaConsumer<String, KPersonoppgavehendelse>,
) {
    every { mockKafkaPersonoppgavehendelse.poll(any<Duration>()) } returns ConsumerRecords(
        mapOf(
            personoppgavehendelseTopicPartition() to listOf(
                personoppgavehendelseRecord(hendelse),
            )
        )
    )
}

fun personoppgavehendelseTopicPartition() = TopicPartition(
    PERSONOPPGAVEHENDELSE_TOPIC,
    0
)

fun personoppgavehendelseRecord(
    kPersonoppgavehendelse: KPersonoppgavehendelse,
) = ConsumerRecord(
    PERSONOPPGAVEHENDELSE_TOPIC,
    0,
    1,
    "key1",
    kPersonoppgavehendelse
)
