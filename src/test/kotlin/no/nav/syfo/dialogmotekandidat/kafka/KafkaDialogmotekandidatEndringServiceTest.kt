package no.nav.syfo.dialogmotekandidat.kafka

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.syfo.personstatus.infrastructure.database.queries.getPersonOversiktStatusList
import no.nav.syfo.personstatus.infrastructure.kafka.dialogmotekandidat.toPersonOversiktStatus
import no.nav.syfo.testutil.*
import no.nav.syfo.testutil.generator.*
import no.nav.syfo.util.nowUTC
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration

class KafkaDialogmotekandidatEndringServiceTest {

    private val externalMockEnvironment = ExternalMockEnvironment.instance
    private val database = externalMockEnvironment.database

    private val kafkaDialogmotekandidatEndringService = TestKafkaModule.kafkaDialogmotekandidatEndringService
    private val mockKafkaConsumerDialogmotekandidatEndring = TestKafkaModule.kafkaConsumerDialogmotekandidatEndring

    private val dialogmoteKandidatTopicPartition = dialogmotekandidatEndringTopicPartition()
    private val kafkaDialogmotekandidatEndringStoppunktYesterday = generateKafkaDialogmotekandidatEndringStoppunkt(
        personIdent = UserConstants.ARBEIDSTAKER_FNR,
        createdAt = nowUTC().minusDays(1)
    )
    private val kafkaDialogmotekandidatEndringStoppunktConsumerRecord = dialogmotekandidatEndringConsumerRecord(
        kafkaDialogmotekandidatEndring = kafkaDialogmotekandidatEndringStoppunktYesterday
    )
    private val kafkaDialogmotekandidatEndringUnntakToday = generateKafkaDialogmotekandidatEndringUnntak(
        personIdent = UserConstants.ARBEIDSTAKER_FNR,
        createdAt = nowUTC()
    )
    private val kafkaDialogmotekandidatEndringUnntakConsumerRecord = dialogmotekandidatEndringConsumerRecord(
        kafkaDialogmotekandidatEndring = kafkaDialogmotekandidatEndringUnntakToday
    )

    @BeforeEach
    fun setUp() {
        database.resetDatabase()

        clearMocks(mockKafkaConsumerDialogmotekandidatEndring)
        every { mockKafkaConsumerDialogmotekandidatEndring.commitSync() } returns Unit
    }

    @Test
    fun `Creates new PersonOversiktStatus if no PersonOversiktStatus exists for personident`() {
        every { mockKafkaConsumerDialogmotekandidatEndring.poll(any<Duration>()) } returns ConsumerRecords(
            mapOf(
                dialogmoteKandidatTopicPartition to listOf(
                    kafkaDialogmotekandidatEndringStoppunktConsumerRecord,
                )
            )
        )

        runBlocking {
            kafkaDialogmotekandidatEndringService.pollAndProcessRecords(kafkaConsumer = mockKafkaConsumerDialogmotekandidatEndring)
        }

        verify(exactly = 1) {
            mockKafkaConsumerDialogmotekandidatEndring.commitSync()
        }

        val pPersonOversiktStatusList =
            database.connection.getPersonOversiktStatusList(UserConstants.ARBEIDSTAKER_FNR)

        assertEquals(1, pPersonOversiktStatusList.size)

        val pPersonOversiktStatus = pPersonOversiktStatusList.first()

        assertEquals(kafkaDialogmotekandidatEndringStoppunktYesterday.personIdentNumber, pPersonOversiktStatus.fnr)
        assertEquals(kafkaDialogmotekandidatEndringStoppunktYesterday.kandidat, pPersonOversiktStatus.dialogmotekandidat)
        assertNotNull(pPersonOversiktStatus.dialogmotekandidatGeneratedAt)

        assertNull(pPersonOversiktStatus.enhet)
        assertNull(pPersonOversiktStatus.veilederIdent)
        assertNull(pPersonOversiktStatus.motebehovUbehandlet)
        assertNull(pPersonOversiktStatus.oppfolgingsplanLPSBistandUbehandlet)
    }

    @Test
    fun `Updates existing PersonOversikStatus when PersonOversiktStatus exists for personident`() {
        every { mockKafkaConsumerDialogmotekandidatEndring.poll(any<Duration>()) } returns ConsumerRecords(
            mapOf(
                dialogmoteKandidatTopicPartition to listOf(
                    kafkaDialogmotekandidatEndringStoppunktConsumerRecord,
                )
            )
        )

        val kafkaOppfolgingstilfellePerson = generateKafkaOppfolgingstilfellePerson()
        val kafkaOppfolgingstilfelle = kafkaOppfolgingstilfellePerson.oppfolgingstilfelleList.first()
        database.createPersonOversiktStatus(
            personOversiktStatus = kafkaOppfolgingstilfellePerson.toPersonOversiktStatus(kafkaOppfolgingstilfelle)
        )

        runBlocking {
            kafkaDialogmotekandidatEndringService.pollAndProcessRecords(kafkaConsumer = mockKafkaConsumerDialogmotekandidatEndring)
        }

        verify(exactly = 1) {
            mockKafkaConsumerDialogmotekandidatEndring.commitSync()
        }

        val pPersonOversiktStatusList =
            database.connection.getPersonOversiktStatusList(UserConstants.ARBEIDSTAKER_FNR)

        assertEquals(1, pPersonOversiktStatusList.size)
        val pPersonOversiktStatus = pPersonOversiktStatusList.first()
        assertNotNull(pPersonOversiktStatus.oppfolgingstilfelleGeneratedAt)
        assertNotNull(pPersonOversiktStatus.oppfolgingstilfelleBitReferanseUuid)

        assertEquals(kafkaDialogmotekandidatEndringStoppunktYesterday.personIdentNumber, pPersonOversiktStatus.fnr)
        assertEquals(kafkaDialogmotekandidatEndringStoppunktYesterday.kandidat, pPersonOversiktStatus.dialogmotekandidat)
        assertNotNull(pPersonOversiktStatus.dialogmotekandidatGeneratedAt)
    }

    @Test
    fun `Updates PersonOversiktStatus if received dialogmotekandidat-endring created after existing PersonOversiktStatus-dialogmotekandidat`() {
        every { mockKafkaConsumerDialogmotekandidatEndring.poll(any<Duration>()) } returns ConsumerRecords(
            mapOf(
                dialogmoteKandidatTopicPartition to listOf(
                    kafkaDialogmotekandidatEndringUnntakConsumerRecord,
                )
            )
        )

        val existingPersonOversiktStatus = kafkaDialogmotekandidatEndringStoppunktYesterday.toPersonOversiktStatus()
        database.createPersonOversiktStatus(
            personOversiktStatus = existingPersonOversiktStatus
        )
        runBlocking {
            kafkaDialogmotekandidatEndringService.pollAndProcessRecords(kafkaConsumer = mockKafkaConsumerDialogmotekandidatEndring)
        }

        verify(exactly = 1) {
            mockKafkaConsumerDialogmotekandidatEndring.commitSync()
        }

        val pPersonOversiktStatusList =
            database.connection.getPersonOversiktStatusList(UserConstants.ARBEIDSTAKER_FNR)

        assertEquals(1, pPersonOversiktStatusList.size)
        val pPersonOversiktStatus = pPersonOversiktStatusList.first()
        assertEquals(kafkaDialogmotekandidatEndringUnntakToday.personIdentNumber, pPersonOversiktStatus.fnr)
        assertEquals(kafkaDialogmotekandidatEndringUnntakToday.kandidat, pPersonOversiktStatus.dialogmotekandidat)
        assertNotNull(pPersonOversiktStatus.dialogmotekandidatGeneratedAt)
    }

    @Test
    fun `Do not update PersonOversiktStatus if received dialogmotekandidat-endring created before existing PersonOversiktStatus-dialogmotekandidat`() {
        every { mockKafkaConsumerDialogmotekandidatEndring.poll(any<Duration>()) } returns ConsumerRecords(
            mapOf(
                dialogmoteKandidatTopicPartition to listOf(
                    kafkaDialogmotekandidatEndringStoppunktConsumerRecord,
                )
            )
        )

        val existingPersonOversiktStatus = kafkaDialogmotekandidatEndringUnntakToday.toPersonOversiktStatus()
        database.createPersonOversiktStatus(
            personOversiktStatus = existingPersonOversiktStatus
        )
        runBlocking {
            kafkaDialogmotekandidatEndringService.pollAndProcessRecords(kafkaConsumer = mockKafkaConsumerDialogmotekandidatEndring)
        }

        verify(exactly = 1) {
            mockKafkaConsumerDialogmotekandidatEndring.commitSync()
        }

        val pPersonOversiktStatusList =
            database.connection.getPersonOversiktStatusList(UserConstants.ARBEIDSTAKER_FNR)

        assertEquals(1, pPersonOversiktStatusList.size)
        val pPersonOversiktStatus = pPersonOversiktStatusList.first()
        assertEquals(kafkaDialogmotekandidatEndringUnntakToday.personIdentNumber, pPersonOversiktStatus.fnr)
        assertEquals(kafkaDialogmotekandidatEndringUnntakToday.kandidat, pPersonOversiktStatus.dialogmotekandidat)
        assertNotNull(pPersonOversiktStatus.dialogmotekandidatGeneratedAt)
    }
}
