package no.nav.syfo.cronjob.behandlendeenhet

import io.ktor.server.testing.*
import io.ktor.util.*
import io.mockk.clearMocks
import io.mockk.every
import kotlinx.coroutines.runBlocking
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.personstatus.*
import no.nav.syfo.personstatus.domain.OversikthendelseType
import no.nav.syfo.personstatus.domain.VeilederBrukerKnytning
import no.nav.syfo.testutil.*
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_ENHET_ERROR_PERSONIDENT
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_ENHET_NOT_FOUND_PERSONIDENT
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testutil.UserConstants.NAV_ENHET_2
import no.nav.syfo.testutil.assertion.checkPPersonOppfolgingstilfelleVirksomhet
import no.nav.syfo.testutil.generator.*
import no.nav.syfo.testutil.mock.behandlendeEnhetDTO
import no.nav.syfo.util.nowUTC
import org.amshove.kluent.*
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.Duration
import java.time.OffsetDateTime

@InternalAPI
object PersonBehandlendeEnhetCronjobSpek : Spek({

    with(TestApplicationEngine()) {
        start()

        val externalMockEnvironment = ExternalMockEnvironment.instance
        val database = externalMockEnvironment.database

        val internalMockEnvironment = InternalMockEnvironment.instance

        val personBehandlendeEnhetCronjob = internalMockEnvironment.personBehandlendeEnhetCronjob

        val oversiktHendelseService = TestKafkaModule.kafkaOversiktHendelseService
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

        beforeEachTest {
            database.connection.dropData()

            clearMocks(mockKafkaConsumerOppfolgingstilfellePerson)
            clearMocks(mockKafkaConsumerDialogmotekandidatEndring)
            every { mockKafkaConsumerOppfolgingstilfellePerson.commitSync() } returns Unit
            every { mockKafkaConsumerDialogmotekandidatEndring.commitSync() } returns Unit
        }

        describe(PersonBehandlendeEnhetCronjobSpek::class.java.simpleName) {

            describe("Successful processing") {

                val kafkaOppfolgingstilfellePersonServiceRelevant = generateKafkaOppfolgingstilfellePerson(
                    personIdent = personIdentDefault,
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

                it("should not update Enhet of existing PersonOversiktStatus if motebehovUbehandlet, moteplanleggerUbehandlet, oppfolgingsplanLPSBistandUbehandlet and dialogmotekandidat are not true)") {
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
                        pPersonOversiktStatus.moteplanleggerUbehandlet.shouldBeNull()
                        pPersonOversiktStatus.oppfolgingsplanLPSBistandUbehandlet.shouldBeNull()
                        pPersonOversiktStatus.dialogmotekandidat.shouldBeNull()

                        pPersonOversiktStatus.enhet.shouldBeNull()
                        pPersonOversiktStatus.tildeltEnhetUpdatedAt.shouldBeNull()
                        pPersonOversiktStatus.veilederIdent.shouldBeNull()

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
                        val result = personBehandlendeEnhetCronjob.runJob()

                        result.failed shouldBeEqualTo 0
                        result.updated shouldBeEqualTo 0
                    }
                }

                it("should update Enhet and remove Veileder of existing PersonOversiktStatus with Enhet, if motebehovUbehandlet, moteplanleggerUbehandlet, or oppfolgingsplanLPSBistandUbehandlet is true") {
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

                    val firstEnhet = NAV_ENHET_2
                    var tildeltEnhetUpdatedAtBeforeUpdate: OffsetDateTime? = null

                    oversikthendelseList.forEachIndexed { index, oversikthendelse ->

                        database.connection.dropData()

                        oversiktHendelseService.oppdaterPersonMedHendelse(
                            oversikthendelse = oversikthendelse,
                        )

                        database.updatePersonTildeltEnhetAndRemoveTildeltVeileder(
                            personIdent = PersonIdent(oversikthendelse.fnr),
                            enhetId = firstEnhet,
                        )

                        val veilederBrukerKnytning = VeilederBrukerKnytning(
                            veilederIdent = UserConstants.VEILEDER_ID,
                            fnr = oversikthendelse.fnr,
                            enhet = firstEnhet,
                        )
                        database.lagreBrukerKnytningPaEnhet(
                            veilederBrukerKnytning = veilederBrukerKnytning,
                        )

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

                        pPersonOversiktStatus.enhet shouldBeEqualTo firstEnhet
                        pPersonOversiktStatus.tildeltEnhetUpdatedAt.shouldNotBeNull()
                        if (index == 0) {
                            tildeltEnhetUpdatedAtBeforeUpdate = pPersonOversiktStatus.tildeltEnhetUpdatedAt
                        }
                        pPersonOversiktStatus.veilederIdent shouldBeEqualTo veilederBrukerKnytning.veilederIdent
                        pPersonOversiktStatus.dialogmotekandidat.shouldBeNull()
                    }

                    runBlocking {
                        val result = personBehandlendeEnhetCronjob.runJob()

                        result.failed shouldBeEqualTo 0
                        result.updated shouldBeEqualTo 1
                    }

                    database.connection.use { connection ->
                        val pPersonOversiktStatusList = connection.getPersonOversiktStatusList(
                            fnr = personIdentDefault.value,
                        )

                        pPersonOversiktStatusList.size shouldBeEqualTo 1

                        val pPersonOversiktStatus = pPersonOversiktStatusList.first()

                        pPersonOversiktStatus.enhet shouldNotBeEqualTo firstEnhet
                        pPersonOversiktStatus.enhet shouldBeEqualTo behandlendeEnhetDTO().enhetId
                        pPersonOversiktStatus.tildeltEnhetUpdatedAt.shouldNotBeNull()
                        pPersonOversiktStatus.tildeltEnhetUpdatedAt!!.toInstant()
                            .toEpochMilli() shouldBeGreaterThan tildeltEnhetUpdatedAtBeforeUpdate!!.toInstant()
                            .toEpochMilli()
                        pPersonOversiktStatus.veilederIdent.shouldBeNull()
                    }

                    runBlocking {
                        val result = personBehandlendeEnhetCronjob.runJob()

                        result.failed shouldBeEqualTo 0
                        result.updated shouldBeEqualTo 0
                    }
                }

                it("should update Enhet if dialogmotekandidat is true") {
                    every { mockKafkaConsumerDialogmotekandidatEndring.poll(any<Duration>()) } returns ConsumerRecords(
                        mapOf(
                            dialogmoteKandidatTopicPartition to listOf(
                                kafkaDialogmotekandidatEndringStoppunktConsumerRecord,
                            )
                        )
                    )
                    database.connection.dropData()

                    kafkaDialogmotekandidatEndringService.pollAndProcessRecords(
                        kafkaConsumer = mockKafkaConsumerDialogmotekandidatEndring,
                    )

                    var pPersonOversiktStatusList = database.getPersonOversiktStatusList(fnr = personIdentDefault.value)

                    pPersonOversiktStatusList.size shouldBeEqualTo 1

                    var pPersonOversiktStatus = pPersonOversiktStatusList.first()
                    pPersonOversiktStatus.motebehovUbehandlet.shouldBeNull()
                    pPersonOversiktStatus.moteplanleggerUbehandlet.shouldBeNull()
                    pPersonOversiktStatus.oppfolgingsplanLPSBistandUbehandlet.shouldBeNull()
                    pPersonOversiktStatus.enhet.shouldBeNull()
                    pPersonOversiktStatus.dialogmotekandidat shouldBeEqualTo true

                    runBlocking {
                        val result = personBehandlendeEnhetCronjob.runJob()

                        result.failed shouldBeEqualTo 0
                        result.updated shouldBeEqualTo 1
                    }

                    pPersonOversiktStatusList = database.getPersonOversiktStatusList(fnr = personIdentDefault.value)
                    pPersonOversiktStatusList.size shouldBeEqualTo 1

                    pPersonOversiktStatus = pPersonOversiktStatusList.first()

                    pPersonOversiktStatus.enhet.shouldNotBeNull()
                    pPersonOversiktStatus.tildeltEnhetUpdatedAt.shouldNotBeNull()

                    runBlocking {
                        val result = personBehandlendeEnhetCronjob.runJob()

                        result.failed shouldBeEqualTo 0
                        result.updated shouldBeEqualTo 0
                    }
                }

                it("should update Enhet and remove Veileder of existing PersonOversiktStatus with no Enhet if oppfolgingsplanLPSBistandUbehandlet is true") {
                    val oversikthendelse = generateKOversikthendelse(
                        oversikthendelseType = OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_MOTTATT,
                        personIdent = personIdentDefault.value,
                    )

                    var tildeltEnhetUpdatedAtBeforeUpdate: OffsetDateTime?

                    oversiktHendelseService.oppdaterPersonMedHendelse(
                        oversikthendelse = oversikthendelse,
                    )

                    val veilederBrukerKnytning = VeilederBrukerKnytning(
                        veilederIdent = UserConstants.VEILEDER_ID,
                        fnr = oversikthendelse.fnr,
                        enhet = behandlendeEnhetDTO().enhetId,
                    )
                    database.lagreBrukerKnytningPaEnhet(
                        veilederBrukerKnytning = veilederBrukerKnytning,
                    )

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
                        pPersonOversiktStatus.moteplanleggerUbehandlet.shouldBeNull()
                        pPersonOversiktStatus.oppfolgingsplanLPSBistandUbehandlet shouldBeEqualTo true

                        pPersonOversiktStatus.enhet.shouldBeNull()
                        pPersonOversiktStatus.tildeltEnhetUpdatedAt.shouldBeNull()
                        tildeltEnhetUpdatedAtBeforeUpdate = pPersonOversiktStatus.tildeltEnhetUpdatedAt
                        pPersonOversiktStatus.veilederIdent shouldBeEqualTo veilederBrukerKnytning.veilederIdent
                    }

                    runBlocking {
                        val result = personBehandlendeEnhetCronjob.runJob()

                        result.failed shouldBeEqualTo 0
                        result.updated shouldBeEqualTo 1
                    }

                    database.connection.use { connection ->
                        val pPersonOversiktStatusList = connection.getPersonOversiktStatusList(
                            fnr = personIdentDefault.value,
                        )

                        pPersonOversiktStatusList.size shouldBeEqualTo 1

                        val pPersonOversiktStatus = pPersonOversiktStatusList.first()

                        pPersonOversiktStatus.enhet shouldBeEqualTo behandlendeEnhetDTO().enhetId
                        pPersonOversiktStatus.tildeltEnhetUpdatedAt.shouldNotBeNull()
                        pPersonOversiktStatus.tildeltEnhetUpdatedAt shouldNotBeEqualTo tildeltEnhetUpdatedAtBeforeUpdate
                        pPersonOversiktStatus.veilederIdent.shouldBeNull()
                    }

                    runBlocking {
                        val result = personBehandlendeEnhetCronjob.runJob()

                        result.failed shouldBeEqualTo 0
                        result.updated shouldBeEqualTo 0
                    }
                }

                it("should update tildeltEnhetUpdatedAt, but not tildeltEnhet, of existing PersonOversiktStatus, if BehandlendeEnhet is not found and oppfolgingsplanLPSBistandUbehandlet is true") {
                    val personIdent = ARBEIDSTAKER_ENHET_NOT_FOUND_PERSONIDENT

                    val oversikthendelse = generateKOversikthendelse(
                        oversikthendelseType = OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_MOTTATT,
                        personIdent = personIdent.value,
                    )
                    oversiktHendelseService.oppdaterPersonMedHendelse(
                        oversikthendelse = oversikthendelse,
                    )

                    var tildeltEnhetUpdatedAtBeforeUpdate: OffsetDateTime?

                    database.connection.use { connection ->
                        val pPersonOversiktStatusList = connection.getPersonOversiktStatusList(
                            fnr = personIdent.value,
                        )

                        pPersonOversiktStatusList.size shouldBeEqualTo 1

                        val pPersonOversiktStatus = pPersonOversiktStatusList.first()
                        pPersonOversiktStatus.enhet.shouldBeNull()
                        pPersonOversiktStatus.tildeltEnhetUpdatedAt.shouldBeNull()
                        pPersonOversiktStatus.veilederIdent.shouldBeNull()

                        tildeltEnhetUpdatedAtBeforeUpdate = pPersonOversiktStatus.tildeltEnhetUpdatedAt
                    }

                    runBlocking {
                        val result = personBehandlendeEnhetCronjob.runJob()

                        result.failed shouldBeEqualTo 0
                        result.updated shouldBeEqualTo 1
                    }

                    database.connection.use { connection ->
                        val pPersonOversiktStatusList = connection.getPersonOversiktStatusList(
                            fnr = personIdent.value,
                        )

                        pPersonOversiktStatusList.size shouldBeEqualTo 1

                        val pPersonOversiktStatus = pPersonOversiktStatusList.first()
                        pPersonOversiktStatus.enhet.shouldBeNull()
                        pPersonOversiktStatus.tildeltEnhetUpdatedAt.shouldNotBeNull()
                        pPersonOversiktStatus shouldNotBeEqualTo tildeltEnhetUpdatedAtBeforeUpdate
                        pPersonOversiktStatus.veilederIdent.shouldBeNull()

                        tildeltEnhetUpdatedAtBeforeUpdate = pPersonOversiktStatus.tildeltEnhetUpdatedAt
                    }

                    val kafkaOppfolgingstilfellePersonServiceRelevantEnhetNotFound =
                        generateKafkaOppfolgingstilfellePerson(
                            personIdent = personIdent,
                        )
                    val kafkaOppfolgingstilfellePersonServiceRecordRelevantEnhetNotFound = oppfolgingstilfellePersonConsumerRecord(
                        kafkaOppfolgingstilfellePerson = kafkaOppfolgingstilfellePersonServiceRelevantEnhetNotFound,
                    )

                    every { mockKafkaConsumerOppfolgingstilfellePerson.poll(any<Duration>()) } returns ConsumerRecords(
                        mapOf(
                            oppfolgingstilfellePersonTopicPartition to listOf(
                                kafkaOppfolgingstilfellePersonServiceRecordRelevantEnhetNotFound,
                            )
                        )
                    )

                    kafkaOppfolgingstilfellePersonService.pollAndProcessRecords(
                        kafkaConsumer = mockKafkaConsumerOppfolgingstilfellePerson,
                    )

                    runBlocking {
                        val result = personBehandlendeEnhetCronjob.runJob()

                        result.failed shouldBeEqualTo 0
                        result.updated shouldBeEqualTo 1
                    }

                    database.connection.use { connection ->
                        val pPersonOversiktStatusList = connection.getPersonOversiktStatusList(
                            fnr = personIdent.value,
                        )

                        pPersonOversiktStatusList.size shouldBeEqualTo 1

                        val pPersonOversiktStatus = pPersonOversiktStatusList.first()
                        pPersonOversiktStatus.enhet.shouldBeNull()
                        pPersonOversiktStatus.tildeltEnhetUpdatedAt.shouldNotBeNull()
                        pPersonOversiktStatus.tildeltEnhetUpdatedAt!!.toInstant()
                            .toEpochMilli() shouldBeGreaterThan tildeltEnhetUpdatedAtBeforeUpdate!!.toInstant()
                            .toEpochMilli()
                        pPersonOversiktStatus.veilederIdent.shouldBeNull()
                    }

                    runBlocking {
                        val result = personBehandlendeEnhetCronjob.runJob()

                        result.failed shouldBeEqualTo 0
                        result.updated shouldBeEqualTo 0
                    }
                }
            }

            describe("Unsuccessful processing") {

                val kafkaOppfolgingstilfellePersonServiceRelevant = generateKafkaOppfolgingstilfellePerson(
                    personIdent = ARBEIDSTAKER_ENHET_ERROR_PERSONIDENT,
                )
                val kafkaOppfolgingstilfellePersonServiceRecordRelevant = oppfolgingstilfellePersonConsumerRecord(
                    kafkaOppfolgingstilfellePerson = kafkaOppfolgingstilfellePersonServiceRelevant,
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

                it("should fail to update Enhet of existing PersonOversiktStatus exception is thrown when requesting Enhet from Syfobehandlendeenhet") {
                    val oversikthendelse = generateKOversikthendelse(
                        oversikthendelseType = OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_MOTTATT,
                        personIdent = ARBEIDSTAKER_ENHET_ERROR_PERSONIDENT.value,
                    )

                    oversiktHendelseService.oppdaterPersonMedHendelse(
                        oversikthendelse = oversikthendelse,
                    )

                    kafkaOppfolgingstilfellePersonService.pollAndProcessRecords(
                        kafkaConsumer = mockKafkaConsumerOppfolgingstilfellePerson,
                    )

                    runBlocking {
                        val result = personBehandlendeEnhetCronjob.runJob()

                        result.failed shouldBeEqualTo 1
                        result.updated shouldBeEqualTo 0
                    }
                }
            }
        }
    }
})
