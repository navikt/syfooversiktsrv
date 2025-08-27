package no.nav.syfo.personstatus.infrastructure.kafka

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.syfo.personstatus.domain.PersonIdent
import no.nav.syfo.personstatus.domain.PersonOversiktStatus
import no.nav.syfo.personstatus.infrastructure.database.queries.createPersonOversiktStatus
import no.nav.syfo.personstatus.infrastructure.database.queries.getPersonOversiktStatusList
import no.nav.syfo.testutil.ExternalMockEnvironment
import no.nav.syfo.testutil.UserConstants
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

class ArbeidsuforhetvurderingConsumerTest {
    private val externalMockEnvironment = ExternalMockEnvironment.instance
    private val database = externalMockEnvironment.database
    private val kafkaConsumer = mockk<KafkaConsumer<String, ArbeidsuforhetvurderingRecord>>()
    private val personoversiktStatusService = externalMockEnvironment.personoversiktStatusService

    private val arbeidsuforhetvurderingConsumer = ArbeidsuforhetvurderingConsumer(
        personoversiktStatusService = personoversiktStatusService,
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
    fun `Consumes arbeidsuforhet vurdering and updates personoversikt status`() {
        val personoversiktStatus =
            PersonOversiktStatus(fnr = UserConstants.ARBEIDSTAKER_FNR, isAktivArbeidsuforhetvurdering = true)
        database.connection.use { connection ->
            connection.createPersonOversiktStatus(
                commit = true,
                personOversiktStatus = personoversiktStatus,
            )
        }

        val arbeidsuforhetvurderingRecord = generateArbeidsvurderingRecord(
            personIdent = PersonIdent(UserConstants.ARBEIDSTAKER_FNR),
        )
        kafkaConsumer.mockPollConsumerRecords(
            recordValue = arbeidsuforhetvurderingRecord,
            topic = "teamsykefravr.arbeidsuforhet-vurdering",
        )

        runBlocking {
            arbeidsuforhetvurderingConsumer.pollAndProcessRecords(kafkaConsumer = kafkaConsumer)
        }

        verify(exactly = 1) {
            kafkaConsumer.commitSync()
        }
        val updatedPersonstatus = database.getPersonOversiktStatusList(fnr = UserConstants.ARBEIDSTAKER_FNR).single()
        assertEquals(arbeidsuforhetvurderingRecord.personident, updatedPersonstatus.fnr)

        assertNotEquals(personoversiktStatus.isAktivArbeidsuforhetvurdering, updatedPersonstatus.isAktivArbeidsuforhetvurdering)
        assertFalse(updatedPersonstatus.isAktivArbeidsuforhetvurdering)
    }
}

private fun generateArbeidsvurderingRecord(
    personIdent: PersonIdent,
): ArbeidsuforhetvurderingRecord = ArbeidsuforhetvurderingRecord(
    uuid = UUID.randomUUID(),
    personident = personIdent.value,
    isFinal = true,
)
