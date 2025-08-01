package no.nav.syfo.personstatus.infrastructure.kafka.meroppfolging

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.syfo.personstatus.domain.PersonOversiktStatus
import no.nav.syfo.personstatus.infrastructure.database.queries.createPersonOversiktStatus
import no.nav.syfo.personstatus.infrastructure.database.queries.getPersonOversiktStatusList
import no.nav.syfo.personstatus.infrastructure.kafka.mockPollConsumerRecords
import no.nav.syfo.testutil.ExternalMockEnvironment
import no.nav.syfo.testutil.UserConstants
import no.nav.syfo.testutil.dropData
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import java.time.OffsetDateTime
import java.util.*

class SenOppfolgingKandidatStatusConsumerTest {
    private val externalMockEnvironment = ExternalMockEnvironment.instance
    private val database = externalMockEnvironment.database
    private val kafkaConsumer = mockk<KafkaConsumer<String, KandidatStatusRecord>>()
    private val personoversiktStatusService = externalMockEnvironment.personoversiktStatusService

    private val senOppfolgingKandidatStatusConsumer = SenOppfolgingKandidatStatusConsumer(
        personoversiktStatusService = personoversiktStatusService,
    )

    private val kandidatStatusRecord = KandidatStatusRecord(
        uuid = UUID.randomUUID(),
        createdAt = OffsetDateTime.now(),
        personident = UserConstants.ARBEIDSTAKER_FNR,
        status = StatusDTO(
            value = Status.KANDIDAT,
            isActive = true,
        ),
    )

    @BeforeEach
    fun setUp() {
        every { kafkaConsumer.commitSync() } returns Unit
    }

    @AfterEach
    fun tearDown() {
        database.dropData()
        clearMocks(kafkaConsumer)
    }

    @Test
    fun `Consumes sen oppfolging kandidat status and creates personoversikt status`() {
        kafkaConsumer.mockPollConsumerRecords(
            recordValue = kandidatStatusRecord,
            topic = SenOppfolgingKandidatStatusConsumer.SEN_OPPFOLGING_KANDIDAT_STATUS_TOPIC,
        )

        runBlocking { senOppfolgingKandidatStatusConsumer.pollAndProcessRecords(kafkaConsumer) }

        verify(exactly = 1) {
            kafkaConsumer.commitSync()
        }
        val pPersonstatus = database.getPersonOversiktStatusList(fnr = UserConstants.ARBEIDSTAKER_FNR).single()
        assertEquals(kandidatStatusRecord.personident, pPersonstatus.fnr)
        assertTrue(pPersonstatus.isAktivSenOppfolgingKandidat)
    }

    @Test
    fun `Consumes sen oppfolging kandidat status and updates personoversikt status`() {
        val personoversiktStatus = PersonOversiktStatus(fnr = UserConstants.ARBEIDSTAKER_FNR)
        database.connection.use { connection ->
            connection.createPersonOversiktStatus(
                commit = true,
                personOversiktStatus = personoversiktStatus,
            )
        }

        kafkaConsumer.mockPollConsumerRecords(
            recordValue = kandidatStatusRecord,
            topic = SenOppfolgingKandidatStatusConsumer.SEN_OPPFOLGING_KANDIDAT_STATUS_TOPIC,
        )

        runBlocking { senOppfolgingKandidatStatusConsumer.pollAndProcessRecords(kafkaConsumer) }

        verify(exactly = 1) {
            kafkaConsumer.commitSync()
        }
        val updatedPersonstatus = database.getPersonOversiktStatusList(fnr = UserConstants.ARBEIDSTAKER_FNR).single()
        assertEquals(kandidatStatusRecord.personident, updatedPersonstatus.fnr)
        assertTrue(updatedPersonstatus.isAktivSenOppfolgingKandidat)
    }

    @Test
    fun `Consumes sen oppfolging kandidat status FERDIGBEHANDLET and updates personoversikt status`() {
        val kandidatStatusFerdigbehandletRecord = KandidatStatusRecord(
            uuid = UUID.randomUUID(),
            createdAt = OffsetDateTime.now(),
            personident = UserConstants.ARBEIDSTAKER_FNR,
            status = StatusDTO(
                value = Status.FERDIGBEHANDLET,
                isActive = false,
            ),
        )
        val personoversiktStatus = PersonOversiktStatus(fnr = UserConstants.ARBEIDSTAKER_FNR)
        database.connection.use { connection ->
            connection.createPersonOversiktStatus(
                commit = true,
                personOversiktStatus = personoversiktStatus,
            )
        }

        kafkaConsumer.mockPollConsumerRecords(
            recordValue = kandidatStatusFerdigbehandletRecord,
            topic = SenOppfolgingKandidatStatusConsumer.SEN_OPPFOLGING_KANDIDAT_STATUS_TOPIC,
        )

        runBlocking { senOppfolgingKandidatStatusConsumer.pollAndProcessRecords(kafkaConsumer) }

        verify(exactly = 1) {
            kafkaConsumer.commitSync()
        }
        val updatedPersonstatus = database.getPersonOversiktStatusList(fnr = UserConstants.ARBEIDSTAKER_FNR).single()
        assertEquals(kandidatStatusFerdigbehandletRecord.personident, updatedPersonstatus.fnr)
        assertFalse(updatedPersonstatus.isAktivSenOppfolgingKandidat)
    }
}
