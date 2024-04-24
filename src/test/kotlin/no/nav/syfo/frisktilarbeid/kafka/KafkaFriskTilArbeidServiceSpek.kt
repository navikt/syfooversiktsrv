package no.nav.syfo.frisktilarbeid.kafka

import io.ktor.server.testing.*
import io.mockk.*
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.oppfolgingstilfelle.kafka.toPersonOversiktStatus
import no.nav.syfo.personstatus.db.*
import no.nav.syfo.testutil.*
import no.nav.syfo.testutil.TestKafkaModule.kafkaConsumerFriskTilArbeid
import no.nav.syfo.testutil.generator.*
import org.amshove.kluent.*
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.Duration
import java.time.LocalDate
import java.time.OffsetDateTime

class KafkaFriskTilArbeidServiceSpek : Spek({
    with(TestApplicationEngine()) {
        start()

        val externalMockEnvironment = ExternalMockEnvironment.instance
        val database = externalMockEnvironment.database

        val kafkaFriskTilArbeidService = TestKafkaModule.kafkaFriskTilArbeidService

        val topicPartition = friskTilArbeidTopicPartition()
        val vedtak = generateKafkaFriskTilArbeidVedtak(
            personIdent = PersonIdent(UserConstants.ARBEIDSTAKER_FNR),
            createdAt = OffsetDateTime.now(),
            fom = LocalDate.now().plusDays(1),
        )
        val kafkaFriskTilArbeidConsumerRecord = friskTilArbeidConsumerRecord(
            vedtakFattetRecord = vedtak,
        )

        beforeEachTest {
            database.connection.dropData()

            clearMocks(kafkaConsumerFriskTilArbeid)
            every { kafkaConsumerFriskTilArbeid.commitSync() } returns Unit
        }

        describe("${KafkaFriskTilArbeidService::class.java.simpleName}: pollAndProcessRecords") {
            it("creates new PersonOversiktStatus if no PersonOversiktStatus exists for personident") {
                every { kafkaConsumerFriskTilArbeid.poll(any<Duration>()) } returns ConsumerRecords(
                    mapOf(
                        topicPartition to listOf(
                            kafkaFriskTilArbeidConsumerRecord,
                        )
                    )
                )

                kafkaFriskTilArbeidService.pollAndProcessRecords(
                    kafkaConsumer = kafkaConsumerFriskTilArbeid,
                )

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

                kafkaFriskTilArbeidService.pollAndProcessRecords(
                    kafkaConsumer = kafkaConsumerFriskTilArbeid,
                )

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
        }
    }
})
