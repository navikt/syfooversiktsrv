package no.nav.syfo.aktivitetskravvurdering.kafka

import io.mockk.*
import kotlinx.coroutines.runBlocking
import no.nav.syfo.aktivitetskravvurdering.domain.AktivitetskravStatus
import no.nav.syfo.personstatus.domain.PersonOversiktStatus
import no.nav.syfo.personstatus.infrastructure.database.queries.getPersonOversiktStatusList
import no.nav.syfo.personstatus.infrastructure.database.queries.createPersonOversiktStatus
import no.nav.syfo.personstatus.infrastructure.kafka.mockPollConsumerRecords
import no.nav.syfo.testutil.*
import no.nav.syfo.testutil.generator.*
import org.amshove.kluent.*
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.*

class AktivitetskravVurderingConsumerSpek : Spek({
    val externalMockEnvironment = ExternalMockEnvironment.instance
    val database = externalMockEnvironment.database

    val consumerMock = mockk<KafkaConsumer<String, AktivitetskravVurderingRecord>>()
    val personoversiktStatusService = externalMockEnvironment.personoversiktStatusService
    val aktivitetskravVurderingConsumer =
        AktivitetskravVurderingConsumer(personoversiktStatusService = personoversiktStatusService)

    val aktivitetskravVurderingTopicPartition = aktivitetskravVurderingTopicPartition()
    val kafkaAktivitetskravVurderingNy = generateKafkaAktivitetskravVurdering(status = AktivitetskravStatus.NY, isFinal = false)
    val kafkaAktivitetskravVurderingAvventer = generateKafkaAktivitetskravVurdering(
        status = AktivitetskravStatus.AVVENT,
        beskrivelse = "Avventer",
        sistVurdert = OffsetDateTime.now().minusMinutes(30),
        frist = LocalDate.now().plusWeeks(1),
        isFinal = false,
    )
    val aktivitetskravVurderingNy = generateKafkaAktivitetskravVurdering(
        status = AktivitetskravStatus.NY,
        isFinal = false,
    )
    val aktivitetskravVurderingOppfylt = generateKafkaAktivitetskravVurdering(
        status = AktivitetskravStatus.OPPFYLT,
        isFinal = true,
    )

    beforeEachTest {
        database.dropData()

        clearMocks(consumerMock)
        every { consumerMock.commitSync() } returns Unit
    }

    describe("${AktivitetskravVurderingConsumer::class.java.simpleName}: pollAndProcessRecords") {
        it("creates new PersonOversiktStatus if no PersonOversiktStatus exists for personident") {
            every { consumerMock.poll(any<Duration>()) } returns ConsumerRecords(
                mapOf(
                    aktivitetskravVurderingTopicPartition to listOf(
                        aktivitetskravVurderingConsumerRecord(
                            aktivitetskravVurderingRecord = kafkaAktivitetskravVurderingNy,
                        ),
                    )
                )
            )

            runBlocking { aktivitetskravVurderingConsumer.pollAndProcessRecords(kafkaConsumer = consumerMock) }
            verify(exactly = 1) { consumerMock.commitSync() }

            val pPersonOversiktStatusList =
                database.connection.use { it.getPersonOversiktStatusList(UserConstants.ARBEIDSTAKER_FNR) }

            pPersonOversiktStatusList.size shouldBeEqualTo 1

            val pPersonOversiktStatus = pPersonOversiktStatusList.first()

            pPersonOversiktStatus.fnr shouldBeEqualTo kafkaAktivitetskravVurderingNy.personIdent
            pPersonOversiktStatus.isAktivAktivitetskravvurdering.shouldBeTrue()

            pPersonOversiktStatus.enhet.shouldBeNull()
            pPersonOversiktStatus.veilederIdent.shouldBeNull()
            pPersonOversiktStatus.motebehovUbehandlet.shouldBeNull()
            pPersonOversiktStatus.oppfolgingsplanLPSBistandUbehandlet.shouldBeNull()
        }
        it("updates existing PersonOversikStatus when PersonOversiktStatus exists for personident") {
            every { consumerMock.poll(any<Duration>()) } returns ConsumerRecords(
                mapOf(
                    aktivitetskravVurderingTopicPartition to listOf(
                        aktivitetskravVurderingConsumerRecord(
                            aktivitetskravVurderingRecord = kafkaAktivitetskravVurderingAvventer,
                        ),
                    )
                )
            )

            val kafkaOppfolgingstilfellePerson = generateKafkaOppfolgingstilfellePerson()
            val kafkaOppfolgingstilfelle = kafkaOppfolgingstilfellePerson.oppfolgingstilfelleList.first()
            database.createPersonOversiktStatus(
                personOversiktStatus = kafkaOppfolgingstilfellePerson.toPersonOversiktStatus(
                    kafkaOppfolgingstilfelle
                )
            )
            runBlocking { aktivitetskravVurderingConsumer.pollAndProcessRecords(kafkaConsumer = consumerMock) }
            verify(exactly = 1) { consumerMock.commitSync() }

            val pPersonOversiktStatusList =
                database.connection.use { it.getPersonOversiktStatusList(UserConstants.ARBEIDSTAKER_FNR) }

            pPersonOversiktStatusList.size shouldBeEqualTo 1
            val pPersonOversiktStatus = pPersonOversiktStatusList.first()
            pPersonOversiktStatus.oppfolgingstilfelleGeneratedAt.shouldNotBeNull()
            pPersonOversiktStatus.oppfolgingstilfelleBitReferanseUuid.shouldNotBeNull()
            pPersonOversiktStatus.isAktivAktivitetskravvurdering.shouldBeTrue()
        }

        it("update is_aktiv_aktivitetskrav_vurdering to active when new aktivitetskrav vurdering is received") {
            val personoversiktStatus = PersonOversiktStatus(fnr = UserConstants.ARBEIDSTAKER_FNR)
            database.connection.use { connection ->
                connection.createPersonOversiktStatus(
                    commit = true,
                    personOversiktStatus = personoversiktStatus,
                )
            }

            consumerMock.mockPollConsumerRecords(
                recordValue = aktivitetskravVurderingNy,
                topic = AKTIVITETSKRAV_VURDERING_TOPIC,
            )
            val personstatusBeforeConsuming =
                database.connection.use { it.getPersonOversiktStatusList(UserConstants.ARBEIDSTAKER_FNR) }.first()
            personstatusBeforeConsuming.isAktivAktivitetskravvurdering shouldBe false
            runBlocking { aktivitetskravVurderingConsumer.pollAndProcessRecords(kafkaConsumer = consumerMock) }
            verify(exactly = 1) { consumerMock.commitSync() }

            val personstatus = database.connection.use { it.getPersonOversiktStatusList(UserConstants.ARBEIDSTAKER_FNR) }.first()
            personstatus.isAktivAktivitetskravvurdering shouldBe true
        }

        it("update is_aktiv_aktivitetskrav_vurdering to inactive when final aktivitetskrav vurdering is received") {
            val personoversiktStatus = PersonOversiktStatus(fnr = UserConstants.ARBEIDSTAKER_FNR, isAktivAktivitetskravvurdering = true)
            database.connection.use { connection ->
                connection.createPersonOversiktStatus(
                    commit = true,
                    personOversiktStatus = personoversiktStatus,
                )
            }
            val personstatusBeforeConsuming =
                database.connection.use { it.getPersonOversiktStatusList(UserConstants.ARBEIDSTAKER_FNR) }.first()
            personstatusBeforeConsuming.isAktivAktivitetskravvurdering shouldBe true

            consumerMock.mockPollConsumerRecords(
                recordValue = aktivitetskravVurderingOppfylt,
                topic = AKTIVITETSKRAV_VURDERING_TOPIC,
            )
            runBlocking { aktivitetskravVurderingConsumer.pollAndProcessRecords(kafkaConsumer = consumerMock) }
            verify(exactly = 1) { consumerMock.commitSync() }

            val pPersonOversiktStatusList = database.connection.use { it.getPersonOversiktStatusList(UserConstants.ARBEIDSTAKER_FNR) }
            pPersonOversiktStatusList.first().isAktivAktivitetskravvurdering shouldBe false
        }
    }
})
