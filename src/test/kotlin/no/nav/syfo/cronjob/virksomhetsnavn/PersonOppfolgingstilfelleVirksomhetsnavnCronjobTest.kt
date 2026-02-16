package no.nav.syfo.cronjob.virksomhetsnavn

import io.mockk.clearMocks
import io.mockk.every
import kotlinx.coroutines.runBlocking
import no.nav.syfo.personstatus.domain.OversikthendelseType
import no.nav.syfo.personstatus.domain.PersonIdent
import no.nav.syfo.personstatus.domain.PersonOversiktStatus
import no.nav.syfo.personstatus.domain.Virksomhetsnummer
import no.nav.syfo.personstatus.infrastructure.database.queries.getPersonOppfolgingstilfelleVirksomhetList
import no.nav.syfo.personstatus.infrastructure.kafka.personoppgavehendelse.KPersonoppgavehendelse
import no.nav.syfo.testutil.ExternalMockEnvironment
import no.nav.syfo.testutil.InternalMockEnvironment
import no.nav.syfo.testutil.TestKafkaModule
import no.nav.syfo.testutil.UserConstants
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testutil.UserConstants.VIRKSOMHETSNUMMER_DEFAULT
import no.nav.syfo.testutil.UserConstants.VIRKSOMHETSNUMMER_NO_VIRKSOMHETSNAVN
import no.nav.syfo.testutil.assertion.checkPPersonOppfolgingstilfelleVirksomhet
import no.nav.syfo.testutil.assertion.checkPPersonOppfolgingstilfelleVirksomhetUpdated
import no.nav.syfo.testutil.createPersonOversiktStatus
import no.nav.syfo.testutil.generator.dialogmotekandidatEndringConsumerRecord
import no.nav.syfo.testutil.generator.dialogmotekandidatEndringTopicPartition
import no.nav.syfo.testutil.generator.generateKafkaDialogmotekandidatEndringStoppunkt
import no.nav.syfo.testutil.generator.generateKafkaOppfolgingstilfellePerson
import no.nav.syfo.testutil.generator.oppfolgingstilfellePersonConsumerRecord
import no.nav.syfo.testutil.generator.oppfolgingstilfellePersonTopicPartition
import no.nav.syfo.util.nowUTC
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Duration

class PersonOppfolgingstilfelleVirksomhetsnavnCronjobTest {

    private val externalMockEnvironment = ExternalMockEnvironment.instance
    private val database = externalMockEnvironment.database
    private val personOversiktStatusRepository = externalMockEnvironment.personOversiktStatusRepository
    private val internalMockEnvironment = InternalMockEnvironment.instance

    private val personOppfolgingstilfelleVirksomhetnavnCronjob =
        internalMockEnvironment.personOppfolgingstilfelleVirksomhetnavnCronjob

    private val oppfolgingstilfelleConsumer = TestKafkaModule.oppfolgingstilfelleConsumer

    private val mockKafkaConsumerOppfolgingstilfellePerson =
        TestKafkaModule.kafkaConsumerOppfolgingstilfellePerson

    private val oppfolgingstilfellePersonTopicPartition = oppfolgingstilfellePersonTopicPartition()
    private val personIdentDefault = PersonIdent(ARBEIDSTAKER_FNR)

    private val kafkaDialogmotekandidatEndringService = TestKafkaModule.dialogmotekandidatEndringConsumer
    private val mockKafkaConsumerDialogmotekandidatEndring =
        TestKafkaModule.kafkaConsumerDialogmotekandidatEndring
    private val dialogmoteKandidatTopicPartition = dialogmotekandidatEndringTopicPartition()
    private val kafkaDialogmotekandidatEndringStoppunkt = generateKafkaDialogmotekandidatEndringStoppunkt(
        personIdent = personIdentDefault.value,
        createdAt = nowUTC().minusDays(1)
    )
    private val kafkaDialogmotekandidatEndringStoppunktConsumerRecord = dialogmotekandidatEndringConsumerRecord(
        kafkaDialogmotekandidatEndring = kafkaDialogmotekandidatEndringStoppunkt
    )

    @BeforeEach
    fun setUp() {
        database.resetDatabase()

        clearMocks(mockKafkaConsumerOppfolgingstilfellePerson)
        clearMocks(mockKafkaConsumerDialogmotekandidatEndring)
        every { mockKafkaConsumerOppfolgingstilfellePerson.commitSync() } returns Unit
        every { mockKafkaConsumerDialogmotekandidatEndring.commitSync() } returns Unit
    }

    @Nested
    @DisplayName("Successful processing")
    inner class SuccessfulProcessing {

        val kafkaOppfolgingstilfellePersonServiceRelevant = generateKafkaOppfolgingstilfellePerson(
            personIdent = personIdentDefault,
            virksomhetsnummerList = listOf(
                VIRKSOMHETSNUMMER_DEFAULT,
                Virksomhetsnummer(UserConstants.VIRKSOMHETSNUMMER_2),
            )
        )
        val kafkaOppfolgingstilfellePersonServiceRecordRelevant = oppfolgingstilfellePersonConsumerRecord(
            oppfolgingstilfellePersonRecord = kafkaOppfolgingstilfellePersonServiceRelevant
        )

        @BeforeEach
        fun setUp() {
            every { mockKafkaConsumerOppfolgingstilfellePerson.poll(any<Duration>()) } returns ConsumerRecords(
                mapOf(
                    oppfolgingstilfellePersonTopicPartition to listOf(
                        kafkaOppfolgingstilfellePersonServiceRecordRelevant,
                    )
                )
            )
        }

        @Test
        @DisplayName("Should not update Virksomhetsnavn of existing PersonOppfolgingstilfelleVirksomhet if motebehovUbehandlet, oppfolgingsplanLPSBistandUbehandlet and dialogmotekandidat are not true")
        fun `Should not update Virksomhetsnavn of existing PersonOppfolgingstilfelleVirksomhet (shortened)`() {
            runBlocking { oppfolgingstilfelleConsumer.pollAndProcessRecords(kafkaConsumer = mockKafkaConsumerOppfolgingstilfellePerson) }

            val recordValue = kafkaOppfolgingstilfellePersonServiceRecordRelevant.value()

            database.connection.use { connection ->
                val pPersonOversiktStatus = with(personOversiktStatusRepository) {
                    connection.getPersonStatus(personident = PersonIdent(recordValue.personIdentNumber))!!
                }

                assertNotNull(pPersonOversiktStatus)

                assertNull(pPersonOversiktStatus.motebehovUbehandlet)
                assertNull(pPersonOversiktStatus.oppfolgingsplanLPSBistandUbehandlet)

                val pPersonOppfolgingstilfelleVirksomhetList =
                    connection.getPersonOppfolgingstilfelleVirksomhetList(
                        pPersonOversikStatusId = pPersonOversiktStatus.id,
                    )

                checkPPersonOppfolgingstilfelleVirksomhet(
                    pPersonOppfolgingstilfelleVirksomhetList = pPersonOppfolgingstilfelleVirksomhetList,
                    oppfolgingstilfellePersonRecord = recordValue,
                )

                runBlocking {
                    val result = personOppfolgingstilfelleVirksomhetnavnCronjob.runJob()

                    assertEquals(0, result.failed)
                    assertEquals(0, result.updated)
                }
            }
        }

        @Test
        fun `Should update Virksomhetsnavn of existing PersonOppfolgingstilfelleVirksomhet if motebehovUbehandlet or oppfolgingsplanLPSBistandUbehandlet is true`() {
            val oversiktHendelseMotebehovSvarMottatt = KPersonoppgavehendelse(
                personIdentDefault.value,
                OversikthendelseType.MOTEBEHOV_SVAR_MOTTATT,
            )
            val oversiktHendelseOPLPSBistandMottatt = KPersonoppgavehendelse(
                personIdentDefault.value,
                OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_MOTTATT,
            )
            val oversikthendelseList = listOf(
                oversiktHendelseOPLPSBistandMottatt,
                oversiktHendelseMotebehovSvarMottatt,
            )
            oversikthendelseList.forEach { oversikthendelse ->
                database.resetDatabase()

                val personoversiktStatus = PersonOversiktStatus(
                    fnr = oversikthendelse.personident
                ).applyOversikthendelse(oversikthendelse.hendelsetype)

                database.createPersonOversiktStatus(personoversiktStatus)

                runBlocking { oppfolgingstilfelleConsumer.pollAndProcessRecords(kafkaConsumer = mockKafkaConsumerOppfolgingstilfellePerson) }

                val recordValue = kafkaOppfolgingstilfellePersonServiceRecordRelevant.value()

                val pPersonOversiktStatus = database.connection.use { connection ->
                    with(personOversiktStatusRepository) {
                        connection.getPersonStatus(personident = PersonIdent(recordValue.personIdentNumber))!!
                    }
                }

                assertNotNull(pPersonOversiktStatus)

                if (oversikthendelse.hendelsetype == OversikthendelseType.MOTEBEHOV_SVAR_MOTTATT) {
                    assertTrue(pPersonOversiktStatus.motebehovUbehandlet!!)
                } else {
                    assertNull(pPersonOversiktStatus.motebehovUbehandlet)
                }
                if (oversikthendelse.hendelsetype == OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_MOTTATT) {
                    assertTrue(pPersonOversiktStatus.oppfolgingsplanLPSBistandUbehandlet!!)
                } else {
                    assertNull(pPersonOversiktStatus.oppfolgingsplanLPSBistandUbehandlet)
                }

                database.connection.use { connection ->
                    val pPersonOppfolgingstilfelleVirksomhetList =
                        connection.getPersonOppfolgingstilfelleVirksomhetList(
                            pPersonOversikStatusId = pPersonOversiktStatus.id,
                        )

                    checkPPersonOppfolgingstilfelleVirksomhet(
                        pPersonOppfolgingstilfelleVirksomhetList = pPersonOppfolgingstilfelleVirksomhetList,
                        oppfolgingstilfellePersonRecord = recordValue,
                    )
                }

                runBlocking {
                    val result = personOppfolgingstilfelleVirksomhetnavnCronjob.runJob()

                    assertEquals(0, result.failed)
                    assertEquals(2, result.updated)
                }

                runBlocking {
                    val result = personOppfolgingstilfelleVirksomhetnavnCronjob.runJob()

                    assertEquals(0, result.failed)
                    assertEquals(0, result.updated)
                }

                database.connection.use { connection ->
                    val pPersonOppfolgingstilfelleVirksomhetList =
                        connection.getPersonOppfolgingstilfelleVirksomhetList(
                            pPersonOversikStatusId = pPersonOversiktStatus.id,
                        )

                    checkPPersonOppfolgingstilfelleVirksomhetUpdated(
                        pPersonOppfolgingstilfelleVirksomhetList = pPersonOppfolgingstilfelleVirksomhetList,
                        oppfolgingstilfellePersonRecord = recordValue,
                    )
                }
            }
        }

        @Test
        fun `Should update Virksomhetsnavn of existing PersonOppfolgingstilfelleVirksomhet if dialogmotekandidat is true`() {
            every { mockKafkaConsumerDialogmotekandidatEndring.poll(any<Duration>()) } returns ConsumerRecords(
                mapOf(
                    dialogmoteKandidatTopicPartition to listOf(
                        kafkaDialogmotekandidatEndringStoppunktConsumerRecord,
                    )
                )
            )
            runBlocking {
                kafkaDialogmotekandidatEndringService.pollAndProcessRecords(kafkaConsumer = mockKafkaConsumerDialogmotekandidatEndring)
                oppfolgingstilfelleConsumer.pollAndProcessRecords(
                    kafkaConsumer = mockKafkaConsumerOppfolgingstilfellePerson,
                )
            }

            val recordValue = kafkaOppfolgingstilfellePersonServiceRecordRelevant.value()

            val pPersonOversiktStatus = database.connection.use { connection ->
                with(personOversiktStatusRepository) {
                    connection.getPersonStatus(personident = PersonIdent(recordValue.personIdentNumber))!!
                }
            }

            assertNotNull(pPersonOversiktStatus)

            assertNull(pPersonOversiktStatus.motebehovUbehandlet)
            assertNull(pPersonOversiktStatus.oppfolgingsplanLPSBistandUbehandlet)
            assertTrue(pPersonOversiktStatus.dialogmotekandidat!!)

            database.connection.use { connection ->
                val pPersonOppfolgingstilfelleVirksomhetList =
                    connection.getPersonOppfolgingstilfelleVirksomhetList(
                        pPersonOversikStatusId = pPersonOversiktStatus.id,
                    )

                checkPPersonOppfolgingstilfelleVirksomhet(
                    pPersonOppfolgingstilfelleVirksomhetList = pPersonOppfolgingstilfelleVirksomhetList,
                    oppfolgingstilfellePersonRecord = recordValue,
                )
            }

            runBlocking {
                val result = personOppfolgingstilfelleVirksomhetnavnCronjob.runJob()

                assertEquals(0, result.failed)
                assertEquals(2, result.updated)
            }

            runBlocking {
                val result = personOppfolgingstilfelleVirksomhetnavnCronjob.runJob()

                assertEquals(0, result.failed)
                assertEquals(0, result.updated)
            }

            database.connection.use { connection ->
                val pPersonOppfolgingstilfelleVirksomhetList =
                    connection.getPersonOppfolgingstilfelleVirksomhetList(
                        pPersonOversikStatusId = pPersonOversiktStatus.id,
                    )

                checkPPersonOppfolgingstilfelleVirksomhetUpdated(
                    pPersonOppfolgingstilfelleVirksomhetList = pPersonOppfolgingstilfelleVirksomhetList,
                    oppfolgingstilfellePersonRecord = recordValue,
                )
            }
        }

        @Test
        fun `Updates Virksomhetsnavn of existing PersonOppfolgingstilfelleVirksomhet if active aktivitetskrav`() {
            personOversiktStatusRepository.upsertAktivitetskravAktivStatus(
                personident = personIdentDefault,
                isAktivVurdering = true,
            )
            runBlocking { oppfolgingstilfelleConsumer.pollAndProcessRecords(kafkaConsumer = mockKafkaConsumerOppfolgingstilfellePerson) }

            val recordValue = kafkaOppfolgingstilfellePersonServiceRecordRelevant.value()

            val pPersonOversiktStatus = database.connection.use { connection ->
                with(personOversiktStatusRepository) {
                    connection.getPersonStatus(personident = PersonIdent(recordValue.personIdentNumber))!!
                }
            }

            assertNotNull(pPersonOversiktStatus)

            assertTrue(pPersonOversiktStatus.isAktivAktivitetskravvurdering)

            database.connection.use { connection ->
                val pPersonOppfolgingstilfelleVirksomhetList =
                    connection.getPersonOppfolgingstilfelleVirksomhetList(
                        pPersonOversikStatusId = pPersonOversiktStatus.id,
                    )

                checkPPersonOppfolgingstilfelleVirksomhet(
                    pPersonOppfolgingstilfelleVirksomhetList = pPersonOppfolgingstilfelleVirksomhetList,
                    oppfolgingstilfellePersonRecord = recordValue,
                )
            }

            runBlocking {
                val result = personOppfolgingstilfelleVirksomhetnavnCronjob.runJob()

                assertEquals(0, result.failed)
                assertEquals(2, result.updated)
            }

            database.connection.use { connection ->
                val pPersonOppfolgingstilfelleVirksomhetList =
                    connection.getPersonOppfolgingstilfelleVirksomhetList(
                        pPersonOversikStatusId = pPersonOversiktStatus.id,
                    )

                checkPPersonOppfolgingstilfelleVirksomhetUpdated(
                    pPersonOppfolgingstilfelleVirksomhetList = pPersonOppfolgingstilfelleVirksomhetList,
                    oppfolgingstilfellePersonRecord = recordValue,
                )
            }
        }
    }

    @Nested
    @DisplayName("Unsuccessful processing")
    inner class UnsuccessfulProcessing {

        val kafkaOppfolgingstilfellePersonServiceRelevant = generateKafkaOppfolgingstilfellePerson(
            personIdent = personIdentDefault,
            virksomhetsnummerList = listOf(
                VIRKSOMHETSNUMMER_NO_VIRKSOMHETSNAVN,
            )
        )
        val kafkaOppfolgingstilfellePersonServiceRecordRelevant = oppfolgingstilfellePersonConsumerRecord(
            oppfolgingstilfellePersonRecord = kafkaOppfolgingstilfellePersonServiceRelevant
        )

        @BeforeEach
        fun setUp() {
            every { mockKafkaConsumerOppfolgingstilfellePerson.poll(any<Duration>()) } returns ConsumerRecords(
                mapOf(
                    oppfolgingstilfellePersonTopicPartition to listOf(
                        kafkaOppfolgingstilfellePersonServiceRecordRelevant,
                    )
                )
            )
        }

        @Test
        fun `Should fail to update Virksomhetsnavn of existing PersonOppfolgingstilfelleVirksomhet exception is thrown when requesting Virksomhetsnavn from Ereg`() {
            val oversiktHendelseOPLPSBistandMottatt = KPersonoppgavehendelse(
                personIdentDefault.value,
                OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_MOTTATT,
            )
            val personoversiktStatus = PersonOversiktStatus(
                fnr = oversiktHendelseOPLPSBistandMottatt.personident
            ).applyOversikthendelse(oversiktHendelseOPLPSBistandMottatt.hendelsetype)

            database.createPersonOversiktStatus(personoversiktStatus)

            runBlocking { oppfolgingstilfelleConsumer.pollAndProcessRecords(kafkaConsumer = mockKafkaConsumerOppfolgingstilfellePerson) }

            runBlocking {
                val result = personOppfolgingstilfelleVirksomhetnavnCronjob.runJob()

                assertEquals(1, result.failed)
                assertEquals(0, result.updated)
            }
        }
    }
}
