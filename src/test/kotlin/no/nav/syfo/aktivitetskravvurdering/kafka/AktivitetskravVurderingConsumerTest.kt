package no.nav.syfo.aktivitetskravvurdering.kafka

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.syfo.personstatus.domain.PersonIdent
import no.nav.syfo.personstatus.domain.PersonOversiktStatus
import no.nav.syfo.personstatus.infrastructure.database.queries.createPersonOversiktStatus
import no.nav.syfo.personstatus.infrastructure.kafka.aktivitetskrav.AKTIVITETSKRAV_VURDERING_TOPIC
import no.nav.syfo.personstatus.infrastructure.kafka.aktivitetskrav.AktivitetskravVurderingConsumer
import no.nav.syfo.personstatus.infrastructure.kafka.aktivitetskrav.AktivitetskravVurderingRecord
import no.nav.syfo.personstatus.infrastructure.kafka.mockPollConsumerRecords
import no.nav.syfo.testutil.ExternalMockEnvironment
import no.nav.syfo.testutil.UserConstants
import no.nav.syfo.testutil.createPersonOversiktStatus
import no.nav.syfo.testutil.generator.aktivitetskravVurderingConsumerRecord
import no.nav.syfo.testutil.generator.aktivitetskravVurderingTopicPartition
import no.nav.syfo.testutil.generator.generateKafkaAktivitetskravVurdering
import no.nav.syfo.testutil.generator.generateKafkaOppfolgingstilfellePerson
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration

class AktivitetskravVurderingConsumerTest {

    private val externalMockEnvironment = ExternalMockEnvironment.instance
    private val database = externalMockEnvironment.database
    private val personOversiktStatusRepository = externalMockEnvironment.personOversiktStatusRepository

    private val consumerMock = mockk<KafkaConsumer<String, AktivitetskravVurderingRecord>>()
    private val personoversiktStatusService = externalMockEnvironment.personoversiktStatusService
    private val aktivitetskravVurderingConsumer =
        AktivitetskravVurderingConsumer(personoversiktStatusService = personoversiktStatusService)

    private val aktivitetskravVurderingTopicPartition = aktivitetskravVurderingTopicPartition()
    private val kafkaAktivitetskravVurderingNy = generateKafkaAktivitetskravVurdering(isFinal = false)
    private val kafkaAktivitetskravVurderingAvventer = generateKafkaAktivitetskravVurdering(isFinal = false)
    private val aktivitetskravVurderingNy = generateKafkaAktivitetskravVurdering(isFinal = false)
    private val aktivitetskravVurderingOppfylt = generateKafkaAktivitetskravVurdering(isFinal = true)

    @BeforeEach
    fun setUp() {
        database.resetDatabase()

        clearMocks(consumerMock)
        every { consumerMock.commitSync() } returns Unit
    }

    @Test
    fun `Creates new PersonOversiktStatus if no PersonOversiktStatus exists for personident`() {
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

        val personstatus = personOversiktStatusRepository.getPersonOversiktStatus(
            PersonIdent(UserConstants.ARBEIDSTAKER_FNR)
        )

        assertNotNull(personstatus)

        assertEquals(kafkaAktivitetskravVurderingNy.personIdent, personstatus!!.fnr)
        assertTrue(personstatus.isAktivAktivitetskravvurdering)

        assertNull(personstatus.enhet)
        assertNull(personstatus.veilederIdent)
        assertNull(personstatus.motebehovUbehandlet)
        assertNull(personstatus.oppfolgingsplanLPSBistandUbehandlet)
    }

    @Test
    fun `Updates existing PersonOversikStatus when PersonOversiktStatus exists for personident`() {
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

        val personstatus = personOversiktStatusRepository.getPersonOversiktStatus(
            PersonIdent(UserConstants.ARBEIDSTAKER_FNR)
        )

        assertNotNull(personstatus)
        assertNotNull(personstatus!!.latestOppfolgingstilfelle?.generatedAt)
        assertNotNull(personstatus.latestOppfolgingstilfelle?.oppfolgingstilfelleBitReferanseUuid)
        assertTrue(personstatus.isAktivAktivitetskravvurdering)
    }

    @Test
    fun `Update is_aktiv_aktivitetskrav_vurdering to active when new aktivitetskrav vurdering is received`() {
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
        val personstatusBeforeConsuming = personOversiktStatusRepository.getPersonOversiktStatus(
            PersonIdent(UserConstants.ARBEIDSTAKER_FNR)
        )
        assertNotNull(personstatusBeforeConsuming)
        assertFalse(personstatusBeforeConsuming!!.isAktivAktivitetskravvurdering)
        runBlocking { aktivitetskravVurderingConsumer.pollAndProcessRecords(kafkaConsumer = consumerMock) }
        verify(exactly = 1) { consumerMock.commitSync() }

        val personstatus = personOversiktStatusRepository.getPersonOversiktStatus(
            PersonIdent(UserConstants.ARBEIDSTAKER_FNR)
        )
        assertNotNull(personstatus)
        assertTrue(personstatus!!.isAktivAktivitetskravvurdering)
    }

    @Test
    fun `Update is_aktiv_aktivitetskrav_vurdering to inactive when final aktivitetskrav vurdering is received`() {
        val personoversiktStatus = PersonOversiktStatus(fnr = UserConstants.ARBEIDSTAKER_FNR, isAktivAktivitetskravvurdering = true)
        database.connection.use { connection ->
            connection.createPersonOversiktStatus(
                commit = true,
                personOversiktStatus = personoversiktStatus,
            )
        }
        val personstatusBeforeConsuming = personOversiktStatusRepository.getPersonOversiktStatus(
            PersonIdent(UserConstants.ARBEIDSTAKER_FNR)
        )
        assertNotNull(personstatusBeforeConsuming)
        assertTrue(personstatusBeforeConsuming!!.isAktivAktivitetskravvurdering)

        consumerMock.mockPollConsumerRecords(
            recordValue = aktivitetskravVurderingOppfylt,
            topic = AKTIVITETSKRAV_VURDERING_TOPIC,
        )
        runBlocking { aktivitetskravVurderingConsumer.pollAndProcessRecords(kafkaConsumer = consumerMock) }
        verify(exactly = 1) { consumerMock.commitSync() }

        val personstatus = personOversiktStatusRepository.getPersonOversiktStatus(
            PersonIdent(UserConstants.ARBEIDSTAKER_FNR)
        )
        assertNotNull(personstatus)
        assertFalse(personstatus!!.isAktivAktivitetskravvurdering)
    }
}
