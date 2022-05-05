package no.nav.syfo.cronjob.virksomhetsnavn

import io.ktor.server.testing.*
import io.ktor.util.*
import io.mockk.clearMocks
import io.mockk.every
import kotlinx.coroutines.runBlocking
import no.nav.syfo.dialogmotekandidat.kafka.DIALOGMOTEKANDIDAT_TOPIC
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.domain.Virksomhetsnummer
import no.nav.syfo.oppfolgingstilfelle.kafka.OPPFOLGINGSTILFELLE_PERSON_TOPIC
import no.nav.syfo.personstatus.domain.OversikthendelseType
import no.nav.syfo.personstatus.getPersonOppfolgingstilfelleVirksomhetList
import no.nav.syfo.personstatus.getPersonOversiktStatusList
import no.nav.syfo.testutil.*
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testutil.UserConstants.VIRKSOMHETSNUMMER_DEFAULT
import no.nav.syfo.testutil.UserConstants.VIRKSOMHETSNUMMER_NO_VIRKSOMHETSNAVN
import no.nav.syfo.testutil.assertion.checkPPersonOppfolgingstilfelleVirksomhet
import no.nav.syfo.testutil.generator.*
import no.nav.syfo.util.nowUTC
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.common.TopicPartition
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.Duration

@InternalAPI
object PersonOppfolgingstilfelleVirksomhetsnavnCronjobSpek : Spek({

    with(TestApplicationEngine()) {
        start()

        val externalMockEnvironment = ExternalMockEnvironment.instance
        val database = externalMockEnvironment.database

        val internalMockEnvironment = InternalMockEnvironment.instance

        val personOppfolgingstilfelleVirksomhetnavnCronjob =
            internalMockEnvironment.personOppfolgingstilfelleVirksomhetnavnCronjob

        val oversiktHendelseService = internalMockEnvironment.oversiktHendelseService
        val kafkaOppfolgingstilfellePersonService = internalMockEnvironment.kafkaOppfolgingstilfellePersonService

        val mockKafkaConsumerOppfolgingstilfellePerson =
            internalMockEnvironment.kafkaConsumerOppfolgingstilfellePerson

        val partition = 0
        val oppfolgingstilfellePersonTopicPartition = TopicPartition(
            OPPFOLGINGSTILFELLE_PERSON_TOPIC,
            partition,
        )
        val personIdentDefault = PersonIdent(ARBEIDSTAKER_FNR)

        val kafkaDialogmotekandidatEndringService = internalMockEnvironment.kafkaDialogmotekandidatEndringService
        val mockKafkaConsumerDialogmotekandidatEndring =
            internalMockEnvironment.kafkaConsumerDialogmotekandidatEndring
        val dialogmoteKandidatTopicPartition = TopicPartition(
            DIALOGMOTEKANDIDAT_TOPIC,
            partition
        )
        val kafkaDialogmotekandidatEndringStoppunkt = generateKafkaDialogmotekandidatEndringStoppunkt(
            personIdent = personIdentDefault.value,
            createdAt = nowUTC().minusDays(1)
        )
        val kafkaDialogmotekandidatEndringStoppunktConsumerRecord = ConsumerRecord(
            DIALOGMOTEKANDIDAT_TOPIC,
            partition,
            1,
            "key2",
            kafkaDialogmotekandidatEndringStoppunkt
        )

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
                val kafkaOppfolgingstilfellePersonServiceRecordRelevant = ConsumerRecord(
                    OPPFOLGINGSTILFELLE_PERSON_TOPIC,
                    partition,
                    1,
                    "key1",
                    kafkaOppfolgingstilfellePersonServiceRelevant,
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

                it("should not update Virksomhetsnavn of existing PersonOppfolgingstilfelleVirksomhet if motebehovUbehandlet, moteplanleggerUbehandlet, oppfolgingsplanLPSBistandUbehandlet and dialogmotekandidat are not true)") {
                    kafkaOppfolgingstilfellePersonService.pollAndProcessRecords(
                        kafkaConsumerOppfolgingstilfellePerson = mockKafkaConsumerOppfolgingstilfellePerson,
                    )

                    val recordValue = kafkaOppfolgingstilfellePersonServiceRecordRelevant.value()

                    database.connection.use { connection ->
                        val pPersonOversiktStatusList = connection.getPersonOversiktStatusList(
                            fnr = recordValue.personIdentNumber,
                        )

                        pPersonOversiktStatusList.size shouldBeEqualTo 1

                        val pPersonOversiktStatus = pPersonOversiktStatusList.first()

                        pPersonOversiktStatus.motebehovUbehandlet.shouldBeNull()
                        pPersonOversiktStatus.moteplanleggerUbehandlet.shouldBeNull()
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

                it("should update Virksomhetsnavn of existing PersonOppfolgingstilfelleVirksomhet if motebehovUbehandlet, moteplanleggerUbehandlet, or oppfolgingsplanLPSBistandUbehandlet is true") {
                    val oversiktHendelseMotebehovSvarMottatt = generateKOversikthendelse(
                        oversikthendelseType = OversikthendelseType.MOTEBEHOV_SVAR_MOTTATT,
                        personIdent = personIdentDefault.value,
                    )
                    val oversiktHendelseMoteplanleggerAlleSvarMottatt = generateKOversikthendelse(
                        oversikthendelseType = OversikthendelseType.MOTEPLANLEGGER_ALLE_SVAR_MOTTATT,
                        personIdent = personIdentDefault.value,
                    )
                    val oversiktHendelseOPLPSBistandMottatt = generateKOversikthendelse(
                        oversikthendelseType = OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_MOTTATT,
                        personIdent = personIdentDefault.value,
                    )
                    val oversikthendelseList = listOf(
                        oversiktHendelseOPLPSBistandMottatt,
                        oversiktHendelseMotebehovSvarMottatt,
                        oversiktHendelseMoteplanleggerAlleSvarMottatt
                    )
                    oversikthendelseList.forEach { oversikthendelse ->
                        database.connection.dropData()

                        oversiktHendelseService.oppdaterPersonMedHendelse(
                            oversiktHendelse = oversikthendelse,
                        )

                        kafkaOppfolgingstilfellePersonService.pollAndProcessRecords(
                            kafkaConsumerOppfolgingstilfellePerson = mockKafkaConsumerOppfolgingstilfellePerson,
                        )

                        val recordValue = kafkaOppfolgingstilfellePersonServiceRecordRelevant.value()

                        val pPersonOversiktStatusList = database.connection.use { connection ->
                            connection.getPersonOversiktStatusList(
                                fnr = recordValue.personIdentNumber,
                            )
                        }

                        pPersonOversiktStatusList.size shouldBeEqualTo 1

                        val pPersonOversiktStatus = pPersonOversiktStatusList.first()

                        if (oversikthendelse.hendelseId == OversikthendelseType.MOTEBEHOV_SVAR_MOTTATT.name) {
                            pPersonOversiktStatus.motebehovUbehandlet shouldBeEqualTo true
                        } else {
                            pPersonOversiktStatus.motebehovUbehandlet.shouldBeNull()
                        }
                        if (oversikthendelse.hendelseId == OversikthendelseType.MOTEPLANLEGGER_ALLE_SVAR_MOTTATT.name) {
                            pPersonOversiktStatus.moteplanleggerUbehandlet shouldBeEqualTo true
                        } else {
                            pPersonOversiktStatus.moteplanleggerUbehandlet.shouldBeNull()
                        }
                        if (oversikthendelse.hendelseId == OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_MOTTATT.name) {
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
                        kafkaConsumerOppfolgingstilfellePerson = mockKafkaConsumerOppfolgingstilfellePerson,
                    )

                    val recordValue = kafkaOppfolgingstilfellePersonServiceRecordRelevant.value()

                    val pPersonOversiktStatusList = database.getPersonOversiktStatusList(
                        fnr = recordValue.personIdentNumber,
                    )

                    pPersonOversiktStatusList.size shouldBeEqualTo 1

                    val pPersonOversiktStatus = pPersonOversiktStatusList.first()

                    pPersonOversiktStatus.motebehovUbehandlet.shouldBeNull()
                    pPersonOversiktStatus.moteplanleggerUbehandlet.shouldBeNull()
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
            }

            describe("Unsuccessful processing") {

                val kafkaOppfolgingstilfellePersonServiceRelevant = generateKafkaOppfolgingstilfellePerson(
                    personIdent = personIdentDefault,
                    virksomhetsnummerList = listOf(
                        VIRKSOMHETSNUMMER_NO_VIRKSOMHETSNAVN,
                    )
                )
                val kafkaOppfolgingstilfellePersonServiceRecordRelevant = ConsumerRecord(
                    OPPFOLGINGSTILFELLE_PERSON_TOPIC,
                    partition,
                    1,
                    "key1",
                    kafkaOppfolgingstilfellePersonServiceRelevant,
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
                    val oversikthendelse = generateKOversikthendelse(
                        oversikthendelseType = OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_MOTTATT,
                        personIdent = personIdentDefault.value,
                    )

                    oversiktHendelseService.oppdaterPersonMedHendelse(
                        oversiktHendelse = oversikthendelse,
                    )

                    kafkaOppfolgingstilfellePersonService.pollAndProcessRecords(
                        kafkaConsumerOppfolgingstilfellePerson = mockKafkaConsumerOppfolgingstilfellePerson,
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
