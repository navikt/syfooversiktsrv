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
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.OffsetDateTime
import java.util.*

class SenOppfolgingKandidatStatusConsumerSpek : Spek({
    val externalMockEnvironment = ExternalMockEnvironment.instance
    val database = externalMockEnvironment.database
    val kafkaConsumer = mockk<KafkaConsumer<String, KandidatStatusRecord>>()
    val personoversiktStatusService = externalMockEnvironment.personoversiktStatusService

    val senOppfolgingKandidatStatusConsumer = SenOppfolgingKandidatStatusConsumer(
        personoversiktStatusService = personoversiktStatusService,
    )

    beforeEachTest {
        every { kafkaConsumer.commitSync() } returns Unit
    }

    afterEachTest {
        database.dropData()
        clearMocks(kafkaConsumer)
    }

    describe("pollAndProcessRecords") {
        val kandidatStatusRecord = KandidatStatusRecord(
            uuid = UUID.randomUUID(),
            createdAt = OffsetDateTime.now(),
            personident = UserConstants.ARBEIDSTAKER_FNR,
            status = StatusDTO(
                value = Status.KANDIDAT,
                isActive = true,
            ),
        )
        it("consumes sen oppfolging kandidat status and creates personoversikt status") {
            kafkaConsumer.mockPollConsumerRecords(
                recordValue = kandidatStatusRecord,
                topic = SenOppfolgingKandidatStatusConsumer.SEN_OPPFOLGING_KANDIDAT_STATUS_TOPIC,
            )

            runBlocking { senOppfolgingKandidatStatusConsumer.pollAndProcessRecords(kafkaConsumer) }

            verify(exactly = 1) {
                kafkaConsumer.commitSync()
            }
            val pPersonstatus = database.getPersonOversiktStatusList(fnr = UserConstants.ARBEIDSTAKER_FNR).single()
            pPersonstatus.fnr shouldBeEqualTo kandidatStatusRecord.personident
            pPersonstatus.isAktivSenOppfolgingKandidat shouldBe true
        }

        it("consumes sen oppfolging kandidat status and updates personoversikt status") {
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
            updatedPersonstatus.fnr shouldBeEqualTo kandidatStatusRecord.personident
            updatedPersonstatus.isAktivSenOppfolgingKandidat shouldBe true
        }

        it("consumes sen oppfolging kandidat status FERDIGBEHANDLET and updates personoversikt status") {
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
            updatedPersonstatus.fnr shouldBeEqualTo kandidatStatusFerdigbehandletRecord.personident
            updatedPersonstatus.isAktivSenOppfolgingKandidat shouldBe false
        }
    }
})
