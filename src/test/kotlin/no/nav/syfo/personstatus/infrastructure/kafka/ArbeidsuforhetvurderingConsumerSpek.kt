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
import no.nav.syfo.testutil.dropData
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEqualTo
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.junit.jupiter.api.*
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class ArbeidsuforhetvurderingConsumerTest {
    private val externalMockEnvironment = ExternalMockEnvironment.instance
    private val database = externalMockEnvironment.database
    private val kafkaConsumer = mockk<KafkaConsumer<String, ArbeidsuforhetvurderingRecord>>()
    private val personoversiktStatusService = externalMockEnvironment.personoversiktStatusService
    private val arbeidsuforhetvurderingConsumer = ArbeidsuforhetvurderingConsumer(
        personoversiktStatusService = personoversiktStatusService,
    )

    @BeforeEach
    fun setup() {
        database.dropData()
        every { kafkaConsumer.commitSync() } returns Unit
    }

    @AfterEach
    fun teardown() {
        database.dropData()
        clearMocks(kafkaConsumer)
    }

    @Test
    fun `consumes arbeidsuforhet vurdering and updates personoversikt status`() {
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
            createdAt = OffsetDateTime.now(),
            isFinal = true,
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
        updatedPersonstatus.fnr shouldBeEqualTo arbeidsuforhetvurderingRecord.personident

        personoversiktStatus.isAktivArbeidsuforhetvurdering shouldNotBeEqualTo updatedPersonstatus.isAktivArbeidsuforhetvurdering
        updatedPersonstatus.isAktivArbeidsuforhetvurdering shouldBe false
    }
}

private fun generateArbeidsvurderingRecord(
    personIdent: PersonIdent,
    createdAt: OffsetDateTime,
    type: VurderingType = VurderingType.OPPFYLT,
    begrunnelse: String = "En kjempegod begrunnelse",
    gjelderFom: LocalDate? = LocalDate.now().plusDays(1),
    isFinal: Boolean = true,
): ArbeidsuforhetvurderingRecord = ArbeidsuforhetvurderingRecord(
    uuid = UUID.randomUUID(),
    createdAt = createdAt,
    personident = personIdent.value,
    veilederident = "Z123456",
    type = type,
    begrunnelse = begrunnelse,
    gjelderFom = gjelderFom,
    isFinal = isFinal,
)
