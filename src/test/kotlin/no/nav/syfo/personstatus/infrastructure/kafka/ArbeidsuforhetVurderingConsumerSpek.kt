package no.nav.syfo.personstatus.infrastructure.kafka

import io.mockk.*
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.personstatus.PersonoversiktStatusService
import no.nav.syfo.personstatus.db.createPersonOversiktStatus
import no.nav.syfo.personstatus.db.getPersonOversiktStatusList
import no.nav.syfo.personstatus.domain.PersonOversiktStatus
import no.nav.syfo.personstatus.infrastructure.database.PersonOversiktStatusRepository
import no.nav.syfo.testutil.ExternalMockEnvironment
import no.nav.syfo.testutil.UserConstants
import no.nav.syfo.testutil.dropData
import org.amshove.kluent.shouldBeEqualTo
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class ArbeidsuforhetVurderingConsumerSpek : Spek({
    val externalMockEnvironment = ExternalMockEnvironment.instance
    val database = externalMockEnvironment.database
    val kafkaConsumer = mockk<KafkaConsumer<String, ArbeidsuforhetVurderingRecord>>()
    val personOppgaveRepository = PersonOversiktStatusRepository(database = database)
    val personoversiktStatusService = PersonoversiktStatusService(
        database = database,
        pdlClient = externalMockEnvironment.pdlClient,
        personoversiktStatusRepository = personOppgaveRepository
    )

    val arbeidsuforhetVurderingConsumer = ArbeidsuforhetVurderingConsumer(
        personoversiktStatusService = personoversiktStatusService,
    )
    val arbeidsuforhetVurderingRecord = generateArbeidsvurderingRecord(
        personIdent = PersonIdent(UserConstants.ARBEIDSTAKER_FNR),
        createdAt = OffsetDateTime.now(),
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
            val newPersonOversiktStatus = PersonOversiktStatus(fnr = UserConstants.ARBEIDSTAKER_FNR)
            database.connection.use { connection ->
                connection.createPersonOversiktStatus(
                    commit = true,
                    personOversiktStatus = newPersonOversiktStatus,
                )
            }

            kafkaConsumer.mockPollConsumerRecords(
                recordValue = arbeidsuforhetVurderingRecord,
                topic = "teamsykefravr.arbeidsuforhet-vurdering",
            )

            arbeidsuforhetVurderingConsumer.pollAndProcessRecords(kafkaConsumer = kafkaConsumer)

            verify(exactly = 1) {
                kafkaConsumer.commitSync()
            }
            val personStatus = database.getPersonOversiktStatusList(fnr = UserConstants.ARBEIDSTAKER_FNR).single()
            personStatus.fnr shouldBeEqualTo arbeidsuforhetVurderingRecord.personident
        }
    }
})

private fun generateArbeidsvurderingRecord(
    personIdent: PersonIdent,
    createdAt: OffsetDateTime,
    type: VurderingType = VurderingType.OPPFYLT,
    begrunnelse: String = "En kjempegod begrunnelse",
    gjelderFom: LocalDate? = LocalDate.now().plusDays(1),
    isFinalVurdering: Boolean = true,
): ArbeidsuforhetVurderingRecord = ArbeidsuforhetVurderingRecord(
    uuid = UUID.randomUUID(),
    createdAt = createdAt,
    personident = personIdent.value,
    veilederident = "Z123456",
    type = type,
    begrunnelse = begrunnelse,
    gjelderFom = gjelderFom,
    isFinalVurdering = isFinalVurdering,
)
