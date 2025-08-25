package no.nav.syfo.frisktilarbeid.kafka

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.syfo.personstatus.domain.PersonIdent
import no.nav.syfo.personstatus.infrastructure.database.queries.getPersonOversiktStatusList
import no.nav.syfo.personstatus.infrastructure.kafka.frisktilarbeid.Status
import no.nav.syfo.testutil.*
import no.nav.syfo.testutil.TestKafkaModule.kafkaConsumerFriskTilArbeid
import no.nav.syfo.testutil.generator.friskTilArbeidConsumerRecord
import no.nav.syfo.testutil.generator.friskTilArbeidTopicPartition
import no.nav.syfo.testutil.generator.generateKafkaFriskTilArbeidVedtak
import no.nav.syfo.testutil.generator.generateKafkaOppfolgingstilfellePerson
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDate

class KafkaFriskTilArbeidServiceTest {

    private val externalMockEnvironment = ExternalMockEnvironment.instance
    private val database = externalMockEnvironment.database

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
        database.dropData()

        clearMocks(kafkaConsumerFriskTilArbeid)
        every { kafkaConsumerFriskTilArbeid.commitSync() } returns Unit
    }

    @Test
    fun `Creates new PersonOversiktStatus if no PersonOversiktStatus exists for personident`() {
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

        assertEquals(1, pPersonOversiktStatusList.size)

        val pPersonOversiktStatus = pPersonOversiktStatusList.first()

        assertEquals(vedtak.personident, pPersonOversiktStatus.fnr)
        assertEquals(vedtak.fom, pPersonOversiktStatus.friskmeldingTilArbeidsformidlingFom)

        assertNull(pPersonOversiktStatus.enhet)
        assertNull(pPersonOversiktStatus.veilederIdent)
    }

    @Test
    fun `Updates existing PersonOversikStatus when PersonOversiktStatus exists for personident`() {
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

        assertEquals(1, pPersonOversiktStatusList.size)
        val pPersonOversiktStatus = pPersonOversiktStatusList.first()
        assertEquals(vedtak.personident, pPersonOversiktStatus.fnr)
        assertEquals(vedtak.fom, pPersonOversiktStatus.friskmeldingTilArbeidsformidlingFom)
    }

    @Test
    fun `Updates existing PersonOversikStatus when FERDIGSTILT`() {
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

        assertEquals(1, pPersonOversiktStatusListBefore.size)

        val pPersonOversiktStatusBefore = pPersonOversiktStatusListBefore.first()

        assertEquals(vedtak.personident, pPersonOversiktStatusBefore.fnr)
        assertEquals(vedtak.fom, pPersonOversiktStatusBefore.friskmeldingTilArbeidsformidlingFom)

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

        assertEquals(1, pPersonOversiktStatusList.size)
        val pPersonOversiktStatus = pPersonOversiktStatusList.first()
        assertEquals(vedtak.personident, pPersonOversiktStatus.fnr)
        assertNull(pPersonOversiktStatus.friskmeldingTilArbeidsformidlingFom)
    }
}
