package no.nav.syfo.oppfolgingstilfelle.kafka

import io.mockk.clearMocks
import io.mockk.every
import kotlinx.coroutines.runBlocking
import no.nav.syfo.personstatus.domain.*
import no.nav.syfo.personstatus.infrastructure.database.queries.getPersonOppfolgingstilfelleVirksomhetList
import no.nav.syfo.personstatus.infrastructure.database.queries.getPersonOversiktStatusList
import no.nav.syfo.personstatus.infrastructure.kafka.personoppgavehendelse.KPersonoppgavehendelse
import no.nav.syfo.testutil.*
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testutil.UserConstants.VIRKSOMHETSNUMMER
import no.nav.syfo.testutil.UserConstants.VIRKSOMHETSNUMMER_2
import no.nav.syfo.testutil.UserConstants.VIRKSOMHETSNUMMER_3
import no.nav.syfo.testutil.assertion.checkPPersonOversiktStatusOppfolgingstilfelle
import no.nav.syfo.testutil.generator.generateKafkaOppfolgingstilfellePerson
import no.nav.syfo.testutil.generator.oppfolgingstilfellePersonConsumerRecord
import no.nav.syfo.testutil.generator.oppfolgingstilfellePersonTopicPartition
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration

class OppfolgingstilfelleConsumerTest {
    val externalMockEnvironment = ExternalMockEnvironment.instance
    val database = externalMockEnvironment.database
    val personOversiktStatusRepository = externalMockEnvironment.personOversiktStatusRepository

    val oppfolgingstilfelleConsumer = TestKafkaModule.oppfolgingstilfelleConsumer

    val mockKafkaConsumerOppfolgingstilfellePerson = TestKafkaModule.kafkaConsumerOppfolgingstilfellePerson

    val oppfolgingstilfellePersonTopicPartition = oppfolgingstilfellePersonTopicPartition()
    val personIdentDefault = PersonIdent(ARBEIDSTAKER_FNR)

    val oppfolgingstilfellePersonRecordRelevant = generateKafkaOppfolgingstilfellePerson(
        personIdent = personIdentDefault,
    )
    val oppfolgingstilfellePersonConsumerRecordRelevant = oppfolgingstilfellePersonConsumerRecord(
        oppfolgingstilfellePersonRecord = oppfolgingstilfellePersonRecordRelevant
    )
    val oppfolgingstilfellePersonRecordRelevantNotArbeidstaker = generateKafkaOppfolgingstilfellePerson(
        arbeidstakerAtTilfelleEnd = false,
        personIdent = personIdentDefault,
        virksomhetsnummerList = emptyList(),
    )
    val oppfolgingstilfellePersonConsumerRecordRelevantNotArbeidstaker = oppfolgingstilfellePersonConsumerRecord(
        oppfolgingstilfellePersonRecord = oppfolgingstilfellePersonRecordRelevantNotArbeidstaker
    )

    fun mockConsumer(vararg records: ConsumerRecord<String, OppfolgingstilfellePersonRecord>) {
        every { mockKafkaConsumerOppfolgingstilfellePerson.poll(any<Duration>()) } returns ConsumerRecords(
            mapOf(
                oppfolgingstilfellePersonTopicPartition to listOf(
                    *records,
                )
            )
        )
    }

    @BeforeEach
    fun setUp() {
        database.resetDatabase()

        clearMocks(mockKafkaConsumerOppfolgingstilfellePerson)
        every { mockKafkaConsumerOppfolgingstilfellePerson.commitSync() } returns Unit
    }

    @Test
    fun `Should create new PersonOversiktStatus if no PersonOversiktStatus exists for PersonIdent`() {
        mockConsumer(oppfolgingstilfellePersonConsumerRecordRelevant)

        runBlocking {
            oppfolgingstilfelleConsumer.pollAndProcessRecords(
                kafkaConsumer = mockKafkaConsumerOppfolgingstilfellePerson,
            )
        }

        val recordValue = oppfolgingstilfellePersonConsumerRecordRelevant.value()

        val pPersonStatus =
            personOversiktStatusRepository.getPersonOversiktStatus(PersonIdent(recordValue.personIdentNumber))

        assertEquals(recordValue.personIdentNumber, pPersonStatus?.fnr)
        assertNotNull(pPersonStatus?.navn)
        assertNotNull(pPersonStatus?.fodselsdato)
    }

    @Test
    fun `Should create new PersonOversiktStatus with navn and fodselsdato from PDL`() {
        mockConsumer(oppfolgingstilfellePersonConsumerRecordRelevant)

        runBlocking {
            oppfolgingstilfelleConsumer.pollAndProcessRecords(
                kafkaConsumer = mockKafkaConsumerOppfolgingstilfellePerson,
            )
        }

        val recordValue = oppfolgingstilfellePersonConsumerRecordRelevant.value()

        database.connection.use { connection ->
            val pPersonOversiktStatusList = connection.getPersonOversiktStatusList(
                fnr = recordValue.personIdentNumber,
            )

            assertEquals(1, pPersonOversiktStatusList.size)

            val pPersonOversiktStatus = pPersonOversiktStatusList.first()

            assertEquals(recordValue.personIdentNumber, pPersonOversiktStatus.fnr)
            assertNull(pPersonOversiktStatus.enhet)
            assertNull(pPersonOversiktStatus.veilederIdent)

            assertNull(pPersonOversiktStatus.motebehovUbehandlet)
            assertNull(pPersonOversiktStatus.oppfolgingsplanLPSBistandUbehandlet)
            assertNull(pPersonOversiktStatus.dialogmotekandidat)
            assertNull(pPersonOversiktStatus.dialogmotekandidatGeneratedAt)

            checkPPersonOversiktStatusOppfolgingstilfelle(
                pPersonOversiktStatus = pPersonOversiktStatus,
                oppfolgingstilfellePersonRecord = recordValue,
            )
        }
    }

    @Test
    fun `Should create new PersonOversiktStatus if no PersonOversiktStatus exists for PersonIdent even if not arbeidstaker`() {
        mockConsumer(oppfolgingstilfellePersonConsumerRecordRelevantNotArbeidstaker)

        runBlocking {
            oppfolgingstilfelleConsumer.pollAndProcessRecords(
                kafkaConsumer = mockKafkaConsumerOppfolgingstilfellePerson,
            )
        }

        val recordValue = oppfolgingstilfellePersonConsumerRecordRelevantNotArbeidstaker.value()

        database.connection.use { connection ->
            val pPersonOversiktStatusList = connection.getPersonOversiktStatusList(
                fnr = recordValue.personIdentNumber,
            )

            assertEquals(1, pPersonOversiktStatusList.size)

            val pPersonOversiktStatus = pPersonOversiktStatusList.first()

            assertEquals(recordValue.personIdentNumber, pPersonOversiktStatus.fnr)
            assertNull(pPersonOversiktStatus.enhet)
            assertNull(pPersonOversiktStatus.veilederIdent)

            assertNull(pPersonOversiktStatus.motebehovUbehandlet)
            assertNull(pPersonOversiktStatus.oppfolgingsplanLPSBistandUbehandlet)
            assertNull(pPersonOversiktStatus.dialogmotekandidat)
            assertNull(pPersonOversiktStatus.dialogmotekandidatGeneratedAt)

            checkPPersonOversiktStatusOppfolgingstilfelle(
                pPersonOversiktStatus = pPersonOversiktStatus,
                oppfolgingstilfellePersonRecord = recordValue,
            )
        }
    }

    @Test
    fun `Should update existing PersonOversiktStatus with OPPFOLGINGSPLANLPS_BISTAND_MOTTATT, with data from KafkaOppfolgingstilfellePerson`() {
        mockConsumer(oppfolgingstilfellePersonConsumerRecordRelevant)

        val oversiktHendelseOPLPSBistandMottatt = KPersonoppgavehendelse(
            personIdentDefault.value,
            OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_MOTTATT,
        )
        val personoversiktStatus = PersonOversiktStatus(
            fnr = oversiktHendelseOPLPSBistandMottatt.personident
        ).applyOversikthendelse(oversiktHendelseOPLPSBistandMottatt.hendelsetype)

        database.createPersonOversiktStatus(personoversiktStatus)
        runBlocking {
            oppfolgingstilfelleConsumer.pollAndProcessRecords(
                kafkaConsumer = mockKafkaConsumerOppfolgingstilfellePerson,
            )
        }

        val recordValue = oppfolgingstilfellePersonConsumerRecordRelevant.value()

        val pPersonOversiktStatusList = database.connection.use { connection ->
            connection.getPersonOversiktStatusList(
                fnr = recordValue.personIdentNumber,
            )
        }

        assertEquals(1, pPersonOversiktStatusList.size)

        val pPersonOversiktStatus = pPersonOversiktStatusList.first()

        assertEquals(recordValue.personIdentNumber, pPersonOversiktStatus.fnr)
        assertNull(pPersonOversiktStatus.enhet)
        assertNull(pPersonOversiktStatus.veilederIdent)

        assertNull(pPersonOversiktStatus.motebehovUbehandlet)
        assertTrue(pPersonOversiktStatus.oppfolgingsplanLPSBistandUbehandlet!!)
        assertNull(pPersonOversiktStatus.dialogmotekandidat)
        assertNull(pPersonOversiktStatus.dialogmotekandidatGeneratedAt)

        checkPPersonOversiktStatusOppfolgingstilfelle(
            pPersonOversiktStatus = pPersonOversiktStatus,
            oppfolgingstilfellePersonRecord = recordValue,
        )
    }

    @Test
    fun `Should only update latest OppfolgingstilfellPerson with the newest referanseTilfelleBitInntruffet, if multiple relevant records on same personIdent is received in same poll`() {
        val kafkaOppfolgingstilfellePersonServiceRelevantFirst = generateKafkaOppfolgingstilfellePerson(
            personIdent = personIdentDefault,
        )
        val kafkaOppfolgingstilfellePersonServiceRecordRelevantFirst = oppfolgingstilfellePersonConsumerRecord(
            oppfolgingstilfellePersonRecord = kafkaOppfolgingstilfellePersonServiceRelevantFirst,
        )

        val kafkaOppfolgingstilfellePersonServiceRelevantNewest = generateKafkaOppfolgingstilfellePerson(
            personIdent = PersonIdent(kafkaOppfolgingstilfellePersonServiceRelevantFirst.personIdentNumber),
        ).copy(
            referanseTilfelleBitInntruffet = kafkaOppfolgingstilfellePersonServiceRelevantFirst.referanseTilfelleBitInntruffet.plusSeconds(
                1
            )
        )
        val kafkaOppfolgingstilfellePersonServiceRecordRelevantNewest = oppfolgingstilfellePersonConsumerRecord(
            oppfolgingstilfellePersonRecord = kafkaOppfolgingstilfellePersonServiceRelevantNewest,
        )

        mockConsumer(
            kafkaOppfolgingstilfellePersonServiceRecordRelevantFirst,
            kafkaOppfolgingstilfellePersonServiceRecordRelevantNewest,
        )

        runBlocking {
            oppfolgingstilfelleConsumer.pollAndProcessRecords(
                kafkaConsumer = mockKafkaConsumerOppfolgingstilfellePerson,
            )
        }

        val recordValue = kafkaOppfolgingstilfellePersonServiceRecordRelevantNewest.value()

        database.connection.use { connection ->
            val pPersonOversiktStatusList = connection.getPersonOversiktStatusList(
                fnr = recordValue.personIdentNumber,
            )

            assertEquals(1, pPersonOversiktStatusList.size)

            val pPersonOversiktStatus = pPersonOversiktStatusList.first()

            assertEquals(recordValue.personIdentNumber, pPersonOversiktStatus.fnr)
            assertNull(pPersonOversiktStatus.enhet)
            assertNull(pPersonOversiktStatus.veilederIdent)

            assertNull(pPersonOversiktStatus.motebehovUbehandlet)
            assertNull(pPersonOversiktStatus.oppfolgingsplanLPSBistandUbehandlet)
            assertNull(pPersonOversiktStatus.dialogmotekandidat)
            assertNull(pPersonOversiktStatus.dialogmotekandidatGeneratedAt)

            checkPPersonOversiktStatusOppfolgingstilfelle(
                pPersonOversiktStatus = pPersonOversiktStatus,
                oppfolgingstilfellePersonRecord = recordValue,
            )

            val pPersonOppfolgingstilfelleVirksomhetList =
                connection.getPersonOppfolgingstilfelleVirksomhetList(
                    pPersonOversikStatusId = pPersonOversiktStatus.id,
                )

            assertEquals(
                recordValue.oppfolgingstilfelleList.first().virksomhetsnummerList.size,
                pPersonOppfolgingstilfelleVirksomhetList.size
            )

            assertEquals(
                recordValue.oppfolgingstilfelleList.first().virksomhetsnummerList.first(),
                pPersonOppfolgingstilfelleVirksomhetList.first().virksomhetsnummer.value
            )
        }
    }

    @Test
    fun `Should only update latest OppfolgingstilfellPerson with the newest createdAt, if multiple relevant records on same personIdent with same referanseTilfelleBitInntruffet is received in same poll `() {
        val kafkaOppfolgingstilfellePersonServiceRelevantFirst = generateKafkaOppfolgingstilfellePerson(
            personIdent = personIdentDefault,
        )
        val kafkaOppfolgingstilfellePersonServiceRecordRelevantFirst = oppfolgingstilfellePersonConsumerRecord(
            oppfolgingstilfellePersonRecord = kafkaOppfolgingstilfellePersonServiceRelevantFirst
        )

        val kafkaOppfolgingstilfellePersonServiceRelevantNewest = generateKafkaOppfolgingstilfellePerson(
            personIdent = PersonIdent(kafkaOppfolgingstilfellePersonServiceRelevantFirst.personIdentNumber),
        ).copy(
            createdAt = kafkaOppfolgingstilfellePersonServiceRelevantFirst.createdAt.plusSeconds(1),
            referanseTilfelleBitInntruffet = kafkaOppfolgingstilfellePersonServiceRelevantFirst.referanseTilfelleBitInntruffet
        )
        val kafkaOppfolgingstilfellePersonServiceRecordRelevantNewest = oppfolgingstilfellePersonConsumerRecord(
            oppfolgingstilfellePersonRecord = kafkaOppfolgingstilfellePersonServiceRelevantNewest,
        )

        mockConsumer(
            kafkaOppfolgingstilfellePersonServiceRecordRelevantNewest,
            kafkaOppfolgingstilfellePersonServiceRecordRelevantFirst,
        )

        runBlocking {
            oppfolgingstilfelleConsumer.pollAndProcessRecords(
                kafkaConsumer = mockKafkaConsumerOppfolgingstilfellePerson,
            )
        }

        val recordValue = kafkaOppfolgingstilfellePersonServiceRecordRelevantNewest.value()

        database.connection.use { connection ->
            val pPersonOversiktStatusList = connection.getPersonOversiktStatusList(
                fnr = recordValue.personIdentNumber,
            )

            assertEquals(1, pPersonOversiktStatusList.size)

            val pPersonOversiktStatus = pPersonOversiktStatusList.first()

            assertEquals(recordValue.personIdentNumber, pPersonOversiktStatus.fnr)
            assertNull(pPersonOversiktStatus.enhet)
            assertNull(pPersonOversiktStatus.veilederIdent)

            assertNull(pPersonOversiktStatus.motebehovUbehandlet)
            assertNull(pPersonOversiktStatus.oppfolgingsplanLPSBistandUbehandlet)
            assertNull(pPersonOversiktStatus.dialogmotekandidat)
            assertNull(pPersonOversiktStatus.dialogmotekandidatGeneratedAt)

            checkPPersonOversiktStatusOppfolgingstilfelle(
                pPersonOversiktStatus = pPersonOversiktStatus,
                oppfolgingstilfellePersonRecord = recordValue,
            )

            val pPersonOppfolgingstilfelleVirksomhetList =
                connection.getPersonOppfolgingstilfelleVirksomhetList(
                    pPersonOversikStatusId = pPersonOversiktStatus.id,
                )

            assertEquals(
                recordValue.oppfolgingstilfelleList.first().virksomhetsnummerList.size,
                pPersonOppfolgingstilfelleVirksomhetList.size
            )

            assertEquals(
                recordValue.oppfolgingstilfelleList.first().virksomhetsnummerList.first(),
                pPersonOppfolgingstilfelleVirksomhetList.first().virksomhetsnummer.value
            )
        }
    }

    @Test
    fun `Should update latest OppfolgingstilfellPerson with the newest referanseTilfelleBitInntruffet, if multiple relevant records on same personIdent is received in different polls`() {
        val kafkaOppfolgingstilfellePersonServiceRelevantFirst = generateKafkaOppfolgingstilfellePerson(
            personIdent = personIdentDefault,
        )
        val kafkaOppfolgingstilfellePersonServiceRecordRelevantFirst = oppfolgingstilfellePersonConsumerRecord(
            oppfolgingstilfellePersonRecord = kafkaOppfolgingstilfellePersonServiceRelevantFirst,
        )

        val kafkaOppfolgingstilfellePersonServiceRelevantSecond = generateKafkaOppfolgingstilfellePerson(
            personIdent = PersonIdent(kafkaOppfolgingstilfellePersonServiceRelevantFirst.personIdentNumber),
        ).copy(
            referanseTilfelleBitInntruffet = kafkaOppfolgingstilfellePersonServiceRelevantFirst.referanseTilfelleBitInntruffet.plusSeconds(
                1
            )
        )
        val kafkaOppfolgingstilfellePersonServiceRecordRelevantSecond = oppfolgingstilfellePersonConsumerRecord(
            oppfolgingstilfellePersonRecord = kafkaOppfolgingstilfellePersonServiceRelevantSecond,
        )

        val kafkaOppfolgingstilfellePersonServiceRelevantNewest = generateKafkaOppfolgingstilfellePerson(
            personIdent = PersonIdent(kafkaOppfolgingstilfellePersonServiceRelevantFirst.personIdentNumber),
        ).copy(
            referanseTilfelleBitInntruffet = kafkaOppfolgingstilfellePersonServiceRelevantSecond.referanseTilfelleBitInntruffet.plusSeconds(
                1
            )
        )
        val kafkaOppfolgingstilfellePersonServiceRecordRelevantNewest = oppfolgingstilfellePersonConsumerRecord(
            oppfolgingstilfellePersonRecord = kafkaOppfolgingstilfellePersonServiceRelevantNewest,
        )

        mockConsumer(kafkaOppfolgingstilfellePersonServiceRecordRelevantSecond)

        runBlocking {
            oppfolgingstilfelleConsumer.pollAndProcessRecords(
                kafkaConsumer = mockKafkaConsumerOppfolgingstilfellePerson,
            )
        }

        mockConsumer(kafkaOppfolgingstilfellePersonServiceRecordRelevantFirst)

        runBlocking {
            oppfolgingstilfelleConsumer.pollAndProcessRecords(
                kafkaConsumer = mockKafkaConsumerOppfolgingstilfellePerson,
            )
        }

        val recordValueSecond = kafkaOppfolgingstilfellePersonServiceRecordRelevantSecond.value()

        database.connection.use { connection ->
            val pPersonOversiktStatusList = connection.getPersonOversiktStatusList(
                fnr = recordValueSecond.personIdentNumber,
            )

            assertEquals(1, pPersonOversiktStatusList.size)

            val pPersonOversiktStatus = pPersonOversiktStatusList.first()

            assertEquals(recordValueSecond.personIdentNumber, pPersonOversiktStatus.fnr)
            assertNull(pPersonOversiktStatus.enhet)
            assertNull(pPersonOversiktStatus.veilederIdent)

            assertNull(pPersonOversiktStatus.motebehovUbehandlet)
            assertNull(pPersonOversiktStatus.oppfolgingsplanLPSBistandUbehandlet)
            assertNull(pPersonOversiktStatus.dialogmotekandidat)
            assertNull(pPersonOversiktStatus.dialogmotekandidatGeneratedAt)

            checkPPersonOversiktStatusOppfolgingstilfelle(
                pPersonOversiktStatus = pPersonOversiktStatus,
                oppfolgingstilfellePersonRecord = recordValueSecond,
            )

            val pPersonOppfolgingstilfelleVirksomhetList =
                connection.getPersonOppfolgingstilfelleVirksomhetList(
                    pPersonOversikStatusId = pPersonOversiktStatus.id,
                )

            assertEquals(
                recordValueSecond.oppfolgingstilfelleList.first().virksomhetsnummerList.size,
                pPersonOppfolgingstilfelleVirksomhetList.size
            )

            assertEquals(
                recordValueSecond.oppfolgingstilfelleList.first().virksomhetsnummerList.first(),
                pPersonOppfolgingstilfelleVirksomhetList.first().virksomhetsnummer.value
            )
        }

        mockConsumer(kafkaOppfolgingstilfellePersonServiceRecordRelevantNewest)

        runBlocking {
            oppfolgingstilfelleConsumer.pollAndProcessRecords(
                kafkaConsumer = mockKafkaConsumerOppfolgingstilfellePerson,
            )
        }

        val recordValueNewest = kafkaOppfolgingstilfellePersonServiceRecordRelevantNewest.value()

        database.connection.use { connection ->
            val pPersonOversiktStatusList = connection.getPersonOversiktStatusList(
                fnr = recordValueNewest.personIdentNumber,
            )

            assertEquals(1, pPersonOversiktStatusList.size)

            val pPersonOversiktStatus = pPersonOversiktStatusList.first()

            assertEquals(recordValueNewest.personIdentNumber, pPersonOversiktStatus.fnr)
            assertNull(pPersonOversiktStatus.enhet)
            assertNull(pPersonOversiktStatus.veilederIdent)

            assertNull(pPersonOversiktStatus.motebehovUbehandlet)
            assertNull(pPersonOversiktStatus.oppfolgingsplanLPSBistandUbehandlet)
            assertNull(pPersonOversiktStatus.dialogmotekandidat)
            assertNull(pPersonOversiktStatus.dialogmotekandidatGeneratedAt)

            checkPPersonOversiktStatusOppfolgingstilfelle(
                pPersonOversiktStatus = pPersonOversiktStatus,
                oppfolgingstilfellePersonRecord = recordValueNewest,
            )

            val pPersonOppfolgingstilfelleVirksomhetList =
                connection.getPersonOppfolgingstilfelleVirksomhetList(
                    pPersonOversikStatusId = pPersonOversiktStatus.id,
                )

            assertEquals(
                recordValueNewest.oppfolgingstilfelleList.first().virksomhetsnummerList.size,
                pPersonOppfolgingstilfelleVirksomhetList.size
            )

            assertEquals(
                recordValueNewest.oppfolgingstilfelleList.first().virksomhetsnummerList.first(),
                pPersonOppfolgingstilfelleVirksomhetList.first().virksomhetsnummer.value
            )
        }
    }

    @Test
    fun `Should update latest OppfolgingstilfellPerson with the newest referanseTilfelleBitInntruffet and createdAt, if multiple relevant records on same personIdent with same inntruffet is received in different polls`() {
        val kafkaOppfolgingstilfellePersonServiceRelevantFirst = generateKafkaOppfolgingstilfellePerson(
            personIdent = personIdentDefault,
        )
        val kafkaOppfolgingstilfellePersonServiceRecordRelevantFirst = oppfolgingstilfellePersonConsumerRecord(
            oppfolgingstilfellePersonRecord = kafkaOppfolgingstilfellePersonServiceRelevantFirst,
        )
        val oppfolgingstilfelle = kafkaOppfolgingstilfellePersonServiceRelevantFirst.oppfolgingstilfelleList[0]
        val kafkaOppfolgingstilfellePersonServiceRelevantSecond = generateKafkaOppfolgingstilfellePerson(
            personIdent = PersonIdent(kafkaOppfolgingstilfellePersonServiceRelevantFirst.personIdentNumber),
        ).copy(
            createdAt = kafkaOppfolgingstilfellePersonServiceRelevantFirst.createdAt.plusSeconds(1),
            referanseTilfelleBitInntruffet = kafkaOppfolgingstilfellePersonServiceRelevantFirst.referanseTilfelleBitInntruffet,
            oppfolgingstilfelleList = listOf(
                KafkaOppfolgingstilfelle(
                    arbeidstakerAtTilfelleEnd = true,
                    start = oppfolgingstilfelle.start.minusDays(7),
                    end = oppfolgingstilfelle.end,
                    virksomhetsnummerList = oppfolgingstilfelle.virksomhetsnummerList,
                    antallSykedager = oppfolgingstilfelle.antallSykedager,
                ),
            )
        )
        val kafkaOppfolgingstilfellePersonServiceRecordRelevantSecond = oppfolgingstilfellePersonConsumerRecord(
            oppfolgingstilfellePersonRecord = kafkaOppfolgingstilfellePersonServiceRelevantSecond,
        )
        mockConsumer(kafkaOppfolgingstilfellePersonServiceRecordRelevantFirst)

        runBlocking {
            oppfolgingstilfelleConsumer.pollAndProcessRecords(
                kafkaConsumer = mockKafkaConsumerOppfolgingstilfellePerson,
            )
        }

        mockConsumer(kafkaOppfolgingstilfellePersonServiceRecordRelevantSecond)

        runBlocking {
            oppfolgingstilfelleConsumer.pollAndProcessRecords(
                kafkaConsumer = mockKafkaConsumerOppfolgingstilfellePerson,
            )
        }
        val recordValueSecond = kafkaOppfolgingstilfellePersonServiceRecordRelevantSecond.value()

        database.connection.use { connection ->
            val pPersonOversiktStatusList = connection.getPersonOversiktStatusList(
                fnr = recordValueSecond.personIdentNumber,
            )

            assertEquals(1, pPersonOversiktStatusList.size)

            val pPersonOversiktStatus = pPersonOversiktStatusList.first()

            assertEquals(recordValueSecond.personIdentNumber, pPersonOversiktStatus.fnr)
            assertNull(pPersonOversiktStatus.enhet)
            assertNull(pPersonOversiktStatus.veilederIdent)

            assertNull(pPersonOversiktStatus.motebehovUbehandlet)
            assertNull(pPersonOversiktStatus.oppfolgingsplanLPSBistandUbehandlet)
            assertNull(pPersonOversiktStatus.dialogmotekandidat)
            assertNull(pPersonOversiktStatus.dialogmotekandidatGeneratedAt)

            checkPPersonOversiktStatusOppfolgingstilfelle(
                pPersonOversiktStatus = pPersonOversiktStatus,
                oppfolgingstilfellePersonRecord = recordValueSecond,
            )

            val pPersonOppfolgingstilfelleVirksomhetList =
                connection.getPersonOppfolgingstilfelleVirksomhetList(
                    pPersonOversikStatusId = pPersonOversiktStatus.id,
                )

            assertEquals(
                recordValueSecond.oppfolgingstilfelleList.first().virksomhetsnummerList.size,
                pPersonOppfolgingstilfelleVirksomhetList.size
            )

            assertEquals(
                recordValueSecond.oppfolgingstilfelleList.first().virksomhetsnummerList.first(),
                pPersonOppfolgingstilfelleVirksomhetList.first().virksomhetsnummer.value
            )
        }
    }

    @Test
    fun `Should only update latest OppfolgingstilfellPerson with the newest createdAt, if multiple relevant records on same personIdent with same referanseTilfelleBitUuid is received in different polls `() {
        val kafkaOppfolgingstilfellePersonServiceRelevantFirst = generateKafkaOppfolgingstilfellePerson(
            personIdent = personIdentDefault,
        )
        val kafkaOppfolgingstilfellePersonServiceRecordRelevantFirst = oppfolgingstilfellePersonConsumerRecord(
            oppfolgingstilfellePersonRecord = kafkaOppfolgingstilfellePersonServiceRelevantFirst,
        )

        val kafkaOppfolgingstilfellePersonServiceRelevantSecond = generateKafkaOppfolgingstilfellePerson(
            personIdent = PersonIdent(kafkaOppfolgingstilfellePersonServiceRelevantFirst.personIdentNumber),
        ).copy(
            createdAt = kafkaOppfolgingstilfellePersonServiceRelevantFirst.createdAt.plusSeconds(1),
            referanseTilfelleBitInntruffet = kafkaOppfolgingstilfellePersonServiceRelevantFirst.referanseTilfelleBitInntruffet,
            referanseTilfelleBitUuid = kafkaOppfolgingstilfellePersonServiceRelevantFirst.referanseTilfelleBitUuid,
        )
        val kafkaOppfolgingstilfellePersonServiceRecordRelevantSecond = oppfolgingstilfellePersonConsumerRecord(
            oppfolgingstilfellePersonRecord = kafkaOppfolgingstilfellePersonServiceRelevantSecond,
        )

        val kafkaOppfolgingstilfellePersonServiceRelevantNewest = generateKafkaOppfolgingstilfellePerson(
            personIdent = PersonIdent(kafkaOppfolgingstilfellePersonServiceRelevantFirst.personIdentNumber),
        ).copy(
            createdAt = kafkaOppfolgingstilfellePersonServiceRelevantSecond.createdAt.plusSeconds(1),
            referanseTilfelleBitInntruffet = kafkaOppfolgingstilfellePersonServiceRelevantFirst.referanseTilfelleBitInntruffet,
            referanseTilfelleBitUuid = kafkaOppfolgingstilfellePersonServiceRelevantFirst.referanseTilfelleBitUuid,
        )
        val kafkaOppfolgingstilfellePersonServiceRecordRelevantNewest = oppfolgingstilfellePersonConsumerRecord(
            oppfolgingstilfellePersonRecord = kafkaOppfolgingstilfellePersonServiceRelevantNewest,
        )

        mockConsumer(kafkaOppfolgingstilfellePersonServiceRecordRelevantSecond)

        runBlocking {
            oppfolgingstilfelleConsumer.pollAndProcessRecords(
                kafkaConsumer = mockKafkaConsumerOppfolgingstilfellePerson,
            )
        }

        mockConsumer(kafkaOppfolgingstilfellePersonServiceRecordRelevantFirst)

        runBlocking {
            oppfolgingstilfelleConsumer.pollAndProcessRecords(
                kafkaConsumer = mockKafkaConsumerOppfolgingstilfellePerson,
            )
        }

        val recordValueSecond = kafkaOppfolgingstilfellePersonServiceRecordRelevantSecond.value()

        database.connection.use { connection ->
            val pPersonOversiktStatusList = connection.getPersonOversiktStatusList(
                fnr = recordValueSecond.personIdentNumber,
            )

            assertEquals(1, pPersonOversiktStatusList.size)

            val pPersonOversiktStatus = pPersonOversiktStatusList.first()

            assertEquals(recordValueSecond.personIdentNumber, pPersonOversiktStatus.fnr)
            assertNull(pPersonOversiktStatus.enhet)
            assertNull(pPersonOversiktStatus.veilederIdent)

            assertNull(pPersonOversiktStatus.motebehovUbehandlet)
            assertNull(pPersonOversiktStatus.oppfolgingsplanLPSBistandUbehandlet)
            assertNull(pPersonOversiktStatus.dialogmotekandidat)
            assertNull(pPersonOversiktStatus.dialogmotekandidatGeneratedAt)

            checkPPersonOversiktStatusOppfolgingstilfelle(
                pPersonOversiktStatus = pPersonOversiktStatus,
                oppfolgingstilfellePersonRecord = recordValueSecond,
            )

            val pPersonOppfolgingstilfelleVirksomhetList =
                connection.getPersonOppfolgingstilfelleVirksomhetList(
                    pPersonOversikStatusId = pPersonOversiktStatus.id,
                )

            assertEquals(
                recordValueSecond.oppfolgingstilfelleList.first().virksomhetsnummerList.size,
                pPersonOppfolgingstilfelleVirksomhetList.size
            )

            assertEquals(
                recordValueSecond.oppfolgingstilfelleList.first().virksomhetsnummerList.first(),
                pPersonOppfolgingstilfelleVirksomhetList.first().virksomhetsnummer.value
            )
        }

        mockConsumer(kafkaOppfolgingstilfellePersonServiceRecordRelevantNewest)

        runBlocking {
            oppfolgingstilfelleConsumer.pollAndProcessRecords(
                kafkaConsumer = mockKafkaConsumerOppfolgingstilfellePerson,
            )
        }

        val recordValueNewest = kafkaOppfolgingstilfellePersonServiceRecordRelevantNewest.value()

        database.connection.use { connection ->
            val pPersonOversiktStatusList = connection.getPersonOversiktStatusList(
                fnr = recordValueNewest.personIdentNumber,
            )

            assertEquals(1, pPersonOversiktStatusList.size)

            val pPersonOversiktStatus = pPersonOversiktStatusList.first()

            assertEquals(recordValueNewest.personIdentNumber, pPersonOversiktStatus.fnr)
            assertNull(pPersonOversiktStatus.enhet)
            assertNull(pPersonOversiktStatus.veilederIdent)

            assertNull(pPersonOversiktStatus.motebehovUbehandlet)
            assertNull(pPersonOversiktStatus.oppfolgingsplanLPSBistandUbehandlet)
            assertNull(pPersonOversiktStatus.dialogmotekandidat)
            assertNull(pPersonOversiktStatus.dialogmotekandidatGeneratedAt)

            checkPPersonOversiktStatusOppfolgingstilfelle(
                pPersonOversiktStatus = pPersonOversiktStatus,
                oppfolgingstilfellePersonRecord = recordValueNewest,
            )

            val pPersonOppfolgingstilfelleVirksomhetList =
                connection.getPersonOppfolgingstilfelleVirksomhetList(
                    pPersonOversikStatusId = pPersonOversiktStatus.id,
                )

            assertEquals(
                recordValueNewest.oppfolgingstilfelleList.first().virksomhetsnummerList.size,
                pPersonOppfolgingstilfelleVirksomhetList.size
            )

            assertEquals(
                recordValueNewest.oppfolgingstilfelleList.first().virksomhetsnummerList.first(),
                pPersonOppfolgingstilfelleVirksomhetList.first().virksomhetsnummer.value
            )
        }
    }

    @Test
    fun `Should first receive and create Virksomhet1 and Virksomhet2, then delete Virksomhet1, keep Viksomhet2 and create Virskomhet3`() {
        val virksomhetsnummerListFirst = listOf(
            Virksomhetsnummer(VIRKSOMHETSNUMMER),
            Virksomhetsnummer(VIRKSOMHETSNUMMER_2),
        )
        val kafkaOppfolgingstilfellePersonServiceRelevantFirst = generateKafkaOppfolgingstilfellePerson(
            personIdent = personIdentDefault,
            virksomhetsnummerList = virksomhetsnummerListFirst
        )
        val kafkaOppfolgingstilfellePersonServiceRecordRelevantFirst = oppfolgingstilfellePersonConsumerRecord(
            oppfolgingstilfellePersonRecord = kafkaOppfolgingstilfellePersonServiceRelevantFirst,
        )

        mockConsumer(kafkaOppfolgingstilfellePersonServiceRecordRelevantFirst)

        runBlocking {
            oppfolgingstilfelleConsumer.pollAndProcessRecords(
                kafkaConsumer = mockKafkaConsumerOppfolgingstilfellePerson,
            )
        }

        val virksomhetsnummerListSecond = listOf(
            Virksomhetsnummer(VIRKSOMHETSNUMMER_2),
            Virksomhetsnummer(VIRKSOMHETSNUMMER_3),
        )
        val kafkaOppfolgingstilfellePersonServiceRelevantSecond = generateKafkaOppfolgingstilfellePerson(
            personIdent = personIdentDefault,
            virksomhetsnummerList = virksomhetsnummerListSecond,
        )
        val kafkaOppfolgingstilfellePersonServiceRecordRelevantSecond = oppfolgingstilfellePersonConsumerRecord(
            oppfolgingstilfellePersonRecord = kafkaOppfolgingstilfellePersonServiceRelevantSecond,
        )

        mockConsumer(kafkaOppfolgingstilfellePersonServiceRecordRelevantSecond)

        runBlocking {
            oppfolgingstilfelleConsumer.pollAndProcessRecords(
                kafkaConsumer = mockKafkaConsumerOppfolgingstilfellePerson,
            )
        }

        val recordValue = kafkaOppfolgingstilfellePersonServiceRecordRelevantSecond.value()

        database.connection.use { connection ->
            val pPersonOversiktStatusList = connection.getPersonOversiktStatusList(
                fnr = recordValue.personIdentNumber,
            )

            assertEquals(1, pPersonOversiktStatusList.size)

            val pPersonOversiktStatus = pPersonOversiktStatusList.first()

            assertEquals(recordValue.personIdentNumber, pPersonOversiktStatus.fnr)
            assertNull(pPersonOversiktStatus.enhet)
            assertNull(pPersonOversiktStatus.veilederIdent)

            assertNull(pPersonOversiktStatus.motebehovUbehandlet)
            assertNull(pPersonOversiktStatus.oppfolgingsplanLPSBistandUbehandlet)
            assertNull(pPersonOversiktStatus.dialogmotekandidat)
            assertNull(pPersonOversiktStatus.dialogmotekandidatGeneratedAt)

            checkPPersonOversiktStatusOppfolgingstilfelle(
                pPersonOversiktStatus = pPersonOversiktStatus,
                oppfolgingstilfellePersonRecord = recordValue,
            )

            val pPersonOppfolgingstilfelleVirksomhetList =
                connection.getPersonOppfolgingstilfelleVirksomhetList(
                    pPersonOversikStatusId = pPersonOversiktStatus.id,
                )

            assertEquals(2, pPersonOppfolgingstilfelleVirksomhetList.size)

            assertEquals(
                virksomhetsnummerListSecond.first().value,
                pPersonOppfolgingstilfelleVirksomhetList.first().virksomhetsnummer.value
            )
            assertEquals(virksomhetsnummerListSecond.last().value, pPersonOppfolgingstilfelleVirksomhetList.last().virksomhetsnummer.value)
        }
    }

    @Test
    fun `Creates new PersonOversiktStatus and handles antallSykedager = null`() {
        val kafkaOppfolgingstilfellePerson = generateKafkaOppfolgingstilfellePerson(
            personIdent = personIdentDefault,
            antallSykedager = null,
        )
        val kafkaOppfolgingstilfellePersonRecord = oppfolgingstilfellePersonConsumerRecord(
            oppfolgingstilfellePersonRecord = kafkaOppfolgingstilfellePerson
        )
        mockConsumer(kafkaOppfolgingstilfellePersonRecord)

        runBlocking {
            oppfolgingstilfelleConsumer.pollAndProcessRecords(
                kafkaConsumer = mockKafkaConsumerOppfolgingstilfellePerson,
            )
        }

        val recordValue = kafkaOppfolgingstilfellePersonRecord.value()

        database.connection.use { connection ->
            val pPersonOversiktStatusList = connection.getPersonOversiktStatusList(
                fnr = recordValue.personIdentNumber,
            )

            assertEquals(1, pPersonOversiktStatusList.size)

            val pPersonOversiktStatus = pPersonOversiktStatusList.first()

            assertEquals(recordValue.personIdentNumber, pPersonOversiktStatus.fnr)
            assertNull(pPersonOversiktStatus.enhet)
            assertNull(pPersonOversiktStatus.veilederIdent)

            assertNull(pPersonOversiktStatus.motebehovUbehandlet)
            assertNull(pPersonOversiktStatus.oppfolgingsplanLPSBistandUbehandlet)
            assertNull(pPersonOversiktStatus.dialogmotekandidat)
            assertNull(pPersonOversiktStatus.dialogmotekandidatGeneratedAt)

            checkPPersonOversiktStatusOppfolgingstilfelle(
                pPersonOversiktStatus = pPersonOversiktStatus,
                oppfolgingstilfellePersonRecord = recordValue,
            )
        }
    }

    @Test
    fun `Updates oppfolgingstilfelle and removes tildelt veileder from existing PersonOversiktStatus when tildelt veileder not found`() {
        mockConsumer(oppfolgingstilfellePersonConsumerRecordRelevant)

        personOversiktStatusRepository.createPersonOversiktStatus(
            PersonOversiktStatus(
                fnr = personIdentDefault.value,
                veilederIdent = UserConstants.VEILEDER_ID_2,
            )
        )

        runBlocking {
            oppfolgingstilfelleConsumer.pollAndProcessRecords(
                kafkaConsumer = mockKafkaConsumerOppfolgingstilfellePerson,
            )
        }

        val personOversiktStatus = personOversiktStatusRepository.getPersonOversiktStatus(personIdentDefault)!!

        assertNotNull(personOversiktStatus.latestOppfolgingstilfelle)
        assertNull(personOversiktStatus.veilederIdent)
    }

    @Test
    fun `Updates oppfolgingstilfelle and removes tildelt veileder from existing PersonOversiktStatus when tildelt veileder not enabled`() {
        mockConsumer(oppfolgingstilfellePersonConsumerRecordRelevant)

        personOversiktStatusRepository.createPersonOversiktStatus(
            PersonOversiktStatus(
                fnr = personIdentDefault.value,
                veilederIdent = UserConstants.VEILEDER_ID_NOT_ENABLED,
            )
        )

        runBlocking {
            oppfolgingstilfelleConsumer.pollAndProcessRecords(
                kafkaConsumer = mockKafkaConsumerOppfolgingstilfellePerson,
            )
        }

        val personOversiktStatus = personOversiktStatusRepository.getPersonOversiktStatus(personIdentDefault)!!

        assertNotNull(personOversiktStatus.latestOppfolgingstilfelle)
        assertNull(personOversiktStatus.veilederIdent)
    }

    @Test
    fun `Updates oppfolgingstilfelle and does not remove tildelt veileder from existing PersonOversiktStatus when tildelt veileder found and enabled`() {
        mockConsumer(oppfolgingstilfellePersonConsumerRecordRelevant)

        personOversiktStatusRepository.createPersonOversiktStatus(
            PersonOversiktStatus(
                fnr = personIdentDefault.value,
                veilederIdent = UserConstants.VEILEDER_ID,
            )
        )

        runBlocking {
            oppfolgingstilfelleConsumer.pollAndProcessRecords(
                kafkaConsumer = mockKafkaConsumerOppfolgingstilfellePerson,
            )
        }

        val personOversiktStatus = personOversiktStatusRepository.getPersonOversiktStatus(personIdentDefault)!!

        assertNotNull(personOversiktStatus.latestOppfolgingstilfelle)
        assertEquals(UserConstants.VEILEDER_ID, personOversiktStatus.veilederIdent)
    }

    @Test
    fun `Updates oppfolgingstilfelle and does not remove tildelt veileder from existing PersonOversiktStatus when request to get tildelt veileder fails`() {
        mockConsumer(oppfolgingstilfellePersonConsumerRecordRelevant)

        personOversiktStatusRepository.createPersonOversiktStatus(
            PersonOversiktStatus(
                fnr = personIdentDefault.value,
                veilederIdent = UserConstants.VEILEDER_ID_WITH_ERROR,
            )
        )

        runBlocking {
            oppfolgingstilfelleConsumer.pollAndProcessRecords(
                kafkaConsumer = mockKafkaConsumerOppfolgingstilfellePerson,
            )
        }

        val personOversiktStatus = personOversiktStatusRepository.getPersonOversiktStatus(personIdentDefault)!!

        assertNotNull(personOversiktStatus.latestOppfolgingstilfelle)
        assertEquals(UserConstants.VEILEDER_ID_WITH_ERROR, personOversiktStatus.veilederIdent)
    }
}
