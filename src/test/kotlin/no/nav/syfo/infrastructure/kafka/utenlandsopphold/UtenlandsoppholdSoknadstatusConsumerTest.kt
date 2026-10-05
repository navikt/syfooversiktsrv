package no.nav.syfo.infrastructure.kafka.utenlandsopphold

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.infrastructure.kafka.mockPollConsumerRecords
import no.nav.syfo.testutil.ExternalMockEnvironment
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_FNR
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class UtenlandsoppholdSoknadstatusConsumerTest {
    private val externalMockEnvironment = ExternalMockEnvironment.instance
    private val database = externalMockEnvironment.database
    private val personoversiktStatusRepository = externalMockEnvironment.personOversiktStatusRepository
    private val kafkaConsumer = mockk<KafkaConsumer<String, UtenlandsoppholdSoknadstatusRecord>>()
    private val consumer = UtenlandsoppholdSoknadstatusConsumer(
        personoversiktStatusService = externalMockEnvironment.personoversiktStatusService,
    )

    @BeforeEach
    fun setUp() {
        every { kafkaConsumer.commitSync() } returns Unit
    }

    @AfterEach
    fun tearDown() {
        database.resetDatabase()
        clearMocks(kafkaConsumer)
    }

    @Test
    fun `MOTTATT creates person with active utenlandsopphold soknad`() {
        val record = mottattRecord()
        mockRecord(record)

        runBlocking { consumer.pollAndProcessRecords(kafkaConsumer) }

        verify(exactly = 1) { kafkaConsumer.commitSync() }
        val personstatus = personoversiktStatusRepository.getPersonOversiktStatus(PersonIdent(ARBEIDSTAKER_FNR))!!
        assertEquals(listOf(record.uuid), personstatus.utenlandsoppholdSoknadUbehandletUuids)
        assertTrue(personstatus.utenlandsoppholdSoknadUbehandlet)
    }

    @Test
    fun `Two MOTTATT and one BEHANDLET keeps status active`() {
        val firstRecord = mottattRecord()
        val secondRecord = mottattRecord()
        mockRecord(firstRecord, secondRecord)
        runBlocking { consumer.pollAndProcessRecords(kafkaConsumer) }

        mockRecord(firstRecord.copy(status = UtenlandsoppholdSoknadstatus.BEHANDLET))
        runBlocking { consumer.pollAndProcessRecords(kafkaConsumer) }

        verify(exactly = 2) { kafkaConsumer.commitSync() }
        val personstatus = personoversiktStatusRepository.getPersonOversiktStatus(PersonIdent(ARBEIDSTAKER_FNR))!!
        assertEquals(listOf(secondRecord.uuid), personstatus.utenlandsoppholdSoknadUbehandletUuids)
        assertTrue(personstatus.utenlandsoppholdSoknadUbehandlet)
    }

    @Test
    fun `BEHANDLET for all soknader clears status`() {
        val firstRecord = mottattRecord()
        val secondRecord = mottattRecord()
        mockRecord(firstRecord, secondRecord)
        runBlocking { consumer.pollAndProcessRecords(kafkaConsumer) }

        mockRecord(
            firstRecord.copy(status = UtenlandsoppholdSoknadstatus.BEHANDLET),
            secondRecord.copy(status = UtenlandsoppholdSoknadstatus.BEHANDLET),
        )
        runBlocking { consumer.pollAndProcessRecords(kafkaConsumer) }

        val personstatus = personoversiktStatusRepository.getPersonOversiktStatus(PersonIdent(ARBEIDSTAKER_FNR))!!
        assertTrue(personstatus.utenlandsoppholdSoknadUbehandletUuids.isEmpty())
        assertFalse(personstatus.utenlandsoppholdSoknadUbehandlet)
    }

    @Test
    fun `Duplicate MOTTATT is idempotent`() {
        val record = mottattRecord()
        mockRecord(record, record)

        runBlocking { consumer.pollAndProcessRecords(kafkaConsumer) }

        val personstatus = personoversiktStatusRepository.getPersonOversiktStatus(PersonIdent(ARBEIDSTAKER_FNR))!!
        assertEquals(listOf(record.uuid), personstatus.utenlandsoppholdSoknadUbehandletUuids)
    }

    @Test
    fun `BEHANDLET for unknown person is no-op`() {
        mockRecord(mottattRecord().copy(status = UtenlandsoppholdSoknadstatus.BEHANDLET))

        runBlocking { consumer.pollAndProcessRecords(kafkaConsumer) }

        verify(exactly = 1) { kafkaConsumer.commitSync() }
        assertNull(personoversiktStatusRepository.getPersonOversiktStatus(PersonIdent(ARBEIDSTAKER_FNR)))
    }

    private fun mockRecord(
        record: UtenlandsoppholdSoknadstatusRecord,
        secondRecord: UtenlandsoppholdSoknadstatusRecord? = null,
    ) {
        kafkaConsumer.mockPollConsumerRecords(
            recordValue = record,
            recordValue2 = secondRecord,
            topic = UtenlandsoppholdSoknadstatusConsumer.UTENLANDSOPPHOLD_SOKNAD_STATUS_TOPIC,
        )
    }

    private fun mottattRecord() =
        UtenlandsoppholdSoknadstatusRecord(
            uuid = UUID.randomUUID(),
            personident = ARBEIDSTAKER_FNR,
            status = UtenlandsoppholdSoknadstatus.MOTTATT,
        )
}
