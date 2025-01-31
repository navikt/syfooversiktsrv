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
import no.nav.syfo.testutil.dropData
import org.amshove.kluent.shouldBeEqualTo
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

class ManglendeMedvirkningVurderingConsumerSpek : Spek({
    val externalMockEnvironment = ExternalMockEnvironment.instance
    val database = externalMockEnvironment.database
    val kafkaConsumer = mockk<KafkaConsumer<String, VurderingRecord>>()
    val personOversiktStatusRepository = externalMockEnvironment.personOversiktStatusRepository
    val personoversiktStatusService = externalMockEnvironment.personoversiktStatusService

    val manglendeMedvirkningConsumer = ManglendeMedvirkningVurderingConsumer(
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

    describe(ManglendeMedvirkningVurderingConsumerSpek::class.java.simpleName) {
        val personident = PersonIdent(UserConstants.ARBEIDSTAKER_FNR)
        val forhandsvarselVurderingRecord = VurderingRecord(
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

        it("creates new personoversiktStatus when person not in database") {
            kafkaConsumer.mockPollConsumerRecords(
                recordValue = forhandsvarselVurderingRecord,
                topic = "teamsykefravr.manglende-medvirkning-vurdering",
            )

            runBlocking { manglendeMedvirkningConsumer.pollAndProcessRecords(kafkaConsumer) }

            verify(exactly = 1) {
                kafkaConsumer.commitSync()
            }

            val personOversiktStatus = personOversiktStatusRepository.getPersonOversiktStatus(personident)
            personOversiktStatus?.isAktivManglendeMedvirkningVurdering shouldBeEqualTo true
            personOversiktStatus?.fnr shouldBeEqualTo personident.value
        }
        it("updates is_aktiv_manglende_medvirkning_vurdering to false when not active vurdering") {
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

            val personOversiktStatus = personOversiktStatusRepository.getPersonOversiktStatus(personident)
            personOversiktStatus?.isAktivManglendeMedvirkningVurdering shouldBeEqualTo false
            personOversiktStatus?.fnr shouldBeEqualTo personident.value
        }
        it("updates is_aktiv_manglende_medvirkning_vurdering to true when active vurdering") {
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

            val personOversiktStatus = personOversiktStatusRepository.getPersonOversiktStatus(personident)
            personOversiktStatus?.isAktivManglendeMedvirkningVurdering shouldBeEqualTo true
            personOversiktStatus?.fnr shouldBeEqualTo personident.value
        }
    }
})
