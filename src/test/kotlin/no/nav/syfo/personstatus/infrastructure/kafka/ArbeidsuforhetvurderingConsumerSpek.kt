package no.nav.syfo.personstatus.infrastructure.kafka

import io.mockk.*
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.syfo.personstatus.domain.PersonIdent
import no.nav.syfo.personstatus.PersonoversiktStatusService
import no.nav.syfo.personstatus.application.IAktivitetskravClient
import no.nav.syfo.personstatus.application.arbeidsuforhet.IArbeidsuforhetvurderingClient
import no.nav.syfo.personstatus.application.manglendemedvirkning.IManglendeMedvirkningClient
import no.nav.syfo.personstatus.application.oppfolgingsoppgave.IOppfolgingsoppgaveClient
import no.nav.syfo.personstatus.db.createPersonOversiktStatus
import no.nav.syfo.personstatus.db.getPersonOversiktStatusList
import no.nav.syfo.personstatus.domain.PersonOversiktStatus
import no.nav.syfo.personstatus.infrastructure.database.repository.PersonOversiktStatusRepository
import no.nav.syfo.testutil.ExternalMockEnvironment
import no.nav.syfo.testutil.UserConstants
import no.nav.syfo.testutil.dropData
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEqualTo
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class ArbeidsuforhetvurderingConsumerSpek : Spek({
    val externalMockEnvironment = ExternalMockEnvironment.instance
    val database = externalMockEnvironment.database
    val kafkaConsumer = mockk<KafkaConsumer<String, ArbeidsuforhetvurderingRecord>>()
    val personOppgaveRepository = PersonOversiktStatusRepository(database = database)
    val arbeidsuforhervurderingClient = mockk<IArbeidsuforhetvurderingClient>()
    val oppfolgingsoppgaveClient = mockk<IOppfolgingsoppgaveClient>()
    val personoversiktStatusService = PersonoversiktStatusService(
        database = database,
        pdlClient = externalMockEnvironment.pdlClient,
        arbeidsuforhetvurderingClient = arbeidsuforhervurderingClient,
        manglendeMedvirkningClient = mockk<IManglendeMedvirkningClient>(),
        personoversiktStatusRepository = personOppgaveRepository,
        oppfolgingsoppgaveClient = oppfolgingsoppgaveClient,
        aktivitetskravClient = mockk<IAktivitetskravClient>(),
    )

    val arbeidsuforhetvurderingConsumer = ArbeidsuforhetvurderingConsumer(
        personoversiktStatusService = personoversiktStatusService,
    )
    beforeEachGroup { database.dropData() }
    beforeEachTest {
        every { kafkaConsumer.commitSync() } returns Unit
    }

    afterEachTest {
        database.dropData()
        clearMocks(kafkaConsumer)
    }

    describe("pollAndProcessRecords") {
        it("consumes arbeidsuforhet vurdering and updates personoversikt status") {
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

            arbeidsuforhetvurderingConsumer.pollAndProcessRecords(kafkaConsumer = kafkaConsumer)

            verify(exactly = 1) {
                kafkaConsumer.commitSync()
            }
            val updatedPersonstatus = database.getPersonOversiktStatusList(fnr = UserConstants.ARBEIDSTAKER_FNR).single()
            updatedPersonstatus.fnr shouldBeEqualTo arbeidsuforhetvurderingRecord.personident

            personoversiktStatus.isAktivArbeidsuforhetvurdering shouldNotBeEqualTo updatedPersonstatus.isAktivArbeidsuforhetvurdering
            updatedPersonstatus.isAktivArbeidsuforhetvurdering shouldBe false
        }
    }
})

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
