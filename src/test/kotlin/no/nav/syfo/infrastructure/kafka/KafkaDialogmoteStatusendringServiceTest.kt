package no.nav.syfo.infrastructure.kafka

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.syfo.domain.DialogmoteStatusendring
import no.nav.syfo.domain.DialogmoteStatusendringType
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.testutil.ExternalMockEnvironment
import no.nav.syfo.testutil.TestKafkaModule
import no.nav.syfo.testutil.UserConstants
import no.nav.syfo.testutil.createPersonOversiktStatus
import no.nav.syfo.testutil.generator.dialogmoteStatusendringConsumerRecord
import no.nav.syfo.testutil.generator.dialogmoteStatusendringTopicPartition
import no.nav.syfo.testutil.generator.generateKafkaDialogmoteStatusendring
import no.nav.syfo.testutil.generator.generateKafkaOppfolgingstilfellePerson
import no.nav.syfo.util.nowUTC
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertNull
import java.time.Duration
import kotlin.test.assertEquals

class KafkaDialogmoteStatusendringServiceTest {

    private val externalMockEnvironment = ExternalMockEnvironment.Companion.instance
    private val database = externalMockEnvironment.database
    private val personOversiktStatusRepository = externalMockEnvironment.personOversiktStatusRepository

    private val kafkaDialogmoteStatusendringService = TestKafkaModule.dialogmoteStatusendringConsumer
    private val mockKafkaConsumerDialogmoteStatusendring = TestKafkaModule.kafkaConsumerDialogmoteStatusendring

    private val dialogmoteStatusendringTopicPartition = dialogmoteStatusendringTopicPartition()
    private val kafkaDialogmoteStatusendringToday = generateKafkaDialogmoteStatusendring(
        personIdent = UserConstants.ARBEIDSTAKER_FNR,
        type = DialogmoteStatusendringType.INNKALT,
        endringsTidspunkt = nowUTC(),
    )
    private val kafkaDialogmoteStatusendringLastYear = generateKafkaDialogmoteStatusendring(
        personIdent = UserConstants.ARBEIDSTAKER_FNR,
        type = DialogmoteStatusendringType.AVLYST,
        endringsTidspunkt = nowUTC().minusYears(1),
    )
    private val kafkaDialogmoteStatusendringTodayConsumerRecord = dialogmoteStatusendringConsumerRecord(
        kafkaDialogmoteStatusendring = kafkaDialogmoteStatusendringToday,
    )
    private val kafkaDialogmoteStatusendringLastYearConsumerRecord = dialogmoteStatusendringConsumerRecord(
        kafkaDialogmoteStatusendring = kafkaDialogmoteStatusendringLastYear,
    )

    @BeforeEach
    fun setUp() {
        database.resetDatabase()

        clearMocks(mockKafkaConsumerDialogmoteStatusendring)
        every { mockKafkaConsumerDialogmoteStatusendring.commitSync() } returns Unit
    }

    @Test
    fun `Creates new PersonOversiktStatus if no PersonOversiktStatus exists for personident`() {
        every { mockKafkaConsumerDialogmoteStatusendring.poll(any<Duration>()) } returns ConsumerRecords(
            mapOf(
                dialogmoteStatusendringTopicPartition to listOf(
                    kafkaDialogmoteStatusendringTodayConsumerRecord,
                )
            )
        )

        runBlocking {
            kafkaDialogmoteStatusendringService.pollAndProcessRecords(kafkaConsumer = mockKafkaConsumerDialogmoteStatusendring)
        }

        verify(exactly = 1) {
            mockKafkaConsumerDialogmoteStatusendring.commitSync()
        }

        val personstatus =
            personOversiktStatusRepository.getPersonOversiktStatus(personident = PersonIdent(UserConstants.ARBEIDSTAKER_FNR))

        assertNotNull(personstatus)

        assertEquals(kafkaDialogmoteStatusendringToday.getPersonIdent(), personstatus?.fnr)
        assertEquals(kafkaDialogmoteStatusendringToday.getStatusEndringType(), personstatus?.motestatus)
        assertNotNull(personstatus?.motestatusGeneratedAt)

        assertNull(personstatus?.dialogmotekandidat)
        assertNull(personstatus?.enhet)
        assertNull(personstatus?.veilederIdent)
        assertNull(personstatus?.motebehovUbehandlet)
        assertNull(personstatus?.oppfolgingsplanLPSBistandUbehandlet)
    }

    @Test
    fun `Updates existing PersonOversikStatus when PersonOversiktStatus without motestatus exists for personident`() {
        every { mockKafkaConsumerDialogmoteStatusendring.poll(any<Duration>()) } returns ConsumerRecords(
            mapOf(
                dialogmoteStatusendringTopicPartition to listOf(
                    kafkaDialogmoteStatusendringTodayConsumerRecord,
                )
            )
        )

        val kafkaOppfolgingstilfellePerson = generateKafkaOppfolgingstilfellePerson()
        val kafkaOppfolgingstilfelle = kafkaOppfolgingstilfellePerson.oppfolgingstilfelleList.first()
        database.createPersonOversiktStatus(
            personOversiktStatus = kafkaOppfolgingstilfellePerson.toPersonOversiktStatus(kafkaOppfolgingstilfelle)
        )

        runBlocking {
            kafkaDialogmoteStatusendringService.pollAndProcessRecords(kafkaConsumer = mockKafkaConsumerDialogmoteStatusendring)
        }

        verify(exactly = 1) {
            mockKafkaConsumerDialogmoteStatusendring.commitSync()
        }

        val pPersonOversiktStatus =
            personOversiktStatusRepository.getPersonOversiktStatus(personident = PersonIdent(UserConstants.ARBEIDSTAKER_FNR))

        assertNotNull(pPersonOversiktStatus)

        assertEquals(kafkaDialogmoteStatusendringToday.getPersonIdent(), pPersonOversiktStatus?.fnr)
        assertEquals(kafkaDialogmoteStatusendringToday.getStatusEndringType(), pPersonOversiktStatus?.motestatus)
        assertNotNull(pPersonOversiktStatus?.motestatusGeneratedAt)
    }

    @Test
    fun `Updates PersonOversiktStatus if received motestatus-endring created after existing PersonOversiktStatus-motestatus`() {
        every { mockKafkaConsumerDialogmoteStatusendring.poll(any<Duration>()) } returns ConsumerRecords(
            mapOf(
                dialogmoteStatusendringTopicPartition to listOf(
                    kafkaDialogmoteStatusendringTodayConsumerRecord,
                )
            )
        )

        val existingPersonOversiktStatus = DialogmoteStatusendring.Companion.create(
            kafkaDialogmoteStatusEndring = kafkaDialogmoteStatusendringLastYear
        ).toPersonOversiktStatus()
        database.createPersonOversiktStatus(
            personOversiktStatus = existingPersonOversiktStatus
        )

        runBlocking {
            kafkaDialogmoteStatusendringService.pollAndProcessRecords(kafkaConsumer = mockKafkaConsumerDialogmoteStatusendring)
        }

        verify(exactly = 1) {
            mockKafkaConsumerDialogmoteStatusendring.commitSync()
        }

        val pPersonOversiktStatus =
            personOversiktStatusRepository.getPersonOversiktStatus(personident = PersonIdent(UserConstants.ARBEIDSTAKER_FNR))

        assertNotNull(pPersonOversiktStatus)

        assertEquals(kafkaDialogmoteStatusendringToday.getPersonIdent(), pPersonOversiktStatus?.fnr)
        assertEquals(kafkaDialogmoteStatusendringToday.getStatusEndringType(), pPersonOversiktStatus?.motestatus)
        assertNotNull(pPersonOversiktStatus?.motestatusGeneratedAt)
    }

    @Test
    fun `Do not update PersonOversiktStatus if received motestatus-endring created before existing PersonOversiktStatus-motestatus`() {
        every { mockKafkaConsumerDialogmoteStatusendring.poll(any<Duration>()) } returns ConsumerRecords(
            mapOf(
                dialogmoteStatusendringTopicPartition to listOf(
                    kafkaDialogmoteStatusendringLastYearConsumerRecord,
                )
            )
        )

        val existingPersonOversiktStatus = DialogmoteStatusendring.Companion.create(
            kafkaDialogmoteStatusEndring = kafkaDialogmoteStatusendringToday
        ).toPersonOversiktStatus()
        database.createPersonOversiktStatus(
            personOversiktStatus = existingPersonOversiktStatus
        )

        runBlocking {
            kafkaDialogmoteStatusendringService.pollAndProcessRecords(kafkaConsumer = mockKafkaConsumerDialogmoteStatusendring)
        }

        verify(exactly = 1) { mockKafkaConsumerDialogmoteStatusendring.commitSync() }

        val pPersonOversiktStatus =
            personOversiktStatusRepository.getPersonOversiktStatus(personident = PersonIdent(UserConstants.ARBEIDSTAKER_FNR))

        assertNotNull(pPersonOversiktStatus)

        assertEquals(kafkaDialogmoteStatusendringToday.getPersonIdent(), pPersonOversiktStatus?.fnr)
        assertEquals(kafkaDialogmoteStatusendringToday.getStatusEndringType(), pPersonOversiktStatus?.motestatus)
        assertNotNull(pPersonOversiktStatus?.motestatusGeneratedAt)
    }
}
