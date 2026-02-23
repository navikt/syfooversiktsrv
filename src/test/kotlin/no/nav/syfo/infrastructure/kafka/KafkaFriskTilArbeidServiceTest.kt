package no.nav.syfo.infrastructure.kafka

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.infrastructure.kafka.frisktilarbeid.Status
import no.nav.syfo.testutil.ExternalMockEnvironment
import no.nav.syfo.testutil.TestKafkaModule
import no.nav.syfo.testutil.UserConstants
import no.nav.syfo.testutil.createPersonOversiktStatus
import no.nav.syfo.testutil.generator.friskTilArbeidConsumerRecord
import no.nav.syfo.testutil.generator.friskTilArbeidTopicPartition
import no.nav.syfo.testutil.generator.generateKafkaFriskTilArbeidVedtak
import no.nav.syfo.testutil.generator.generateKafkaOppfolgingstilfellePerson
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertNull
import java.time.Duration
import java.time.LocalDate
import kotlin.test.assertEquals

class KafkaFriskTilArbeidServiceTest {

    private val externalMockEnvironment = ExternalMockEnvironment.Companion.instance
    private val database = externalMockEnvironment.database
    private val personOversiktStatusRepository = externalMockEnvironment.personOversiktStatusRepository

    private val kafkaFriskTilArbeidService = TestKafkaModule.friskTilArbeidVedtakConsumer

    private val topicPartition = friskTilArbeidTopicPartition()
    private val vedtak = generateKafkaFriskTilArbeidVedtak(
        personIdent = PersonIdent(UserConstants.ARBEIDSTAKER_FNR),
        fom = LocalDate.now().plusDays(1),
    )
    private val kafkaFriskTilArbeidConsumerRecord = friskTilArbeidConsumerRecord(
        vedtakStatusRecord = vedtak,
    )

    private val kafkaFriskTilArbeidFerdigConsumerRecord = friskTilArbeidConsumerRecord(
        vedtakStatusRecord = vedtak.copy(status = Status.FERDIG_BEHANDLET),
    )

    @BeforeEach
    fun setUp() {
        database.resetDatabase()

        clearMocks(TestKafkaModule.kafkaConsumerFriskTilArbeid)
        every { TestKafkaModule.kafkaConsumerFriskTilArbeid.commitSync() } returns Unit
    }

    @Test
    fun `Creates new PersonOversiktStatus if no PersonOversiktStatus exists for personident`() {
        every { TestKafkaModule.kafkaConsumerFriskTilArbeid.poll(any<Duration>()) } returns ConsumerRecords(
            mapOf(
                topicPartition to listOf(
                    kafkaFriskTilArbeidConsumerRecord,
                )
            )
        )

        runBlocking {
            kafkaFriskTilArbeidService.pollAndProcessRecords(kafkaConsumer = TestKafkaModule.kafkaConsumerFriskTilArbeid)
        }

        verify(exactly = 1) {
            TestKafkaModule.kafkaConsumerFriskTilArbeid.commitSync()
        }

        val personOversiktStatus = personOversiktStatusRepository.getPersonOversiktStatus(
            PersonIdent(UserConstants.ARBEIDSTAKER_FNR)
        )

        assertNotNull(personOversiktStatus)

        assertEquals(vedtak.personident, personOversiktStatus!!.fnr)
        assertEquals(vedtak.fom, personOversiktStatus.friskmeldingTilArbeidsformidlingFom)

        assertNull(personOversiktStatus.enhet)
        assertNull(personOversiktStatus.veilederIdent)
    }

    @Test
    fun `Updates existing PersonOversikStatus when PersonOversiktStatus exists for personident`() {
        every { TestKafkaModule.kafkaConsumerFriskTilArbeid.poll(any<Duration>()) } returns ConsumerRecords(
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
            kafkaFriskTilArbeidService.pollAndProcessRecords(kafkaConsumer = TestKafkaModule.kafkaConsumerFriskTilArbeid)
        }

        verify(exactly = 1) {
            TestKafkaModule.kafkaConsumerFriskTilArbeid.commitSync()
        }

        val personOversiktStatus = personOversiktStatusRepository.getPersonOversiktStatus(
            PersonIdent(UserConstants.ARBEIDSTAKER_FNR)
        )

        assertNotNull(personOversiktStatus)
        assertEquals(vedtak.personident, personOversiktStatus!!.fnr)
        assertEquals(vedtak.fom, personOversiktStatus.friskmeldingTilArbeidsformidlingFom)
    }

    @Test
    fun `Updates existing PersonOversikStatus when FERDIGSTILT`() {
        every { TestKafkaModule.kafkaConsumerFriskTilArbeid.poll(any<Duration>()) } returns ConsumerRecords(
            mapOf(
                topicPartition to listOf(
                    kafkaFriskTilArbeidConsumerRecord,
                )
            )
        )
        runBlocking {
            kafkaFriskTilArbeidService.pollAndProcessRecords(kafkaConsumer = TestKafkaModule.kafkaConsumerFriskTilArbeid)
        }
        clearMocks(TestKafkaModule.kafkaConsumerFriskTilArbeid)
        every { TestKafkaModule.kafkaConsumerFriskTilArbeid.commitSync() } returns Unit

        val personOversiktStatusBefore = personOversiktStatusRepository.getPersonOversiktStatus(
            PersonIdent(UserConstants.ARBEIDSTAKER_FNR)
        )

        assertNotNull(personOversiktStatusBefore)

        assertEquals(vedtak.personident, personOversiktStatusBefore!!.fnr)
        assertEquals(vedtak.fom, personOversiktStatusBefore.friskmeldingTilArbeidsformidlingFom)

        every { TestKafkaModule.kafkaConsumerFriskTilArbeid.poll(any<Duration>()) } returns ConsumerRecords(
            mapOf(
                topicPartition to listOf(
                    kafkaFriskTilArbeidFerdigConsumerRecord,
                )
            )
        )

        runBlocking {
            kafkaFriskTilArbeidService.pollAndProcessRecords(kafkaConsumer = TestKafkaModule.kafkaConsumerFriskTilArbeid)
        }

        verify(exactly = 1) {
            TestKafkaModule.kafkaConsumerFriskTilArbeid.commitSync()
        }

        val personOversiktStatus = personOversiktStatusRepository.getPersonOversiktStatus(
            PersonIdent(UserConstants.ARBEIDSTAKER_FNR)
        )

        assertNotNull(personOversiktStatus)
        assertEquals(vedtak.personident, personOversiktStatus!!.fnr)
        assertNull(personOversiktStatus.friskmeldingTilArbeidsformidlingFom)
    }
}
