package no.nav.syfo.dialogmotestatusendring.kafka

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.syfo.dialogmotestatusendring.domain.DialogmoteStatusendring
import no.nav.syfo.dialogmotestatusendring.domain.DialogmoteStatusendringType
import no.nav.syfo.personstatus.infrastructure.database.queries.getPersonOversiktStatusList
import no.nav.syfo.testutil.*
import no.nav.syfo.testutil.generator.dialogmoteStatusendringConsumerRecord
import no.nav.syfo.testutil.generator.dialogmoteStatusendringTopicPartition
import no.nav.syfo.testutil.generator.generateKafkaDialogmoteStatusendring
import no.nav.syfo.testutil.generator.generateKafkaOppfolgingstilfellePerson
import no.nav.syfo.util.nowUTC
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration

class KafkaDialogmoteStatusendringServiceTest {

    private val externalMockEnvironment = ExternalMockEnvironment.instance
    private val database = externalMockEnvironment.database

    private val kafkaDialogmoteStatusendringService = TestKafkaModule.kafkaDialogmoteStatusendringService
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
        database.dropData()

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

        val pPersonOversiktStatusList =
            database.connection.getPersonOversiktStatusList(UserConstants.ARBEIDSTAKER_FNR)

        assertEquals(1, pPersonOversiktStatusList.size)

        val pPersonOversiktStatus = pPersonOversiktStatusList.first()

        assertEquals(kafkaDialogmoteStatusendringToday.getPersonIdent(), pPersonOversiktStatus.fnr)
        assertEquals(kafkaDialogmoteStatusendringToday.getStatusEndringType(), pPersonOversiktStatus.motestatus)
        assertNotNull(pPersonOversiktStatus.motestatusGeneratedAt)

        assertNull(pPersonOversiktStatus.dialogmotekandidat)
        assertNull(pPersonOversiktStatus.enhet)
        assertNull(pPersonOversiktStatus.veilederIdent)
        assertNull(pPersonOversiktStatus.motebehovUbehandlet)
        assertNull(pPersonOversiktStatus.oppfolgingsplanLPSBistandUbehandlet)
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

        val pPersonOversiktStatusList =
            database.connection.getPersonOversiktStatusList(UserConstants.ARBEIDSTAKER_FNR)

        assertEquals(1, pPersonOversiktStatusList.size)
        val pPersonOversiktStatus = pPersonOversiktStatusList.first()
        assertNotNull(pPersonOversiktStatus.oppfolgingstilfelleGeneratedAt)
        assertNotNull(pPersonOversiktStatus.oppfolgingstilfelleBitReferanseUuid)

        assertEquals(kafkaDialogmoteStatusendringToday.getPersonIdent(), pPersonOversiktStatus.fnr)
        assertEquals(kafkaDialogmoteStatusendringToday.getStatusEndringType(), pPersonOversiktStatus.motestatus)
        assertNotNull(pPersonOversiktStatus.motestatusGeneratedAt)
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

        val existingPersonOversiktStatus = DialogmoteStatusendring.create(
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

        val pPersonOversiktStatusList =
            database.connection.getPersonOversiktStatusList(UserConstants.ARBEIDSTAKER_FNR)

        assertEquals(1, pPersonOversiktStatusList.size)
        val pPersonOversiktStatus = pPersonOversiktStatusList.first()

        assertEquals(kafkaDialogmoteStatusendringToday.getPersonIdent(), pPersonOversiktStatus.fnr)
        assertEquals(kafkaDialogmoteStatusendringToday.getStatusEndringType(), pPersonOversiktStatus.motestatus)
        assertNotNull(pPersonOversiktStatus.motestatusGeneratedAt)
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

        val existingPersonOversiktStatus = DialogmoteStatusendring.create(
            kafkaDialogmoteStatusEndring = kafkaDialogmoteStatusendringToday
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

        val pPersonOversiktStatusList =
            database.connection.getPersonOversiktStatusList(UserConstants.ARBEIDSTAKER_FNR)

        assertEquals(1, pPersonOversiktStatusList.size)
        val pPersonOversiktStatus = pPersonOversiktStatusList.first()

        assertEquals(kafkaDialogmoteStatusendringToday.getPersonIdent(), pPersonOversiktStatus.fnr)
        assertEquals(kafkaDialogmoteStatusendringToday.getStatusEndringType(), pPersonOversiktStatus.motestatus)
        assertNotNull(pPersonOversiktStatus.motestatusGeneratedAt)
    }
}
