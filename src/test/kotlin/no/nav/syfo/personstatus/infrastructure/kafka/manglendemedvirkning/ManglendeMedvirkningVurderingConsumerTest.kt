package no.nav.syfo.personstatus.infrastructure.kafka.manglendemedvirkning

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.syfo.personstatus.domain.PersonIdent
import no.nav.syfo.personstatus.domain.PersonOversiktStatus
import no.nav.syfo.personstatus.infrastructure.database.queries.createPersonOversiktStatus
import no.nav.syfo.personstatus.infrastructure.kafka.mockPollConsumerRecords
import no.nav.syfo.testutil.ExternalMockEnvironment
import no.nav.syfo.testutil.UserConstants
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertFalse
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

class ManglendeMedvirkningVurderingConsumerTest {
    private val externalMockEnvironment = ExternalMockEnvironment.instance
    private val database = externalMockEnvironment.database
    private val kafkaConsumer = mockk<KafkaConsumer<String, VurderingRecord>>()
    private val personOversiktStatusRepository = externalMockEnvironment.personOversiktStatusRepository
    private val personoversiktStatusService = externalMockEnvironment.personoversiktStatusService

    private val manglendeMedvirkningConsumer = ManglendeMedvirkningVurderingConsumer(
        personoversiktStatusService = personoversiktStatusService,
    )

    private val personident = PersonIdent(UserConstants.ARBEIDSTAKER_FNR)
    private val forhandsvarselVurderingRecord = VurderingRecord(
        uuid = UUID.randomUUID(),
        personident = personident.value,
        veilederident = UserConstants.VEILEDER_ID,
        createdAt = OffsetDateTime.now(),
        begrunnelse = "begrunnelse",
        varsel = Varsel(
            uuid = UUID.randomUUID(),
            createdAt = OffsetDateTime.now(),
            svarfrist = LocalDate.now().plusWeeks(3),
        ),
        vurderingType = VurderingTypeDTO(
            value = VurderingType.FORHANDSVARSEL,
            isActive = true,
        )
    )

    @BeforeEach
    fun setUp() {
        database.resetDatabase()
        every { kafkaConsumer.commitSync() } returns Unit
    }

    @AfterEach
    fun tearDown() {
        database.resetDatabase()
        clearMocks(kafkaConsumer)
    }

    @Test
    fun `Creates new personoversiktStatus when person not in database`() {
        kafkaConsumer.mockPollConsumerRecords(
            recordValue = forhandsvarselVurderingRecord,
            topic = "teamsykefravr.manglende-medvirkning-vurdering",
        )

        runBlocking { manglendeMedvirkningConsumer.pollAndProcessRecords(kafkaConsumer) }

        verify(exactly = 1) {
            kafkaConsumer.commitSync()
        }

        val personOversiktStatus = personOversiktStatusRepository.getPersonOversiktStatus(personident)
        assertTrue(personOversiktStatus?.isAktivManglendeMedvirkningVurdering == true)
        assertEquals(personident.value, personOversiktStatus?.fnr)
    }

    @Test
    fun `Updates is_aktiv_manglende_medvirkning_vurdering to false when not active vurdering`() {
        val oppfyltVurderingRecord = forhandsvarselVurderingRecord.copy(
            vurderingType = VurderingTypeDTO(
                value = VurderingType.OPPFYLT,
                isActive = false,
            ),
            varsel = null,
        )
        val personoversiktStatus = PersonOversiktStatus(
            fnr = personident.value,
            isAktivManglendeMedvirkningVurdering = true,
        )
        database.connection.use { connection ->
            connection.createPersonOversiktStatus(
                commit = true,
                personOversiktStatus = personoversiktStatus,
            )
        }

        kafkaConsumer.mockPollConsumerRecords(
            recordValue = oppfyltVurderingRecord,
            topic = "teamsykefravr.manglende-medvirkning-vurdering",
        )

        runBlocking { manglendeMedvirkningConsumer.pollAndProcessRecords(kafkaConsumer) }

        verify(exactly = 1) {
            kafkaConsumer.commitSync()
        }

        val updatedPersonOversiktStatus = personOversiktStatusRepository.getPersonOversiktStatus(personident)
        assertFalse(updatedPersonOversiktStatus?.isAktivManglendeMedvirkningVurdering == true)
        assertEquals(personident.value, updatedPersonOversiktStatus?.fnr)
    }

    @Test
    fun `Updates is_aktiv_manglende_medvirkning_vurdering to true when active vurdering`() {
        val personoversiktStatus = PersonOversiktStatus(
            fnr = personident.value,
            isAktivManglendeMedvirkningVurdering = false,
        )
        database.connection.use { connection ->
            connection.createPersonOversiktStatus(
                commit = true,
                personOversiktStatus = personoversiktStatus,
            )
        }

        kafkaConsumer.mockPollConsumerRecords(
            recordValue = forhandsvarselVurderingRecord,
            topic = "teamsykefravr.manglende-medvirkning-vurdering",
        )

        runBlocking { manglendeMedvirkningConsumer.pollAndProcessRecords(kafkaConsumer) }

        verify(exactly = 1) {
            kafkaConsumer.commitSync()
        }

        val updatedPersonOversiktStatus = personOversiktStatusRepository.getPersonOversiktStatus(personident)
        assertTrue(updatedPersonOversiktStatus?.isAktivManglendeMedvirkningVurdering == true)
        assertEquals(personident.value, updatedPersonOversiktStatus?.fnr)
    }
}
