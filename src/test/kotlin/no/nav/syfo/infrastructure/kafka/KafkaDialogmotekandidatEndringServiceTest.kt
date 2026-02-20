package no.nav.syfo.infrastructure.kafka

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.infrastructure.kafka.dialogmotekandidat.toPersonOversiktStatus
import no.nav.syfo.testutil.ExternalMockEnvironment
import no.nav.syfo.testutil.TestKafkaModule
import no.nav.syfo.testutil.UserConstants
import no.nav.syfo.testutil.createPersonOversiktStatus
import no.nav.syfo.testutil.generator.dialogmotekandidatEndringConsumerRecord
import no.nav.syfo.testutil.generator.dialogmotekandidatEndringTopicPartition
import no.nav.syfo.testutil.generator.generateKafkaDialogmotekandidatEndringStoppunkt
import no.nav.syfo.testutil.generator.generateKafkaDialogmotekandidatEndringUnntak
import no.nav.syfo.testutil.generator.generateKafkaOppfolgingstilfellePerson
import no.nav.syfo.util.nowUTC
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertNull
import java.time.Duration
import kotlin.test.assertEquals

class KafkaDialogmotekandidatEndringServiceTest {

    private val externalMockEnvironment = ExternalMockEnvironment.Companion.instance
    private val database = externalMockEnvironment.database
    private val personOversiktStatusRepository = externalMockEnvironment.personOversiktStatusRepository

    private val kafkaDialogmotekandidatEndringService = TestKafkaModule.dialogmotekandidatEndringConsumer
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

        val personstatus = personOversiktStatusRepository.getPersonOversiktStatus(
            PersonIdent(UserConstants.ARBEIDSTAKER_FNR)
        )

        assertNotNull(personstatus)

        assertEquals(kafkaDialogmotekandidatEndringStoppunktYesterday.personIdentNumber, personstatus!!.fnr)
        assertEquals(kafkaDialogmotekandidatEndringStoppunktYesterday.kandidat, personstatus.dialogmotekandidat)
        assertNotNull(personstatus.dialogmotekandidatGeneratedAt)

        assertNull(personstatus.enhet)
        assertNull(personstatus.veilederIdent)
        assertNull(personstatus.motebehovUbehandlet)
        assertNull(personstatus.oppfolgingsplanLPSBistandUbehandlet)
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

        val pPersonOversiktStatus = personOversiktStatusRepository.getPersonOversiktStatus(
            PersonIdent(UserConstants.ARBEIDSTAKER_FNR)
        )

        assertNotNull(pPersonOversiktStatus)
        assertNotNull(pPersonOversiktStatus!!.latestOppfolgingstilfelle?.generatedAt)
        assertNotNull(pPersonOversiktStatus.latestOppfolgingstilfelle?.oppfolgingstilfelleBitReferanseUuid)

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

        val pPersonOversiktStatus = personOversiktStatusRepository.getPersonOversiktStatus(
            PersonIdent(UserConstants.ARBEIDSTAKER_FNR)
        )

        assertNotNull(pPersonOversiktStatus)
        assertEquals(kafkaDialogmotekandidatEndringUnntakToday.personIdentNumber, pPersonOversiktStatus!!.fnr)
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

        val pPersonOversiktStatus = personOversiktStatusRepository.getPersonOversiktStatus(
            PersonIdent(UserConstants.ARBEIDSTAKER_FNR)
        )

        assertNotNull(pPersonOversiktStatus)
        assertEquals(kafkaDialogmotekandidatEndringUnntakToday.personIdentNumber, pPersonOversiktStatus!!.fnr)
        assertEquals(kafkaDialogmotekandidatEndringUnntakToday.kandidat, pPersonOversiktStatus.dialogmotekandidat)
        assertNotNull(pPersonOversiktStatus.dialogmotekandidatGeneratedAt)
    }
}
