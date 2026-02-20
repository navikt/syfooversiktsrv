package no.nav.syfo.infrastructure.kafka.meroppfolging

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.domain.PersonOversiktStatus
import no.nav.syfo.infrastructure.kafka.kartleggingssporsmal.KartleggingssporsmalKandidatStatusConsumer
import no.nav.syfo.infrastructure.kafka.kartleggingssporsmal.KartleggingssporsmalKandidatStatusRecord
import no.nav.syfo.infrastructure.kafka.mockPollConsumerRecords
import no.nav.syfo.testutil.ExternalMockEnvironment
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_FNR
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import java.time.OffsetDateTime
import java.util.*

class KartlegginssporsmalKandidatStatusConsumerTest {
    private val externalMockEnvironment = ExternalMockEnvironment.instance
    private val database = externalMockEnvironment.database
    private val kafkaConsumer = mockk<KafkaConsumer<String, KartleggingssporsmalKandidatStatusRecord>>()
    private val personoversiktStatusService = externalMockEnvironment.personoversiktStatusService
    private val personOversiktStatusRepository = externalMockEnvironment.personOversiktStatusRepository

    private val kartleggingssporsmalKandidatStatusConsumer = KartleggingssporsmalKandidatStatusConsumer(
        personoversiktStatusService = personoversiktStatusService,
    )

    private val kandidatStatusSvarMottattRecord = KartleggingssporsmalKandidatStatusRecord(
        kandidatUuid = UUID.randomUUID(),
        createdAt = OffsetDateTime.now(),
        personident = ARBEIDSTAKER_FNR,
        status = "SVAR_MOTTATT",
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
    fun `Consumes kartleggingssporsmal kandidatstatus and creates new personstatus`() {
        kafkaConsumer.mockPollConsumerRecords(
            recordValue = kandidatStatusSvarMottattRecord,
            topic = KartleggingssporsmalKandidatStatusConsumer.KARTLEGGINGSSPORSMAL_KANDIDAT_STATUS_TOPIC,
        )

        runBlocking { kartleggingssporsmalKandidatStatusConsumer.pollAndProcessRecords(kafkaConsumer) }

        verify(exactly = 1) { kafkaConsumer.commitSync() }
        val personstatus = personOversiktStatusRepository.getPersonOversiktStatus(
            personident = PersonIdent(ARBEIDSTAKER_FNR),
        )
        assertNotNull(personstatus)
        assertEquals(kandidatStatusSvarMottattRecord.personident, personstatus.fnr)
        assertTrue(personstatus.isAktivKartleggingssporsmalVurdering)
    }

    @Test
    fun `Consumes kartleggingssporsmal kandidatstatus and updates existing personstatus`() {
        val personoversiktStatus = PersonOversiktStatus(fnr = ARBEIDSTAKER_FNR)
        personOversiktStatusRepository.createPersonOversiktStatus(personoversiktStatus)

        kafkaConsumer.mockPollConsumerRecords(
            recordValue = kandidatStatusSvarMottattRecord,
            topic = KartleggingssporsmalKandidatStatusConsumer.KARTLEGGINGSSPORSMAL_KANDIDAT_STATUS_TOPIC,
        )

        runBlocking { kartleggingssporsmalKandidatStatusConsumer.pollAndProcessRecords(kafkaConsumer) }

        verify(exactly = 1) { kafkaConsumer.commitSync() }
        val personstatus = personOversiktStatusRepository.getPersonOversiktStatus(
            personident = PersonIdent(ARBEIDSTAKER_FNR),
        )
        assertNotNull(personstatus)
        assertEquals(kandidatStatusSvarMottattRecord.personident, personstatus.fnr)
        assertTrue(personstatus.isAktivKartleggingssporsmalVurdering)
    }

    @Test
    fun `Consumes kartleggingssporsmal kandidatstatus KANDIDAT, and personstatus is still not true`() {
        val kandidatStatusKandidatRecord = kandidatStatusSvarMottattRecord.copy(
            status = "KANDIDAT",
        )
        val personoversiktStatus = PersonOversiktStatus(fnr = ARBEIDSTAKER_FNR)
        personOversiktStatusRepository.createPersonOversiktStatus(personoversiktStatus)

        kafkaConsumer.mockPollConsumerRecords(
            recordValue = kandidatStatusKandidatRecord,
            topic = KartleggingssporsmalKandidatStatusConsumer.KARTLEGGINGSSPORSMAL_KANDIDAT_STATUS_TOPIC,
        )

        runBlocking { kartleggingssporsmalKandidatStatusConsumer.pollAndProcessRecords(kafkaConsumer) }

        verify(exactly = 1) { kafkaConsumer.commitSync() }

        val personstatus = personOversiktStatusRepository.getPersonOversiktStatus(
            personident = PersonIdent(ARBEIDSTAKER_FNR),
        )
        assertNotNull(personstatus)
        assertEquals(kandidatStatusSvarMottattRecord.personident, personstatus.fnr)
        assertFalse(personstatus.isAktivKartleggingssporsmalVurdering)
    }

    @Test
    fun `Consumes kartleggingssporsmal kandidatstatus FERDIG_BEHANDLET and updates personoversikt status from true to false`() {
        val kandidatStatusFerdigbehandletRecord = kandidatStatusSvarMottattRecord.copy(
            status = "FERDIG_BEHANDLET",
        )
        val personoversiktStatus = PersonOversiktStatus(
            fnr = ARBEIDSTAKER_FNR,
            isAktivKartleggingssporsmalVurdering = true,
        )
        personOversiktStatusRepository.createPersonOversiktStatus(personoversiktStatus)

        kafkaConsumer.mockPollConsumerRecords(
            recordValue = kandidatStatusFerdigbehandletRecord,
            topic = KartleggingssporsmalKandidatStatusConsumer.KARTLEGGINGSSPORSMAL_KANDIDAT_STATUS_TOPIC,
        )

        runBlocking { kartleggingssporsmalKandidatStatusConsumer.pollAndProcessRecords(kafkaConsumer) }

        verify(exactly = 1) { kafkaConsumer.commitSync() }

        val personstatus = personOversiktStatusRepository.getPersonOversiktStatus(
            personident = PersonIdent(ARBEIDSTAKER_FNR),
        )
        assertNotNull(personstatus)
        assertEquals(kandidatStatusSvarMottattRecord.personident, personstatus.fnr)
        assertFalse(personstatus.isAktivKartleggingssporsmalVurdering)
    }
}
