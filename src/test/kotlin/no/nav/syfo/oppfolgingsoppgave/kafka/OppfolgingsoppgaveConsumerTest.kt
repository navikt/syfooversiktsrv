package no.nav.syfo.oppfolgingsoppgave.kafka

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.syfo.personstatus.application.oppfolgingsoppgave.OppfolgingsoppgaveRecord
import no.nav.syfo.personstatus.application.oppfolgingsoppgave.OppfolgingsoppgaveService
import no.nav.syfo.personstatus.domain.PersonOversiktStatus
import no.nav.syfo.personstatus.infrastructure.database.queries.getPersonOversiktStatusList
import no.nav.syfo.personstatus.infrastructure.kafka.oppfolgingsoppgave.OppfolgingsoppgaveConsumer
import no.nav.syfo.testutil.*
import no.nav.syfo.testutil.generator.generateKafkaHuskelapp
import no.nav.syfo.testutil.generator.huskelappConsumerRecord
import no.nav.syfo.testutil.generator.huskelappTopicPartition
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDate

class OppfolgingsoppgaveConsumerTest {
    private val externalMockEnvironment = ExternalMockEnvironment.instance
    private val internalMockEnvironment = InternalMockEnvironment.instance
    private val database = externalMockEnvironment.database

    private val kafkaConsumerMock = mockk<KafkaConsumer<String, OppfolgingsoppgaveRecord>>()
    private val oppfolgingsoppgaveService = OppfolgingsoppgaveService(
        personBehandlendeEnhetService = internalMockEnvironment.personBehandlendeEnhetService,
        personOversiktStatusRepository = externalMockEnvironment.personOversiktStatusRepository,
    )
    private val oppfolgingsoppgaveConsumer = OppfolgingsoppgaveConsumer(oppfolgingsoppgaveService)

    private val frist = LocalDate.now().plusWeeks(1)

    @BeforeEach
    fun setUp() {
        database.dropData()
        clearMocks(kafkaConsumerMock)
        every { kafkaConsumerMock.commitSync() } returns Unit
    }

    @Nested
    @DisplayName("No PersonOversiktStatus exists for personident")
    inner class NoPersonOversiktStatusExists {

        @Test
        fun `Creates new PersonOversiktStatus for personident from kafka record with active huskelapp and frist`() {
            val activeHuskelappWithFrist = generateKafkaHuskelapp(isActive = true, frist = frist)
            mockIncomingKafkaRecord(
                kafkaRecord = activeHuskelappWithFrist,
                kafkaConsumerMock = kafkaConsumerMock,
            )

            runBlocking { oppfolgingsoppgaveConsumer.pollAndProcessRecords(consumer = kafkaConsumerMock) }

            verify(exactly = 1) {
                kafkaConsumerMock.commitSync()
            }

            val pPersonOversiktStatusList =
                database.connection.use { it.getPersonOversiktStatusList(UserConstants.ARBEIDSTAKER_FNR) }
            assertEquals(1, pPersonOversiktStatusList.size)
            val pPersonOversiktStatus = pPersonOversiktStatusList.first()
            assertEquals(activeHuskelappWithFrist.personIdent, pPersonOversiktStatus.fnr)
            assertTrue(pPersonOversiktStatus.isAktivOppfolgingsoppgave)
        }

        @Test
        fun `Creates new PersonOversiktStatus for personident from kafka record with active huskelapp no frist`() {
            val activeHuskelappNoFrist = generateKafkaHuskelapp(isActive = true, frist = null)
            mockIncomingKafkaRecord(
                kafkaRecord = activeHuskelappNoFrist,
                kafkaConsumerMock = kafkaConsumerMock,
            )

            runBlocking {
                oppfolgingsoppgaveConsumer.pollAndProcessRecords(consumer = kafkaConsumerMock)
            }

            verify(exactly = 1) {
                kafkaConsumerMock.commitSync()
            }

            val pPersonOversiktStatusList =
                database.connection.use { it.getPersonOversiktStatusList(UserConstants.ARBEIDSTAKER_FNR) }
            assertEquals(1, pPersonOversiktStatusList.size)
            val pPersonOversiktStatus = pPersonOversiktStatusList.first()
            assertEquals(activeHuskelappNoFrist.personIdent, pPersonOversiktStatus.fnr)
            assertTrue(pPersonOversiktStatus.isAktivOppfolgingsoppgave)
        }

        @Test
        fun `Updates PersonOversiktStatus tildeltEnhet for personident from kafka record with active huskelapp`() {
            val activeHuskelappNoFrist = generateKafkaHuskelapp(isActive = true, frist = null)
            mockIncomingKafkaRecord(
                kafkaRecord = activeHuskelappNoFrist,
                kafkaConsumerMock = kafkaConsumerMock,
            )

            runBlocking {
                oppfolgingsoppgaveConsumer.pollAndProcessRecords(consumer = kafkaConsumerMock)
            }

            verify(exactly = 1) {
                kafkaConsumerMock.commitSync()
            }

            val pPersonOversiktStatusList =
                database.connection.use { it.getPersonOversiktStatusList(UserConstants.ARBEIDSTAKER_FNR) }
            assertEquals(1, pPersonOversiktStatusList.size)
            val pPersonOversiktStatus = pPersonOversiktStatusList.first()
            assertNotNull(pPersonOversiktStatus.enhet)
        }

        @Test
        fun `Does not update PersonOversiktStatus tildeltEnhet for personident from kafka record with inactive huskelapp`() {
            val inactiveHuskelappNoFrist = generateKafkaHuskelapp(isActive = false, frist = null)
            mockIncomingKafkaRecord(
                kafkaRecord = inactiveHuskelappNoFrist,
                kafkaConsumerMock = kafkaConsumerMock,
            )

            runBlocking {
                oppfolgingsoppgaveConsumer.pollAndProcessRecords(consumer = kafkaConsumerMock)
            }

            verify(exactly = 1) {
                kafkaConsumerMock.commitSync()
            }

            val pPersonOversiktStatusList =
                database.connection.use { it.getPersonOversiktStatusList(UserConstants.ARBEIDSTAKER_FNR) }
            assertEquals(1, pPersonOversiktStatusList.size)
            val pPersonOversiktStatus = pPersonOversiktStatusList.first()
            assertNull(pPersonOversiktStatus.enhet)
        }

        @Test
        fun `Does not update PersonOversiktStatus tildeltEnhet for personident from kafka record with active huskelapp and failing call to behandlende enhet`() {
            val personIdent = UserConstants.ARBEIDSTAKER_ENHET_ERROR_PERSONIDENT.value
            val activeHuskelappNoFrist = generateKafkaHuskelapp(isActive = true, frist = null, personIdent = personIdent)
            mockIncomingKafkaRecord(
                kafkaRecord = activeHuskelappNoFrist,
                kafkaConsumerMock = kafkaConsumerMock,
            )

            runBlocking {
                oppfolgingsoppgaveConsumer.pollAndProcessRecords(consumer = kafkaConsumerMock)
            }

            verify(exactly = 1) {
                kafkaConsumerMock.commitSync()
            }

            val pPersonOversiktStatusList =
                database.connection.use { it.getPersonOversiktStatusList(personIdent) }
            assertEquals(1, pPersonOversiktStatusList.size)
            val pPersonOversiktStatus = pPersonOversiktStatusList.first()
            assertNull(pPersonOversiktStatus.enhet)
        }
    }

    @Nested
    @DisplayName("Existing PersonOversikStatus for personident")
    inner class ExistingPersonOversikStatus {

        @Test
        fun `Updates trenger_oppfolging from kafka record with active huskelapp and frist`() {
            val activeHuskelappWithFrist = generateKafkaHuskelapp(isActive = true, frist = frist)
            val personident = UserConstants.ARBEIDSTAKER_FNR
            database.createPersonOversiktStatus(
                personOversiktStatus = PersonOversiktStatus(fnr = personident)
            )
            mockIncomingKafkaRecord(
                kafkaRecord = activeHuskelappWithFrist,
                kafkaConsumerMock = kafkaConsumerMock,
            )

            runBlocking {
                oppfolgingsoppgaveConsumer.pollAndProcessRecords(consumer = kafkaConsumerMock)
            }

            verify(exactly = 1) {
                kafkaConsumerMock.commitSync()
            }
            val pPersonOversiktStatusList =
                database.connection.use { it.getPersonOversiktStatusList(personident) }
            assertEquals(1, pPersonOversiktStatusList.size)
            val pPersonOversiktStatus = pPersonOversiktStatusList.first()
            assertEquals(activeHuskelappWithFrist.personIdent, pPersonOversiktStatus.fnr)
            assertTrue(pPersonOversiktStatus.isAktivOppfolgingsoppgave)
        }

        @Test
        fun `Updates to trenger_oppfolging false from kafka record with inactive huskelapp and frist`() {
            val inactiveHuskelappWithFrist = generateKafkaHuskelapp(isActive = false, frist = frist)
            val personident = UserConstants.ARBEIDSTAKER_FNR
            database.createPersonOversiktStatus(
                personOversiktStatus = PersonOversiktStatus(
                    fnr = personident,
                ).copy(
                    isAktivOppfolgingsoppgave = true,
                )
            )
            mockIncomingKafkaRecord(
                kafkaRecord = inactiveHuskelappWithFrist,
                kafkaConsumerMock = kafkaConsumerMock,
            )

            runBlocking {
                oppfolgingsoppgaveConsumer.pollAndProcessRecords(consumer = kafkaConsumerMock)
            }

            verify(exactly = 1) {
                kafkaConsumerMock.commitSync()
            }
            val pPersonOversiktStatusList =
                database.connection.use { it.getPersonOversiktStatusList(personident) }
            assertEquals(1, pPersonOversiktStatusList.size)
            val pPersonOversiktStatus = pPersonOversiktStatusList.first()
            assertEquals(inactiveHuskelappWithFrist.personIdent, pPersonOversiktStatus.fnr)
            assertFalse(pPersonOversiktStatus.isAktivOppfolgingsoppgave)
        }

        @Test
        fun `Updates to trenger_oppfolging false from kafka record with inactive huskelapp and no frist`() {
            val inactiveHuskelappNoFrist = generateKafkaHuskelapp(isActive = false, frist = null)
            val personident = UserConstants.ARBEIDSTAKER_FNR
            database.createPersonOversiktStatus(
                personOversiktStatus = PersonOversiktStatus(
                    fnr = personident,
                ).copy(
                    isAktivOppfolgingsoppgave = true,
                )
            )
            mockIncomingKafkaRecord(
                kafkaRecord = inactiveHuskelappNoFrist,
                kafkaConsumerMock = kafkaConsumerMock,
            )

            runBlocking {
                oppfolgingsoppgaveConsumer.pollAndProcessRecords(consumer = kafkaConsumerMock)
            }

            verify(exactly = 1) {
                kafkaConsumerMock.commitSync()
            }
            val pPersonOversiktStatusList =
                database.connection.use { it.getPersonOversiktStatusList(personident) }
            assertEquals(1, pPersonOversiktStatusList.size)
            val pPersonOversiktStatus = pPersonOversiktStatusList.first()
            assertEquals(inactiveHuskelappNoFrist.personIdent, pPersonOversiktStatus.fnr)
            assertFalse(pPersonOversiktStatus.isAktivOppfolgingsoppgave)
        }
    }
}

fun mockIncomingKafkaRecord(kafkaRecord: OppfolgingsoppgaveRecord, kafkaConsumerMock: KafkaConsumer<String, OppfolgingsoppgaveRecord>) {
    every { kafkaConsumerMock.poll(any<Duration>()) } returns ConsumerRecords(
        mapOf(
            huskelappTopicPartition() to listOf(
                huskelappConsumerRecord(
                    oppfolgingsoppgaveRecord = kafkaRecord,
                ),
            )
        )
    )
}
