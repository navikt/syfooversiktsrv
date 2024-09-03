package no.nav.syfo.cronjob.behandlendeenhet

import io.ktor.server.testing.*
import io.ktor.util.*
import io.mockk.clearMocks
import io.mockk.every
import kotlinx.coroutines.runBlocking
import no.nav.syfo.personstatus.domain.PersonIdent
import no.nav.syfo.personoppgavehendelse.kafka.KPersonoppgavehendelse
import no.nav.syfo.personstatus.db.*
import no.nav.syfo.personstatus.domain.OversikthendelseType
import no.nav.syfo.personstatus.domain.PersonOversiktStatus
import no.nav.syfo.personstatus.domain.VeilederBrukerKnytning
import no.nav.syfo.personstatus.domain.applyHendelse
import no.nav.syfo.personstatus.infrastructure.cronjob.behandlendeenhet.updatePersonTildeltEnhetAndRemoveTildeltVeileder
import no.nav.syfo.personstatus.infrastructure.database.repository.PersonOversiktStatusRepository
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
import java.time.*

@InternalAPI
object PersonBehandlendeEnhetCronjobSpek : Spek({

    with(TestApplicationEngine()) {
        start()

        val externalMockEnvironment = ExternalMockEnvironment.instance
        val database = externalMockEnvironment.database

        val internalMockEnvironment = InternalMockEnvironment.instance

        val personBehandlendeEnhetCronjob = internalMockEnvironment.personBehandlendeEnhetCronjob

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

        val personOversiktStatusRepository = PersonOversiktStatusRepository(database = database)

        beforeEachTest {
            database.dropData()

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

                it("should not update Enhet of existing PersonOversiktStatus if motebehovUbehandlet, oppfolgingsplanLPSBistandUbehandlet and dialogmotekandidat are not true)") {
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

                it("should update Enhet and remove Veileder of existing PersonOversiktStatus with Enhet, if motebehovUbehandlet, or oppfolgingsplanLPSBistandUbehandlet is true") {
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

                    val firstEnhet = NAV_ENHET_2
                    var tildeltEnhetUpdatedAtBeforeUpdate: OffsetDateTime? = null

                    oversikthendelseList.forEachIndexed { index, oversikthendelse ->

                        database.dropData()

                        val personoversiktStatus = PersonOversiktStatus(
                            fnr = oversikthendelse.personident
                        ).applyHendelse(oversikthendelse.hendelsetype)
                        database.createPersonOversiktStatus(personoversiktStatus)

                        database.updatePersonTildeltEnhetAndRemoveTildeltVeileder(
                            personIdent = PersonIdent(oversikthendelse.personident),
                            enhetId = firstEnhet,
                        )
                        database.updateTildeltEnhetUpdatedAt(
                            ident = PersonIdent(oversikthendelse.personident),
                            time = nowUTC().minusDays(2),
                        )

                        val veilederBrukerKnytning = VeilederBrukerKnytning(
                            veilederIdent = UserConstants.VEILEDER_ID,
                            fnr = oversikthendelse.personident,
                        )
                        database.lagreVeilederForBruker(
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
                    database.dropData()

                    kafkaDialogmotekandidatEndringService.pollAndProcessRecords(
                        kafkaConsumer = mockKafkaConsumerDialogmotekandidatEndring,
                    )

                    var pPersonOversiktStatusList = database.getPersonOversiktStatusList(fnr = personIdentDefault.value)

                    pPersonOversiktStatusList.size shouldBeEqualTo 1

                    var pPersonOversiktStatus = pPersonOversiktStatusList.first()
                    pPersonOversiktStatus.motebehovUbehandlet.shouldBeNull()
                    pPersonOversiktStatus.oppfolgingsplanLPSBistandUbehandlet.shouldBeNull()
                    pPersonOversiktStatus.enhet.shouldBeNull()
                    pPersonOversiktStatus.dialogmotekandidat shouldBeEqualTo true

                    database.updateTildeltEnhetUpdatedAt(
                        ident = personIdentDefault,
                        time = nowUTC().minusDays(2),
                    )
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

                it("updates Enhet if active aktivitetskrav") {
                    personOversiktStatusRepository.upsertAktivitetskravAktivStatus(
                        personident = personIdentDefault,
                        isAktivVurdering = true,
                    )

                    var pPersonOversiktStatusList = database.getPersonOversiktStatusList(fnr = personIdentDefault.value)

                    pPersonOversiktStatusList.size shouldBeEqualTo 1

                    var pPersonOversiktStatus = pPersonOversiktStatusList.first()
                    pPersonOversiktStatus.enhet.shouldBeNull()
                    pPersonOversiktStatus.tildeltEnhetUpdatedAt.shouldBeNull()
                    pPersonOversiktStatus.isAktivAktivitetskravvurdering.shouldBeTrue()

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
                }

                it("should update Enhet and remove Veileder of existing PersonOversiktStatus with no Enhet if oppfolgingsplanLPSBistandUbehandlet is true") {
                    val oversikthendelse = KPersonoppgavehendelse(
                        personIdentDefault.value,
                        OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_MOTTATT,
                    )
                    val personoversiktStatus = PersonOversiktStatus(
                        fnr = oversikthendelse.personident
                    ).applyHendelse(oversikthendelse.hendelsetype)

                    database.createPersonOversiktStatus(personoversiktStatus)

                    var tildeltEnhetUpdatedAtBeforeUpdate: OffsetDateTime?

                    val veilederBrukerKnytning = VeilederBrukerKnytning(
                        veilederIdent = UserConstants.VEILEDER_ID,
                        fnr = oversikthendelse.personident,
                    )
                    database.lagreVeilederForBruker(
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
                        pPersonOversiktStatus.oppfolgingsplanLPSBistandUbehandlet shouldBeEqualTo true

                        pPersonOversiktStatus.enhet.shouldBeNull()
                        pPersonOversiktStatus.tildeltEnhetUpdatedAt.shouldBeNull()
                        tildeltEnhetUpdatedAtBeforeUpdate = pPersonOversiktStatus.tildeltEnhetUpdatedAt
                        pPersonOversiktStatus.veilederIdent shouldBeEqualTo veilederBrukerKnytning.veilederIdent
                    }

                    database.updateTildeltEnhetUpdatedAt(
                        ident = PersonIdent(recordValue.personIdentNumber),
                        time = nowUTC().minusDays(2),
                    )
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

                    val oversikthendelse = KPersonoppgavehendelse(
                        personIdent.value,
                        OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_MOTTATT,
                    )
                    val personoversiktStatus = PersonOversiktStatus(
                        fnr = oversikthendelse.personident
                    ).applyHendelse(oversikthendelse.hendelsetype)

                    database.createPersonOversiktStatus(personoversiktStatus)

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

                    database.updateTildeltEnhetUpdatedAt(
                        ident = personIdent,
                        time = nowUTC().minusDays(2),
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
                        pPersonOversiktStatus shouldNotBeEqualTo tildeltEnhetUpdatedAtBeforeUpdate
                        pPersonOversiktStatus.veilederIdent.shouldBeNull()

                        tildeltEnhetUpdatedAtBeforeUpdate = pPersonOversiktStatus.tildeltEnhetUpdatedAt
                    }

                    val kafkaOppfolgingstilfellePersonServiceRelevantEnhetNotFound =
                        generateKafkaOppfolgingstilfellePerson(
                            personIdent = personIdent,
                        )
                    val kafkaOppfolgingstilfellePersonServiceRecordRelevantEnhetNotFound =
                        oppfolgingstilfellePersonConsumerRecord(
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

                    database.updateTildeltEnhetUpdatedAt(
                        ident = personIdent,
                        time = nowUTC().minusDays(2),
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

                it("don't update enhet if updated less than 24 hours ago") {
                    val oversikthendelse = KPersonoppgavehendelse(
                        personIdentDefault.value,
                        OversikthendelseType.MOTEBEHOV_SVAR_MOTTATT,
                    )

                    val firstEnhet = NAV_ENHET_2

                    database.dropData()

                    val personoversiktStatus = PersonOversiktStatus(
                        fnr = oversikthendelse.personident
                    ).applyHendelse(oversikthendelse.hendelsetype)

                    database.createPersonOversiktStatus(personoversiktStatus)

                    database.updatePersonTildeltEnhetAndRemoveTildeltVeileder(
                        personIdent = PersonIdent(oversikthendelse.personident),
                        enhetId = firstEnhet,
                    )
                    database.updateTildeltEnhetUpdatedAt(
                        ident = PersonIdent(oversikthendelse.personident),
                        time = nowUTC().minusHours(22),
                    )

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
                    val oversikthendelse = KPersonoppgavehendelse(
                        ARBEIDSTAKER_ENHET_ERROR_PERSONIDENT.value,
                        OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_MOTTATT,
                    )
                    val personoversiktStatus = PersonOversiktStatus(
                        fnr = oversikthendelse.personident
                    ).applyHendelse(oversikthendelse.hendelsetype)

                    database.createPersonOversiktStatus(personoversiktStatus)

                    kafkaOppfolgingstilfellePersonService.pollAndProcessRecords(
                        kafkaConsumer = mockKafkaConsumerOppfolgingstilfellePerson,
                    )

                    database.updateTildeltEnhetUpdatedAt(
                        ident = ARBEIDSTAKER_ENHET_ERROR_PERSONIDENT,
                        time = nowUTC().minusDays(2),
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
