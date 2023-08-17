package no.nav.syfo.cronjob.virksomhetsnavn

import io.ktor.server.testing.*
import io.ktor.util.*
import io.mockk.clearMocks
import io.mockk.every
import kotlinx.coroutines.runBlocking
import no.nav.syfo.aktivitetskravvurdering.domain.AktivitetskravStatus
import no.nav.syfo.aktivitetskravvurdering.persistAktivitetskrav
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.domain.Virksomhetsnummer
import no.nav.syfo.personoppgavehendelse.kafka.KPersonoppgavehendelse
import no.nav.syfo.personstatus.domain.OversikthendelseType
import no.nav.syfo.personstatus.getPersonOppfolgingstilfelleVirksomhetList
import no.nav.syfo.personstatus.db.*
import no.nav.syfo.personstatus.domain.PersonOversiktStatus
import no.nav.syfo.personstatus.domain.applyHendelse
import no.nav.syfo.testutil.*
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testutil.UserConstants.VIRKSOMHETSNUMMER_DEFAULT
import no.nav.syfo.testutil.UserConstants.VIRKSOMHETSNUMMER_NO_VIRKSOMHETSNAVN
import no.nav.syfo.testutil.assertion.checkPPersonOppfolgingstilfelleVirksomhet
import no.nav.syfo.testutil.generator.*
import no.nav.syfo.util.nowUTC
import org.amshove.kluent.*
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.*

@InternalAPI
object PersonOppfolgingstilfelleVirksomhetsnavnCronjobSpek : Spek({

    with(TestApplicationEngine()) {
        start()

        val externalMockEnvironment = ExternalMockEnvironment.instance
        val database = externalMockEnvironment.database

        val internalMockEnvironment = InternalMockEnvironment.instance

        val personOppfolgingstilfelleVirksomhetnavnCronjob =
            internalMockEnvironment.personOppfolgingstilfelleVirksomhetnavnCronjob

        val kafkaOppfolgingstilfellePersonService = TestKafkaModule.kafkaOppfolgingstilfellePersonService

        val mockKafkaConsumerOppfolgingstilfellePerson =
            TestKafkaModule.kafkaConsumerOppfolgingstilfellePerson

        val oppfolgingstilfellePersonTopicPartition = oppfolgingstilfellePersonTopicPartition()
        val personIdentDefault = PersonIdent(ARBEIDSTAKER_FNR)

        val kafkaDialogmotekandidatEndringService = TestKafkaModule.kafkaDialogmotekandidatEndringService
        val mockKafkaConsumerDialogmotekandidatEndring =
            TestKafkaModule.kafkaConsumerDialogmotekandidatEndring
        val dialogmoteKandidatTopicPartition = dialogmotekandidatEndringTopicPartition()
        val kafkaDialogmotekandidatEndringStoppunkt = generateKafkaDialogmotekandidatEndringStoppunkt(
            personIdent = personIdentDefault.value,
            createdAt = nowUTC().minusDays(1)
        )
        val kafkaDialogmotekandidatEndringStoppunktConsumerRecord = dialogmotekandidatEndringConsumerRecord(
            kafkaDialogmotekandidatEndring = kafkaDialogmotekandidatEndringStoppunkt
        )
        val aktivitetskravGenerator =
            AktivitetskravGenerator(arenaCutoff = externalMockEnvironment.environment.arenaCutoff)

        beforeEachTest {
            database.connection.dropData()

            clearMocks(mockKafkaConsumerOppfolgingstilfellePerson)
            clearMocks(mockKafkaConsumerDialogmotekandidatEndring)
            every { mockKafkaConsumerOppfolgingstilfellePerson.commitSync() } returns Unit
            every { mockKafkaConsumerDialogmotekandidatEndring.commitSync() } returns Unit
        }

        describe(PersonOppfolgingstilfelleVirksomhetsnavnCronjobSpek::class.java.simpleName) {

            describe("Successful processing") {

                val kafkaOppfolgingstilfellePersonServiceRelevant = generateKafkaOppfolgingstilfellePerson(
                    personIdent = personIdentDefault,
                    virksomhetsnummerList = listOf(
                        VIRKSOMHETSNUMMER_DEFAULT,
                        Virksomhetsnummer(UserConstants.VIRKSOMHETSNUMMER_2),
                    )
                )
                val kafkaOppfolgingstilfellePersonServiceRecordRelevant = oppfolgingstilfellePersonConsumerRecord(
                    kafkaOppfolgingstilfellePerson = kafkaOppfolgingstilfellePersonServiceRelevant
                )

                beforeEachTest {
                    every { mockKafkaConsumerOppfolgingstilfellePerson.poll(any<Duration>()) } returns ConsumerRecords(
                        mapOf(
                            oppfolgingstilfellePersonTopicPartition to listOf(
                                kafkaOppfolgingstilfellePersonServiceRecordRelevant,
                            )
                        )
                    )
                }

                it("should not update Virksomhetsnavn of existing PersonOppfolgingstilfelleVirksomhet if motebehovUbehandlet, oppfolgingsplanLPSBistandUbehandlet and dialogmotekandidat are not true)") {
                    kafkaOppfolgingstilfellePersonService.pollAndProcessRecords(
                        kafkaConsumer = mockKafkaConsumerOppfolgingstilfellePerson,
                    )

                    val recordValue = kafkaOppfolgingstilfellePersonServiceRecordRelevant.value()

                    database.connection.use { connection ->
                        val pPersonOversiktStatusList = connection.getPersonOversiktStatusList(
                            fnr = recordValue.personIdentNumber,
                        )

                        pPersonOversiktStatusList.size shouldBeEqualTo 1

                        val pPersonOversiktStatus = pPersonOversiktStatusList.first()

                        pPersonOversiktStatus.motebehovUbehandlet.shouldBeNull()
                        pPersonOversiktStatus.oppfolgingsplanLPSBistandUbehandlet.shouldBeNull()

                        val pPersonOppfolgingstilfelleVirksomhetList =
                            connection.getPersonOppfolgingstilfelleVirksomhetList(
                                pPersonOversikStatusId = pPersonOversiktStatus.id,
                            )

                        checkPPersonOppfolgingstilfelleVirksomhet(
                            pPersonOppfolgingstilfelleVirksomhetList = pPersonOppfolgingstilfelleVirksomhetList,
                            kafkaOppfolgingstilfellePerson = recordValue,
                            updated = false,
                        )

                        runBlocking {
                            val result = personOppfolgingstilfelleVirksomhetnavnCronjob.runJob()

                            result.failed shouldBeEqualTo 0
                            result.updated shouldBeEqualTo 0
                        }
                    }
                }

                it("should update Virksomhetsnavn of existing PersonOppfolgingstilfelleVirksomhet if motebehovUbehandlet, or oppfolgingsplanLPSBistandUbehandlet is true") {
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
                        database.connection.dropData()

                        val personoversiktStatus = PersonOversiktStatus(
                            fnr = oversikthendelse.personident
                        ).applyHendelse(oversikthendelse.hendelsetype)

                        database.createPersonOversiktStatus(personoversiktStatus)

                        kafkaOppfolgingstilfellePersonService.pollAndProcessRecords(
                            kafkaConsumer = mockKafkaConsumerOppfolgingstilfellePerson,
                        )

                        val recordValue = kafkaOppfolgingstilfellePersonServiceRecordRelevant.value()

                        val pPersonOversiktStatusList = database.connection.use { connection ->
                            connection.getPersonOversiktStatusList(
                                fnr = recordValue.personIdentNumber,
                            )
                        }

                        pPersonOversiktStatusList.size shouldBeEqualTo 1

                        val pPersonOversiktStatus = pPersonOversiktStatusList.first()

                        if (oversikthendelse.hendelsetype == OversikthendelseType.MOTEBEHOV_SVAR_MOTTATT) {
                            pPersonOversiktStatus.motebehovUbehandlet shouldBeEqualTo true
                        } else {
                            pPersonOversiktStatus.motebehovUbehandlet.shouldBeNull()
                        }
                        if (oversikthendelse.hendelsetype == OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_MOTTATT) {
                            pPersonOversiktStatus.oppfolgingsplanLPSBistandUbehandlet shouldBeEqualTo true
                        } else {
                            pPersonOversiktStatus.oppfolgingsplanLPSBistandUbehandlet.shouldBeNull()
                        }

                        database.connection.use { connection ->
                            val pPersonOppfolgingstilfelleVirksomhetList =
                                connection.getPersonOppfolgingstilfelleVirksomhetList(
                                    pPersonOversikStatusId = pPersonOversiktStatus.id,
                                )

                            checkPPersonOppfolgingstilfelleVirksomhet(
                                pPersonOppfolgingstilfelleVirksomhetList = pPersonOppfolgingstilfelleVirksomhetList,
                                kafkaOppfolgingstilfellePerson = recordValue,
                                updated = false,
                            )
                        }

                        runBlocking {
                            val result = personOppfolgingstilfelleVirksomhetnavnCronjob.runJob()

                            result.failed shouldBeEqualTo 0
                            result.updated shouldBeEqualTo 2
                        }

                        runBlocking {
                            val result = personOppfolgingstilfelleVirksomhetnavnCronjob.runJob()

                            result.failed shouldBeEqualTo 0
                            result.updated shouldBeEqualTo 0
                        }

                        database.connection.use { connection ->
                            val pPersonOppfolgingstilfelleVirksomhetList =
                                connection.getPersonOppfolgingstilfelleVirksomhetList(
                                    pPersonOversikStatusId = pPersonOversiktStatus.id,
                                )

                            checkPPersonOppfolgingstilfelleVirksomhet(
                                pPersonOppfolgingstilfelleVirksomhetList = pPersonOppfolgingstilfelleVirksomhetList,
                                kafkaOppfolgingstilfellePerson = recordValue,
                                updated = true,
                            )
                        }
                    }
                }

                it("should update Virksomhetsnavn of existing PersonOppfolgingstilfelleVirksomhet if dialogmotekandidat is true") {
                    every { mockKafkaConsumerDialogmotekandidatEndring.poll(any<Duration>()) } returns ConsumerRecords(
                        mapOf(
                            dialogmoteKandidatTopicPartition to listOf(
                                kafkaDialogmotekandidatEndringStoppunktConsumerRecord,
                            )
                        )
                    )
                    kafkaDialogmotekandidatEndringService.pollAndProcessRecords(
                        kafkaConsumer = mockKafkaConsumerDialogmotekandidatEndring,
                    )
                    kafkaOppfolgingstilfellePersonService.pollAndProcessRecords(
                        kafkaConsumer = mockKafkaConsumerOppfolgingstilfellePerson,
                    )

                    val recordValue = kafkaOppfolgingstilfellePersonServiceRecordRelevant.value()

                    val pPersonOversiktStatusList = database.getPersonOversiktStatusList(
                        fnr = recordValue.personIdentNumber,
                    )

                    pPersonOversiktStatusList.size shouldBeEqualTo 1

                    val pPersonOversiktStatus = pPersonOversiktStatusList.first()

                    pPersonOversiktStatus.motebehovUbehandlet.shouldBeNull()
                    pPersonOversiktStatus.oppfolgingsplanLPSBistandUbehandlet.shouldBeNull()
                    pPersonOversiktStatus.dialogmotekandidat shouldBeEqualTo true

                    database.connection.use { connection ->
                        val pPersonOppfolgingstilfelleVirksomhetList =
                            connection.getPersonOppfolgingstilfelleVirksomhetList(
                                pPersonOversikStatusId = pPersonOversiktStatus.id,
                            )

                        checkPPersonOppfolgingstilfelleVirksomhet(
                            pPersonOppfolgingstilfelleVirksomhetList = pPersonOppfolgingstilfelleVirksomhetList,
                            kafkaOppfolgingstilfellePerson = recordValue,
                            updated = false,
                        )
                    }

                    runBlocking {
                        val result = personOppfolgingstilfelleVirksomhetnavnCronjob.runJob()

                        result.failed shouldBeEqualTo 0
                        result.updated shouldBeEqualTo 2
                    }

                    runBlocking {
                        val result = personOppfolgingstilfelleVirksomhetnavnCronjob.runJob()

                        result.failed shouldBeEqualTo 0
                        result.updated shouldBeEqualTo 0
                    }

                    database.connection.use { connection ->
                        val pPersonOppfolgingstilfelleVirksomhetList =
                            connection.getPersonOppfolgingstilfelleVirksomhetList(
                                pPersonOversikStatusId = pPersonOversiktStatus.id,
                            )

                        checkPPersonOppfolgingstilfelleVirksomhet(
                            pPersonOppfolgingstilfelleVirksomhetList = pPersonOppfolgingstilfelleVirksomhetList,
                            kafkaOppfolgingstilfellePerson = recordValue,
                            updated = true,
                        )
                    }
                }

                it("updates Virksomhetsnavn of existing PersonOppfolgingstilfelleVirksomhet if active aktivitetskrav") {
                    val aktivitetskrav = aktivitetskravGenerator.generateAktivitetskrav(
                        personIdent = personIdentDefault,
                        status = AktivitetskravStatus.AVVENT,
                        stoppunktAfterCutoff = true,
                    )
                    database.connection.use { connection ->
                        persistAktivitetskrav(
                            connection = connection,
                            aktivitetskrav = aktivitetskrav,
                        )
                        connection.commit()
                    }

                    kafkaOppfolgingstilfellePersonService.pollAndProcessRecords(
                        kafkaConsumer = mockKafkaConsumerOppfolgingstilfellePerson,
                    )

                    val recordValue = kafkaOppfolgingstilfellePersonServiceRecordRelevant.value()

                    val pPersonOversiktStatusList = database.getPersonOversiktStatusList(
                        fnr = recordValue.personIdentNumber,
                    )

                    pPersonOversiktStatusList.size shouldBeEqualTo 1

                    val pPersonOversiktStatus = pPersonOversiktStatusList.first()
                    pPersonOversiktStatus.aktivitetskrav.shouldNotBeNull()

                    database.connection.use { connection ->
                        val pPersonOppfolgingstilfelleVirksomhetList =
                            connection.getPersonOppfolgingstilfelleVirksomhetList(
                                pPersonOversikStatusId = pPersonOversiktStatus.id,
                            )

                        checkPPersonOppfolgingstilfelleVirksomhet(
                            pPersonOppfolgingstilfelleVirksomhetList = pPersonOppfolgingstilfelleVirksomhetList,
                            kafkaOppfolgingstilfellePerson = recordValue,
                            updated = false,
                        )
                    }

                    runBlocking {
                        val result = personOppfolgingstilfelleVirksomhetnavnCronjob.runJob()

                        result.failed shouldBeEqualTo 0
                        result.updated shouldBeEqualTo 2
                    }

                    database.connection.use { connection ->
                        val pPersonOppfolgingstilfelleVirksomhetList =
                            connection.getPersonOppfolgingstilfelleVirksomhetList(
                                pPersonOversikStatusId = pPersonOversiktStatus.id,
                            )

                        checkPPersonOppfolgingstilfelleVirksomhet(
                            pPersonOppfolgingstilfelleVirksomhetList = pPersonOppfolgingstilfelleVirksomhetList,
                            kafkaOppfolgingstilfellePerson = recordValue,
                            updated = true,
                        )
                    }
                }
            }

            describe("Unsuccessful processing") {

                val kafkaOppfolgingstilfellePersonServiceRelevant = generateKafkaOppfolgingstilfellePerson(
                    personIdent = personIdentDefault,
                    virksomhetsnummerList = listOf(
                        VIRKSOMHETSNUMMER_NO_VIRKSOMHETSNAVN,
                    )
                )
                val kafkaOppfolgingstilfellePersonServiceRecordRelevant = oppfolgingstilfellePersonConsumerRecord(
                    kafkaOppfolgingstilfellePerson = kafkaOppfolgingstilfellePersonServiceRelevant
                )

                beforeEachTest {
                    every { mockKafkaConsumerOppfolgingstilfellePerson.poll(any<Duration>()) } returns ConsumerRecords(
                        mapOf(
                            oppfolgingstilfellePersonTopicPartition to listOf(
                                kafkaOppfolgingstilfellePersonServiceRecordRelevant,
                            )
                        )
                    )
                }

                it("should fail to update Virksomhetsnavn of existing PersonOppfolgingstilfelleVirksomhet exception is thrown when requesting Virksomhetsnavn from Ereg") {
                    val oversiktHendelseOPLPSBistandMottatt = KPersonoppgavehendelse(
                        personIdentDefault.value,
                        OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_MOTTATT,
                    )
                    val personoversiktStatus = PersonOversiktStatus(
                        fnr = oversiktHendelseOPLPSBistandMottatt.personident
                    ).applyHendelse(oversiktHendelseOPLPSBistandMottatt.hendelsetype)

                    database.createPersonOversiktStatus(personoversiktStatus)

                    kafkaOppfolgingstilfellePersonService.pollAndProcessRecords(
                        kafkaConsumer = mockKafkaConsumerOppfolgingstilfellePerson,
                    )

                    runBlocking {
                        val result = personOppfolgingstilfelleVirksomhetnavnCronjob.runJob()

                        result.failed shouldBeEqualTo 1
                        result.updated shouldBeEqualTo 0
                    }
                }
            }
        }
    }
})
