package no.nav.syfo.personstatus.infrastructure.kafka

import io.mockk.*
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
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
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.util.UUID

class ArbeidsuforhetvurderingConsumerSpek : Spek({
    val externalMockEnvironment = ExternalMockEnvironment.instance
    val database = externalMockEnvironment.database
    val kafkaConsumer = mockk<KafkaConsumer<String, ArbeidsuforhetvurderingRecord>>()
    val personoversiktStatusService = externalMockEnvironment.personoversiktStatusService

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
})

private fun generateArbeidsvurderingRecord(
    personIdent: PersonIdent,
): ArbeidsuforhetvurderingRecord = ArbeidsuforhetvurderingRecord(
    uuid = UUID.randomUUID(),
    personident = personIdent.value,
    isFinal = true,
)
