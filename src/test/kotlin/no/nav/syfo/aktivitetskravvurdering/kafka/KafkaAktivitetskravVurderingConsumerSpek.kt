package no.nav.syfo.aktivitetskravvurdering.kafka

import io.ktor.server.testing.*
import io.mockk.*
import no.nav.syfo.aktivitetskravvurdering.domain.AktivitetskravStatus
import no.nav.syfo.oppfolgingstilfelle.kafka.toPersonOversiktStatus
import no.nav.syfo.personstatus.PersonoversiktStatusService
import no.nav.syfo.personstatus.application.arbeidsuforhet.IArbeidsuforhetvurderingClient
import no.nav.syfo.personstatus.application.oppfolgingsoppgave.IOppfolgingsoppgaveClient
import no.nav.syfo.personstatus.db.*
import no.nav.syfo.personstatus.infrastructure.database.repository.PersonOversiktStatusRepository
import no.nav.syfo.testutil.*
import no.nav.syfo.testutil.generator.*
import org.amshove.kluent.*
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.*

class KafkaAktivitetskravVurderingConsumerSpek : Spek({
    with(TestApplicationEngine()) {
        start()

        val externalMockEnvironment = ExternalMockEnvironment.instance
        val database = externalMockEnvironment.database

        val kafkaConsumerMock = mockk<KafkaConsumer<String, AktivitetskravVurderingRecord>>()
        val personOppgaveRepository = PersonOversiktStatusRepository(database = database)
        val personoversiktStatusService = PersonoversiktStatusService(
            database = database,
            pdlClient = externalMockEnvironment.pdlClient,
            arbeidsuforhetvurderingClient = mockk<IArbeidsuforhetvurderingClient>(),
            personoversiktStatusRepository = personOppgaveRepository,
            oppfolgingsoppgaveClient = mockk<IOppfolgingsoppgaveClient>(),
        )
        val aktivitetskravVurderingConsumer =
            AktivitetskravVurderingConsumer(database = database, personoversiktStatusService = personoversiktStatusService)

        val aktivitetskravVurderingTopicPartition = aktivitetskravVurderingTopicPartition()
        val kafkaAktivitetskravVurderingNy = generateKafkaAktivitetskravVurdering(status = AktivitetskravStatus.NY, isFinal = false)
        val kafkaAktivitetskravVurderingAvventer = generateKafkaAktivitetskravVurdering(
            status = AktivitetskravStatus.AVVENT,
            beskrivelse = "Avventer",
            sistVurdert = OffsetDateTime.now().minusMinutes(30),
            frist = LocalDate.now().plusWeeks(1),
            isFinal = false,
        )

        beforeEachTest {
            database.dropData()

            clearMocks(kafkaConsumerMock)
            every { kafkaConsumerMock.commitSync() } returns Unit
        }

        describe("${AktivitetskravVurderingConsumer::class.java.simpleName}: pollAndProcessRecords") {
            it("creates new PersonOversiktStatus if no PersonOversiktStatus exists for personident") {
                every { kafkaConsumerMock.poll(any<Duration>()) } returns ConsumerRecords(
                    mapOf(
                        aktivitetskravVurderingTopicPartition to listOf(
                            aktivitetskravVurderingConsumerRecord(
                                aktivitetskravVurderingRecord = kafkaAktivitetskravVurderingNy,
                            ),
                        )
                    )
                )

                aktivitetskravVurderingConsumer.pollAndProcessRecords(
                    kafkaConsumer = kafkaConsumerMock,
                )

                verify(exactly = 1) {
                    kafkaConsumerMock.commitSync()
                }

                val pPersonOversiktStatusList =
                    database.connection.use { it.getPersonOversiktStatusList(UserConstants.ARBEIDSTAKER_FNR) }

                pPersonOversiktStatusList.size shouldBeEqualTo 1

                val pPersonOversiktStatus = pPersonOversiktStatusList.first()

                pPersonOversiktStatus.fnr shouldBeEqualTo kafkaAktivitetskravVurderingNy.personIdent
                pPersonOversiktStatus.aktivitetskrav shouldBeEqualTo kafkaAktivitetskravVurderingNy.status
                pPersonOversiktStatus.aktivitetskravStoppunkt shouldBeEqualTo kafkaAktivitetskravVurderingNy.stoppunktAt
                pPersonOversiktStatus.aktivitetskravVurderingFrist.shouldBeNull()

                pPersonOversiktStatus.enhet.shouldBeNull()
                pPersonOversiktStatus.veilederIdent.shouldBeNull()
                pPersonOversiktStatus.motebehovUbehandlet.shouldBeNull()
                pPersonOversiktStatus.oppfolgingsplanLPSBistandUbehandlet.shouldBeNull()
            }
            it("updates existing PersonOversikStatus when PersonOversiktStatus exists for personident") {
                every { kafkaConsumerMock.poll(any<Duration>()) } returns ConsumerRecords(
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

                aktivitetskravVurderingConsumer.pollAndProcessRecords(
                    kafkaConsumer = kafkaConsumerMock,
                )

                verify(exactly = 1) {
                    kafkaConsumerMock.commitSync()
                }

                val pPersonOversiktStatusList =
                    database.connection.use { it.getPersonOversiktStatusList(UserConstants.ARBEIDSTAKER_FNR) }

                pPersonOversiktStatusList.size shouldBeEqualTo 1
                val pPersonOversiktStatus = pPersonOversiktStatusList.first()
                pPersonOversiktStatus.oppfolgingstilfelleGeneratedAt.shouldNotBeNull()
                pPersonOversiktStatus.oppfolgingstilfelleBitReferanseUuid.shouldNotBeNull()

                pPersonOversiktStatus.aktivitetskrav shouldBeEqualTo kafkaAktivitetskravVurderingAvventer.status
                pPersonOversiktStatus.aktivitetskravStoppunkt shouldBeEqualTo kafkaAktivitetskravVurderingAvventer.stoppunktAt
                pPersonOversiktStatus.aktivitetskravVurderingFrist shouldBeEqualTo kafkaAktivitetskravVurderingAvventer.frist
            }
        }
    }
})
