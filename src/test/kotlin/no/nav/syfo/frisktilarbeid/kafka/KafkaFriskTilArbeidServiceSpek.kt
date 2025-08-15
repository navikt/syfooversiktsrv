package no.nav.syfo.frisktilarbeid.kafka

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.syfo.personstatus.domain.PersonIdent
import no.nav.syfo.personstatus.infrastructure.database.queries.getPersonOversiktStatusList
import no.nav.syfo.personstatus.infrastructure.kafka.frisktilarbeid.FriskTilArbeidVedtakConsumer
import no.nav.syfo.personstatus.infrastructure.kafka.frisktilarbeid.Status
import no.nav.syfo.testutil.*
import no.nav.syfo.testutil.TestKafkaModule.kafkaConsumerFriskTilArbeid
import no.nav.syfo.testutil.generator.friskTilArbeidConsumerRecord
import no.nav.syfo.testutil.generator.friskTilArbeidTopicPartition
import no.nav.syfo.testutil.generator.generateKafkaFriskTilArbeidVedtak
import no.nav.syfo.testutil.generator.generateKafkaOppfolgingstilfellePerson
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.Duration
import java.time.LocalDate

class KafkaFriskTilArbeidServiceSpek : Spek({
    val externalMockEnvironment = ExternalMockEnvironment.instance
    val database = externalMockEnvironment.database

    val kafkaFriskTilArbeidService = TestKafkaModule.friskTilArbeidVedtakConsumer

    val topicPartition = friskTilArbeidTopicPartition()
    val vedtak = generateKafkaFriskTilArbeidVedtak(
        personIdent = PersonIdent(UserConstants.ARBEIDSTAKER_FNR),
        fom = LocalDate.now().plusDays(1),
    )
    val kafkaFriskTilArbeidConsumerRecord = friskTilArbeidConsumerRecord(
        vedtakStatusRecord = vedtak,
    )

    val kafkaFriskTilArbeidFerdigConsumerRecord = friskTilArbeidConsumerRecord(
        vedtakStatusRecord = vedtak.copy(status = Status.FERDIG_BEHANDLET),
    )

    beforeEachTest {
        database.dropData()

        clearMocks(kafkaConsumerFriskTilArbeid)
        every { kafkaConsumerFriskTilArbeid.commitSync() } returns Unit
    }

    describe("${FriskTilArbeidVedtakConsumer::class.java.simpleName}: pollAndProcessRecords") {
        it("creates new PersonOversiktStatus if no PersonOversiktStatus exists for personident") {
            every { kafkaConsumerFriskTilArbeid.poll(any<Duration>()) } returns ConsumerRecords(
                mapOf(
                    topicPartition to listOf(
                        kafkaFriskTilArbeidConsumerRecord,
                    )
                )
            )

            runBlocking {
                kafkaFriskTilArbeidService.pollAndProcessRecords(kafkaConsumer = kafkaConsumerFriskTilArbeid)
            }

            verify(exactly = 1) {
                kafkaConsumerFriskTilArbeid.commitSync()
            }

            val pPersonOversiktStatusList =
                database.connection.getPersonOversiktStatusList(UserConstants.ARBEIDSTAKER_FNR)

            pPersonOversiktStatusList.size shouldBeEqualTo 1

            val pPersonOversiktStatus = pPersonOversiktStatusList.first()

            pPersonOversiktStatus.fnr shouldBeEqualTo vedtak.personident
            pPersonOversiktStatus.friskmeldingTilArbeidsformidlingFom shouldBeEqualTo vedtak.fom

            pPersonOversiktStatus.enhet.shouldBeNull()
            pPersonOversiktStatus.veilederIdent.shouldBeNull()
        }
        it("updates existing PersonOversikStatus when PersonOversiktStatus exists for personident") {
            every { kafkaConsumerFriskTilArbeid.poll(any<Duration>()) } returns ConsumerRecords(
                mapOf(
                    topicPartition to listOf(
                        kafkaFriskTilArbeidConsumerRecord,
                    )
                )
            )

            val kafkaOppfolgingstilfellePerson = generateKafkaOppfolgingstilfellePerson()
            val kafkaOppfolgingstilfelle = kafkaOppfolgingstilfellePerson.oppfolgingstilfelleList.first()
            database.createPersonOversiktStatus(
                personOversiktStatus = kafkaOppfolgingstilfellePerson.toPersonOversiktStatus(kafkaOppfolgingstilfelle)
            )

            runBlocking {
                kafkaFriskTilArbeidService.pollAndProcessRecords(kafkaConsumer = kafkaConsumerFriskTilArbeid)
            }

            verify(exactly = 1) {
                kafkaConsumerFriskTilArbeid.commitSync()
            }

            val pPersonOversiktStatusList =
                database.connection.getPersonOversiktStatusList(UserConstants.ARBEIDSTAKER_FNR)

            pPersonOversiktStatusList.size shouldBeEqualTo 1
            val pPersonOversiktStatus = pPersonOversiktStatusList.first()
            pPersonOversiktStatus.fnr shouldBeEqualTo vedtak.personident
            pPersonOversiktStatus.friskmeldingTilArbeidsformidlingFom shouldBeEqualTo vedtak.fom
        }
        it("updates existing PersonOversikStatus when FERDIGSTILT") {
            every { kafkaConsumerFriskTilArbeid.poll(any<Duration>()) } returns ConsumerRecords(
                mapOf(
                    topicPartition to listOf(
                        kafkaFriskTilArbeidConsumerRecord,
                    )
                )
            )
            runBlocking {
                kafkaFriskTilArbeidService.pollAndProcessRecords(kafkaConsumer = kafkaConsumerFriskTilArbeid)
            }
            clearMocks(kafkaConsumerFriskTilArbeid)
            every { kafkaConsumerFriskTilArbeid.commitSync() } returns Unit

            val pPersonOversiktStatusListBefore =
                database.connection.getPersonOversiktStatusList(UserConstants.ARBEIDSTAKER_FNR)

            pPersonOversiktStatusListBefore.size shouldBeEqualTo 1

            val pPersonOversiktStatusBefore = pPersonOversiktStatusListBefore.first()

            pPersonOversiktStatusBefore.fnr shouldBeEqualTo vedtak.personident
            pPersonOversiktStatusBefore.friskmeldingTilArbeidsformidlingFom shouldBeEqualTo vedtak.fom

            every { kafkaConsumerFriskTilArbeid.poll(any<Duration>()) } returns ConsumerRecords(
                mapOf(
                    topicPartition to listOf(
                        kafkaFriskTilArbeidFerdigConsumerRecord,
                    )
                )
            )

            runBlocking {
                kafkaFriskTilArbeidService.pollAndProcessRecords(kafkaConsumer = kafkaConsumerFriskTilArbeid)
            }

            verify(exactly = 1) {
                kafkaConsumerFriskTilArbeid.commitSync()
            }

            val pPersonOversiktStatusList =
                database.connection.getPersonOversiktStatusList(UserConstants.ARBEIDSTAKER_FNR)

            pPersonOversiktStatusList.size shouldBeEqualTo 1
            val pPersonOversiktStatus = pPersonOversiktStatusList.first()
            pPersonOversiktStatus.fnr shouldBeEqualTo vedtak.personident
            pPersonOversiktStatus.friskmeldingTilArbeidsformidlingFom shouldBe null
        }
    }
})
