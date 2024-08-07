package no.nav.syfo.oppfolgingstilfelle.kafka

import io.ktor.server.testing.*
import io.ktor.util.*
import io.mockk.clearMocks
import io.mockk.every
import no.nav.syfo.personstatus.domain.PersonIdent
import no.nav.syfo.personstatus.domain.Virksomhetsnummer
import no.nav.syfo.personoppgavehendelse.kafka.KPersonoppgavehendelse
import no.nav.syfo.personstatus.db.*
import no.nav.syfo.personstatus.domain.OversikthendelseType
import no.nav.syfo.personstatus.domain.PersonOversiktStatus
import no.nav.syfo.personstatus.domain.applyHendelse
import no.nav.syfo.testutil.*
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testutil.UserConstants.VIRKSOMHETSNUMMER
import no.nav.syfo.testutil.UserConstants.VIRKSOMHETSNUMMER_2
import no.nav.syfo.testutil.UserConstants.VIRKSOMHETSNUMMER_3
import no.nav.syfo.testutil.assertion.checkPPersonOversiktStatusOppfolgingstilfelle
import no.nav.syfo.testutil.generator.*
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.Duration

@InternalAPI
object KafkaOppfolgingstilfellePersonServiceSpek : Spek({

    with(TestApplicationEngine()) {
        start()

        val externalMockEnvironment = ExternalMockEnvironment.instance
        val database = externalMockEnvironment.database

        application.testApiModule(
            externalMockEnvironment = externalMockEnvironment,
        )

        val kafkaOppfolgingstilfellePersonService = TestKafkaModule.kafkaOppfolgingstilfellePersonService

        val mockKafkaConsumerOppfolgingstilfellePerson = TestKafkaModule.kafkaConsumerOppfolgingstilfellePerson

        val oppfolgingstilfellePersonTopicPartition = oppfolgingstilfellePersonTopicPartition()
        val personIdentDefault = PersonIdent(ARBEIDSTAKER_FNR)

        val kafkaOppfolgingstilfellePersonServiceRelevant = generateKafkaOppfolgingstilfellePerson(
            personIdent = personIdentDefault,
        )
        val kafkaOppfolgingstilfellePersonServiceRecordRelevant = oppfolgingstilfellePersonConsumerRecord(
            kafkaOppfolgingstilfellePerson = kafkaOppfolgingstilfellePersonServiceRelevant
        )
        val kafkaOppfolgingstilfellePersonRelevantNotArbeidstaker = generateKafkaOppfolgingstilfellePerson(
            arbeidstakerAtTilfelleEnd = false,
            personIdent = personIdentDefault,
            virksomhetsnummerList = emptyList(),
        )
        val kafkaOppfolgingstilfellePersonRelevantNotArbeidstakerRecord = oppfolgingstilfellePersonConsumerRecord(
            kafkaOppfolgingstilfellePerson = kafkaOppfolgingstilfellePersonRelevantNotArbeidstaker
        )

        fun mockConsumer(vararg records: ConsumerRecord<String, KafkaOppfolgingstilfellePerson>) {
            every { mockKafkaConsumerOppfolgingstilfellePerson.poll(any<Duration>()) } returns ConsumerRecords(
                mapOf(
                    oppfolgingstilfellePersonTopicPartition to listOf(
                        *records,
                    )
                )
            )
        }

        beforeEachTest {
            database.dropData()

            clearMocks(mockKafkaConsumerOppfolgingstilfellePerson)
            every { mockKafkaConsumerOppfolgingstilfellePerson.commitSync() } returns Unit
        }

        describe("Read KafkaOppfolgingstilfellePerson") {

            it("should create new PersonOversiktStatus if no PersonOversiktStatus exists for PersonIdent") {
                mockConsumer(kafkaOppfolgingstilfellePersonServiceRecordRelevant)

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

                    pPersonOversiktStatus.fnr shouldBeEqualTo recordValue.personIdentNumber
                    pPersonOversiktStatus.enhet.shouldBeNull()
                    pPersonOversiktStatus.veilederIdent.shouldBeNull()

                    pPersonOversiktStatus.motebehovUbehandlet.shouldBeNull()
                    pPersonOversiktStatus.oppfolgingsplanLPSBistandUbehandlet.shouldBeNull()
                    pPersonOversiktStatus.dialogmotekandidat.shouldBeNull()
                    pPersonOversiktStatus.dialogmotekandidatGeneratedAt.shouldBeNull()

                    checkPPersonOversiktStatusOppfolgingstilfelle(
                        pPersonOversiktStatus = pPersonOversiktStatus,
                        kafkaOppfolgingstilfellePerson = recordValue,
                    )
                }
            }

            it("should create new PersonOversiktStatus if no PersonOversiktStatus exists for PersonIdent even if not arbeidstaker") {
                mockConsumer(kafkaOppfolgingstilfellePersonRelevantNotArbeidstakerRecord)

                kafkaOppfolgingstilfellePersonService.pollAndProcessRecords(
                    kafkaConsumer = mockKafkaConsumerOppfolgingstilfellePerson,
                )

                val recordValue = kafkaOppfolgingstilfellePersonRelevantNotArbeidstakerRecord.value()

                database.connection.use { connection ->
                    val pPersonOversiktStatusList = connection.getPersonOversiktStatusList(
                        fnr = recordValue.personIdentNumber,
                    )

                    pPersonOversiktStatusList.size shouldBeEqualTo 1

                    val pPersonOversiktStatus = pPersonOversiktStatusList.first()

                    pPersonOversiktStatus.fnr shouldBeEqualTo recordValue.personIdentNumber
                    pPersonOversiktStatus.enhet.shouldBeNull()
                    pPersonOversiktStatus.veilederIdent.shouldBeNull()

                    pPersonOversiktStatus.motebehovUbehandlet.shouldBeNull()
                    pPersonOversiktStatus.oppfolgingsplanLPSBistandUbehandlet.shouldBeNull()
                    pPersonOversiktStatus.dialogmotekandidat.shouldBeNull()
                    pPersonOversiktStatus.dialogmotekandidatGeneratedAt.shouldBeNull()

                    checkPPersonOversiktStatusOppfolgingstilfelle(
                        pPersonOversiktStatus = pPersonOversiktStatus,
                        kafkaOppfolgingstilfellePerson = recordValue,
                    )
                }
            }

            it("should update existing PersonOversiktStatus with OPPFOLGINGSPLANLPS_BISTAND_MOTTATT, with data from KafkaOppfolgingstilfellePerson") {
                mockConsumer(kafkaOppfolgingstilfellePersonServiceRecordRelevant)

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

                val recordValue = kafkaOppfolgingstilfellePersonServiceRecordRelevant.value()

                val pPersonOversiktStatusList = database.connection.use { connection ->
                    connection.getPersonOversiktStatusList(
                        fnr = recordValue.personIdentNumber,
                    )
                }

                pPersonOversiktStatusList.size shouldBeEqualTo 1

                val pPersonOversiktStatus = pPersonOversiktStatusList.first()

                pPersonOversiktStatus.fnr shouldBeEqualTo recordValue.personIdentNumber
                pPersonOversiktStatus.enhet.shouldBeNull()
                pPersonOversiktStatus.veilederIdent.shouldBeNull()

                pPersonOversiktStatus.motebehovUbehandlet.shouldBeNull()
                pPersonOversiktStatus.oppfolgingsplanLPSBistandUbehandlet shouldBeEqualTo true
                pPersonOversiktStatus.dialogmotekandidat.shouldBeNull()
                pPersonOversiktStatus.dialogmotekandidatGeneratedAt.shouldBeNull()

                checkPPersonOversiktStatusOppfolgingstilfelle(
                    pPersonOversiktStatus = pPersonOversiktStatus,
                    kafkaOppfolgingstilfellePerson = recordValue,
                )
            }

            it("should only update latest OppfolgingstilfellPerson with the newest referanseTilfelleBitInntruffet, if multiple relevant records on same personIdent is received in same poll") {
                val kafkaOppfolgingstilfellePersonServiceRelevantFirst = generateKafkaOppfolgingstilfellePerson(
                    personIdent = personIdentDefault,
                )
                val kafkaOppfolgingstilfellePersonServiceRecordRelevantFirst = oppfolgingstilfellePersonConsumerRecord(
                    kafkaOppfolgingstilfellePerson = kafkaOppfolgingstilfellePersonServiceRelevantFirst,
                )

                val kafkaOppfolgingstilfellePersonServiceRelevantNewest = generateKafkaOppfolgingstilfellePerson(
                    personIdent = PersonIdent(kafkaOppfolgingstilfellePersonServiceRelevantFirst.personIdentNumber),
                ).copy(
                    referanseTilfelleBitInntruffet = kafkaOppfolgingstilfellePersonServiceRelevantFirst.referanseTilfelleBitInntruffet.plusSeconds(
                        1
                    )
                )
                val kafkaOppfolgingstilfellePersonServiceRecordRelevantNewest = oppfolgingstilfellePersonConsumerRecord(
                    kafkaOppfolgingstilfellePerson = kafkaOppfolgingstilfellePersonServiceRelevantNewest,
                )

                mockConsumer(
                    kafkaOppfolgingstilfellePersonServiceRecordRelevantNewest,
                    kafkaOppfolgingstilfellePersonServiceRecordRelevantFirst,
                )

                kafkaOppfolgingstilfellePersonService.pollAndProcessRecords(
                    kafkaConsumer = mockKafkaConsumerOppfolgingstilfellePerson,
                )

                val recordValue = kafkaOppfolgingstilfellePersonServiceRecordRelevantNewest.value()

                database.connection.use { connection ->
                    val pPersonOversiktStatusList = connection.getPersonOversiktStatusList(
                        fnr = recordValue.personIdentNumber,
                    )

                    pPersonOversiktStatusList.size shouldBeEqualTo 1

                    val pPersonOversiktStatus = pPersonOversiktStatusList.first()

                    pPersonOversiktStatus.fnr shouldBeEqualTo recordValue.personIdentNumber
                    pPersonOversiktStatus.enhet.shouldBeNull()
                    pPersonOversiktStatus.veilederIdent.shouldBeNull()

                    pPersonOversiktStatus.motebehovUbehandlet.shouldBeNull()
                    pPersonOversiktStatus.oppfolgingsplanLPSBistandUbehandlet.shouldBeNull()
                    pPersonOversiktStatus.dialogmotekandidat.shouldBeNull()
                    pPersonOversiktStatus.dialogmotekandidatGeneratedAt.shouldBeNull()

                    checkPPersonOversiktStatusOppfolgingstilfelle(
                        pPersonOversiktStatus = pPersonOversiktStatus,
                        kafkaOppfolgingstilfellePerson = recordValue,
                    )

                    val pPersonOppfolgingstilfelleVirksomhetList =
                        connection.getPersonOppfolgingstilfelleVirksomhetList(
                            pPersonOversikStatusId = pPersonOversiktStatus.id,
                        )

                    pPersonOppfolgingstilfelleVirksomhetList.size shouldBeEqualTo recordValue.oppfolgingstilfelleList.first().virksomhetsnummerList.size

                    pPersonOppfolgingstilfelleVirksomhetList.first().virksomhetsnummer.value shouldBeEqualTo recordValue.oppfolgingstilfelleList.first().virksomhetsnummerList.first()
                }
            }

            it("should only update latest OppfolgingstilfellPerson with the newest createdAt, if multiple relevant records on same personIdent with same referanseTilfelleBitInntruffet is received in same poll ") {
                val kafkaOppfolgingstilfellePersonServiceRelevantFirst = generateKafkaOppfolgingstilfellePerson(
                    personIdent = personIdentDefault,
                )
                val kafkaOppfolgingstilfellePersonServiceRecordRelevantFirst = oppfolgingstilfellePersonConsumerRecord(
                    kafkaOppfolgingstilfellePerson = kafkaOppfolgingstilfellePersonServiceRelevantFirst
                )

                val kafkaOppfolgingstilfellePersonServiceRelevantNewest = generateKafkaOppfolgingstilfellePerson(
                    personIdent = PersonIdent(kafkaOppfolgingstilfellePersonServiceRelevantFirst.personIdentNumber),
                ).copy(
                    createdAt = kafkaOppfolgingstilfellePersonServiceRelevantFirst.createdAt.plusSeconds(1),
                    referanseTilfelleBitInntruffet = kafkaOppfolgingstilfellePersonServiceRelevantFirst.referanseTilfelleBitInntruffet
                )
                val kafkaOppfolgingstilfellePersonServiceRecordRelevantNewest = oppfolgingstilfellePersonConsumerRecord(
                    kafkaOppfolgingstilfellePerson = kafkaOppfolgingstilfellePersonServiceRelevantNewest,
                )

                mockConsumer(
                    kafkaOppfolgingstilfellePersonServiceRecordRelevantNewest,
                    kafkaOppfolgingstilfellePersonServiceRecordRelevantFirst,
                )

                kafkaOppfolgingstilfellePersonService.pollAndProcessRecords(
                    kafkaConsumer = mockKafkaConsumerOppfolgingstilfellePerson,
                )

                val recordValue = kafkaOppfolgingstilfellePersonServiceRecordRelevantNewest.value()

                database.connection.use { connection ->
                    val pPersonOversiktStatusList = connection.getPersonOversiktStatusList(
                        fnr = recordValue.personIdentNumber,
                    )

                    pPersonOversiktStatusList.size shouldBeEqualTo 1

                    val pPersonOversiktStatus = pPersonOversiktStatusList.first()

                    pPersonOversiktStatus.fnr shouldBeEqualTo recordValue.personIdentNumber
                    pPersonOversiktStatus.enhet.shouldBeNull()
                    pPersonOversiktStatus.veilederIdent.shouldBeNull()

                    pPersonOversiktStatus.motebehovUbehandlet.shouldBeNull()
                    pPersonOversiktStatus.oppfolgingsplanLPSBistandUbehandlet.shouldBeNull()
                    pPersonOversiktStatus.dialogmotekandidat.shouldBeNull()
                    pPersonOversiktStatus.dialogmotekandidatGeneratedAt.shouldBeNull()

                    checkPPersonOversiktStatusOppfolgingstilfelle(
                        pPersonOversiktStatus = pPersonOversiktStatus,
                        kafkaOppfolgingstilfellePerson = recordValue,
                    )

                    val pPersonOppfolgingstilfelleVirksomhetList =
                        connection.getPersonOppfolgingstilfelleVirksomhetList(
                            pPersonOversikStatusId = pPersonOversiktStatus.id,
                        )

                    pPersonOppfolgingstilfelleVirksomhetList.size shouldBeEqualTo recordValue.oppfolgingstilfelleList.first().virksomhetsnummerList.size

                    pPersonOppfolgingstilfelleVirksomhetList.first().virksomhetsnummer.value shouldBeEqualTo recordValue.oppfolgingstilfelleList.first().virksomhetsnummerList.first()
                }
            }

            it("should update latest OppfolgingstilfellPerson with the newest referanseTilfelleBitInntruffet, if multiple relevant records on same personIdent is received in different polls") {
                val kafkaOppfolgingstilfellePersonServiceRelevantFirst = generateKafkaOppfolgingstilfellePerson(
                    personIdent = personIdentDefault,
                )
                val kafkaOppfolgingstilfellePersonServiceRecordRelevantFirst = oppfolgingstilfellePersonConsumerRecord(
                    kafkaOppfolgingstilfellePerson = kafkaOppfolgingstilfellePersonServiceRelevantFirst,
                )

                val kafkaOppfolgingstilfellePersonServiceRelevantSecond = generateKafkaOppfolgingstilfellePerson(
                    personIdent = PersonIdent(kafkaOppfolgingstilfellePersonServiceRelevantFirst.personIdentNumber),
                ).copy(
                    referanseTilfelleBitInntruffet = kafkaOppfolgingstilfellePersonServiceRelevantFirst.referanseTilfelleBitInntruffet.plusSeconds(
                        1
                    )
                )
                val kafkaOppfolgingstilfellePersonServiceRecordRelevantSecond = oppfolgingstilfellePersonConsumerRecord(
                    kafkaOppfolgingstilfellePerson = kafkaOppfolgingstilfellePersonServiceRelevantSecond,
                )

                val kafkaOppfolgingstilfellePersonServiceRelevantNewest = generateKafkaOppfolgingstilfellePerson(
                    personIdent = PersonIdent(kafkaOppfolgingstilfellePersonServiceRelevantFirst.personIdentNumber),
                ).copy(
                    referanseTilfelleBitInntruffet = kafkaOppfolgingstilfellePersonServiceRelevantSecond.referanseTilfelleBitInntruffet.plusSeconds(
                        1
                    )
                )
                val kafkaOppfolgingstilfellePersonServiceRecordRelevantNewest = oppfolgingstilfellePersonConsumerRecord(
                    kafkaOppfolgingstilfellePerson = kafkaOppfolgingstilfellePersonServiceRelevantNewest,
                )

                mockConsumer(kafkaOppfolgingstilfellePersonServiceRecordRelevantSecond)

                kafkaOppfolgingstilfellePersonService.pollAndProcessRecords(
                    kafkaConsumer = mockKafkaConsumerOppfolgingstilfellePerson,
                )

                mockConsumer(kafkaOppfolgingstilfellePersonServiceRecordRelevantFirst)

                kafkaOppfolgingstilfellePersonService.pollAndProcessRecords(
                    kafkaConsumer = mockKafkaConsumerOppfolgingstilfellePerson,
                )

                val recordValueSecond = kafkaOppfolgingstilfellePersonServiceRecordRelevantSecond.value()

                database.connection.use { connection ->
                    val pPersonOversiktStatusList = connection.getPersonOversiktStatusList(
                        fnr = recordValueSecond.personIdentNumber,
                    )

                    pPersonOversiktStatusList.size shouldBeEqualTo 1

                    val pPersonOversiktStatus = pPersonOversiktStatusList.first()

                    pPersonOversiktStatus.fnr shouldBeEqualTo recordValueSecond.personIdentNumber
                    pPersonOversiktStatus.enhet.shouldBeNull()
                    pPersonOversiktStatus.veilederIdent.shouldBeNull()

                    pPersonOversiktStatus.motebehovUbehandlet.shouldBeNull()
                    pPersonOversiktStatus.oppfolgingsplanLPSBistandUbehandlet.shouldBeNull()
                    pPersonOversiktStatus.dialogmotekandidat.shouldBeNull()
                    pPersonOversiktStatus.dialogmotekandidatGeneratedAt.shouldBeNull()

                    checkPPersonOversiktStatusOppfolgingstilfelle(
                        pPersonOversiktStatus = pPersonOversiktStatus,
                        kafkaOppfolgingstilfellePerson = recordValueSecond,
                    )

                    val pPersonOppfolgingstilfelleVirksomhetList =
                        connection.getPersonOppfolgingstilfelleVirksomhetList(
                            pPersonOversikStatusId = pPersonOversiktStatus.id,
                        )

                    pPersonOppfolgingstilfelleVirksomhetList.size shouldBeEqualTo recordValueSecond.oppfolgingstilfelleList.first().virksomhetsnummerList.size

                    pPersonOppfolgingstilfelleVirksomhetList.first().virksomhetsnummer.value shouldBeEqualTo recordValueSecond.oppfolgingstilfelleList.first().virksomhetsnummerList.first()
                }

                mockConsumer(kafkaOppfolgingstilfellePersonServiceRecordRelevantNewest)

                kafkaOppfolgingstilfellePersonService.pollAndProcessRecords(
                    kafkaConsumer = mockKafkaConsumerOppfolgingstilfellePerson,
                )

                val recordValueNewest = kafkaOppfolgingstilfellePersonServiceRecordRelevantNewest.value()

                database.connection.use { connection ->
                    val pPersonOversiktStatusList = connection.getPersonOversiktStatusList(
                        fnr = recordValueNewest.personIdentNumber,
                    )

                    pPersonOversiktStatusList.size shouldBeEqualTo 1

                    val pPersonOversiktStatus = pPersonOversiktStatusList.first()

                    pPersonOversiktStatus.fnr shouldBeEqualTo recordValueNewest.personIdentNumber
                    pPersonOversiktStatus.enhet.shouldBeNull()
                    pPersonOversiktStatus.veilederIdent.shouldBeNull()

                    pPersonOversiktStatus.motebehovUbehandlet.shouldBeNull()
                    pPersonOversiktStatus.oppfolgingsplanLPSBistandUbehandlet.shouldBeNull()
                    pPersonOversiktStatus.dialogmotekandidat.shouldBeNull()
                    pPersonOversiktStatus.dialogmotekandidatGeneratedAt.shouldBeNull()

                    checkPPersonOversiktStatusOppfolgingstilfelle(
                        pPersonOversiktStatus = pPersonOversiktStatus,
                        kafkaOppfolgingstilfellePerson = recordValueNewest,
                    )

                    val pPersonOppfolgingstilfelleVirksomhetList =
                        connection.getPersonOppfolgingstilfelleVirksomhetList(
                            pPersonOversikStatusId = pPersonOversiktStatus.id,
                        )

                    pPersonOppfolgingstilfelleVirksomhetList.size shouldBeEqualTo recordValueNewest.oppfolgingstilfelleList.first().virksomhetsnummerList.size

                    pPersonOppfolgingstilfelleVirksomhetList.first().virksomhetsnummer.value shouldBeEqualTo recordValueNewest.oppfolgingstilfelleList.first().virksomhetsnummerList.first()
                }
            }

            it("should update latest OppfolgingstilfellPerson with the newest referanseTilfelleBitInntruffet and createdAt, if multiple relevant records on same personIdent with same inntruffet is received in different polls") {
                val kafkaOppfolgingstilfellePersonServiceRelevantFirst = generateKafkaOppfolgingstilfellePerson(
                    personIdent = personIdentDefault,
                )
                val kafkaOppfolgingstilfellePersonServiceRecordRelevantFirst = oppfolgingstilfellePersonConsumerRecord(
                    kafkaOppfolgingstilfellePerson = kafkaOppfolgingstilfellePersonServiceRelevantFirst,
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
                    kafkaOppfolgingstilfellePerson = kafkaOppfolgingstilfellePersonServiceRelevantSecond,
                )
                mockConsumer(kafkaOppfolgingstilfellePersonServiceRecordRelevantFirst)

                kafkaOppfolgingstilfellePersonService.pollAndProcessRecords(
                    kafkaConsumer = mockKafkaConsumerOppfolgingstilfellePerson,
                )

                mockConsumer(kafkaOppfolgingstilfellePersonServiceRecordRelevantSecond)

                kafkaOppfolgingstilfellePersonService.pollAndProcessRecords(
                    kafkaConsumer = mockKafkaConsumerOppfolgingstilfellePerson,
                )
                val recordValueSecond = kafkaOppfolgingstilfellePersonServiceRecordRelevantSecond.value()

                database.connection.use { connection ->
                    val pPersonOversiktStatusList = connection.getPersonOversiktStatusList(
                        fnr = recordValueSecond.personIdentNumber,
                    )

                    pPersonOversiktStatusList.size shouldBeEqualTo 1

                    val pPersonOversiktStatus = pPersonOversiktStatusList.first()

                    pPersonOversiktStatus.fnr shouldBeEqualTo recordValueSecond.personIdentNumber
                    pPersonOversiktStatus.enhet.shouldBeNull()
                    pPersonOversiktStatus.veilederIdent.shouldBeNull()

                    pPersonOversiktStatus.motebehovUbehandlet.shouldBeNull()
                    pPersonOversiktStatus.oppfolgingsplanLPSBistandUbehandlet.shouldBeNull()
                    pPersonOversiktStatus.dialogmotekandidat.shouldBeNull()
                    pPersonOversiktStatus.dialogmotekandidatGeneratedAt.shouldBeNull()

                    checkPPersonOversiktStatusOppfolgingstilfelle(
                        pPersonOversiktStatus = pPersonOversiktStatus,
                        kafkaOppfolgingstilfellePerson = recordValueSecond,
                    )

                    val pPersonOppfolgingstilfelleVirksomhetList =
                        connection.getPersonOppfolgingstilfelleVirksomhetList(
                            pPersonOversikStatusId = pPersonOversiktStatus.id,
                        )

                    pPersonOppfolgingstilfelleVirksomhetList.size shouldBeEqualTo recordValueSecond.oppfolgingstilfelleList.first().virksomhetsnummerList.size

                    pPersonOppfolgingstilfelleVirksomhetList.first().virksomhetsnummer.value shouldBeEqualTo recordValueSecond.oppfolgingstilfelleList.first().virksomhetsnummerList.first()
                }
            }

            it("should only update latest OppfolgingstilfellPerson with the newest createdAt, if multiple relevant records on same personIdent with same referanseTilfelleBitUuid is received in different polls ") {
                val kafkaOppfolgingstilfellePersonServiceRelevantFirst = generateKafkaOppfolgingstilfellePerson(
                    personIdent = personIdentDefault,
                )
                val kafkaOppfolgingstilfellePersonServiceRecordRelevantFirst = oppfolgingstilfellePersonConsumerRecord(
                    kafkaOppfolgingstilfellePerson = kafkaOppfolgingstilfellePersonServiceRelevantFirst,
                )

                val kafkaOppfolgingstilfellePersonServiceRelevantSecond = generateKafkaOppfolgingstilfellePerson(
                    personIdent = PersonIdent(kafkaOppfolgingstilfellePersonServiceRelevantFirst.personIdentNumber),
                ).copy(
                    createdAt = kafkaOppfolgingstilfellePersonServiceRelevantFirst.createdAt.plusSeconds(1),
                    referanseTilfelleBitInntruffet = kafkaOppfolgingstilfellePersonServiceRelevantFirst.referanseTilfelleBitInntruffet,
                    referanseTilfelleBitUuid = kafkaOppfolgingstilfellePersonServiceRelevantFirst.referanseTilfelleBitUuid,
                )
                val kafkaOppfolgingstilfellePersonServiceRecordRelevantSecond = oppfolgingstilfellePersonConsumerRecord(
                    kafkaOppfolgingstilfellePerson = kafkaOppfolgingstilfellePersonServiceRelevantSecond,
                )

                val kafkaOppfolgingstilfellePersonServiceRelevantNewest = generateKafkaOppfolgingstilfellePerson(
                    personIdent = PersonIdent(kafkaOppfolgingstilfellePersonServiceRelevantFirst.personIdentNumber),
                ).copy(
                    createdAt = kafkaOppfolgingstilfellePersonServiceRelevantSecond.createdAt.plusSeconds(1),
                    referanseTilfelleBitInntruffet = kafkaOppfolgingstilfellePersonServiceRelevantFirst.referanseTilfelleBitInntruffet,
                    referanseTilfelleBitUuid = kafkaOppfolgingstilfellePersonServiceRelevantFirst.referanseTilfelleBitUuid,
                )
                val kafkaOppfolgingstilfellePersonServiceRecordRelevantNewest = oppfolgingstilfellePersonConsumerRecord(
                    kafkaOppfolgingstilfellePerson = kafkaOppfolgingstilfellePersonServiceRelevantNewest,
                )

                mockConsumer(kafkaOppfolgingstilfellePersonServiceRecordRelevantSecond)

                kafkaOppfolgingstilfellePersonService.pollAndProcessRecords(
                    kafkaConsumer = mockKafkaConsumerOppfolgingstilfellePerson,
                )

                mockConsumer(kafkaOppfolgingstilfellePersonServiceRecordRelevantFirst)

                kafkaOppfolgingstilfellePersonService.pollAndProcessRecords(
                    kafkaConsumer = mockKafkaConsumerOppfolgingstilfellePerson,
                )

                val recordValueSecond = kafkaOppfolgingstilfellePersonServiceRecordRelevantSecond.value()

                database.connection.use { connection ->
                    val pPersonOversiktStatusList = connection.getPersonOversiktStatusList(
                        fnr = recordValueSecond.personIdentNumber,
                    )

                    pPersonOversiktStatusList.size shouldBeEqualTo 1

                    val pPersonOversiktStatus = pPersonOversiktStatusList.first()

                    pPersonOversiktStatus.fnr shouldBeEqualTo recordValueSecond.personIdentNumber
                    pPersonOversiktStatus.enhet.shouldBeNull()
                    pPersonOversiktStatus.veilederIdent.shouldBeNull()

                    pPersonOversiktStatus.motebehovUbehandlet.shouldBeNull()
                    pPersonOversiktStatus.oppfolgingsplanLPSBistandUbehandlet.shouldBeNull()
                    pPersonOversiktStatus.dialogmotekandidat.shouldBeNull()
                    pPersonOversiktStatus.dialogmotekandidatGeneratedAt.shouldBeNull()

                    checkPPersonOversiktStatusOppfolgingstilfelle(
                        pPersonOversiktStatus = pPersonOversiktStatus,
                        kafkaOppfolgingstilfellePerson = recordValueSecond,
                    )

                    val pPersonOppfolgingstilfelleVirksomhetList =
                        connection.getPersonOppfolgingstilfelleVirksomhetList(
                            pPersonOversikStatusId = pPersonOversiktStatus.id,
                        )

                    pPersonOppfolgingstilfelleVirksomhetList.size shouldBeEqualTo recordValueSecond.oppfolgingstilfelleList.first().virksomhetsnummerList.size

                    pPersonOppfolgingstilfelleVirksomhetList.first().virksomhetsnummer.value shouldBeEqualTo recordValueSecond.oppfolgingstilfelleList.first().virksomhetsnummerList.first()
                }

                mockConsumer(kafkaOppfolgingstilfellePersonServiceRecordRelevantNewest)

                kafkaOppfolgingstilfellePersonService.pollAndProcessRecords(
                    kafkaConsumer = mockKafkaConsumerOppfolgingstilfellePerson,
                )

                val recordValueNewest = kafkaOppfolgingstilfellePersonServiceRecordRelevantNewest.value()

                database.connection.use { connection ->
                    val pPersonOversiktStatusList = connection.getPersonOversiktStatusList(
                        fnr = recordValueNewest.personIdentNumber,
                    )

                    pPersonOversiktStatusList.size shouldBeEqualTo 1

                    val pPersonOversiktStatus = pPersonOversiktStatusList.first()

                    pPersonOversiktStatus.fnr shouldBeEqualTo recordValueNewest.personIdentNumber
                    pPersonOversiktStatus.enhet.shouldBeNull()
                    pPersonOversiktStatus.veilederIdent.shouldBeNull()

                    pPersonOversiktStatus.motebehovUbehandlet.shouldBeNull()
                    pPersonOversiktStatus.oppfolgingsplanLPSBistandUbehandlet.shouldBeNull()
                    pPersonOversiktStatus.dialogmotekandidat.shouldBeNull()
                    pPersonOversiktStatus.dialogmotekandidatGeneratedAt.shouldBeNull()

                    checkPPersonOversiktStatusOppfolgingstilfelle(
                        pPersonOversiktStatus = pPersonOversiktStatus,
                        kafkaOppfolgingstilfellePerson = recordValueNewest,
                    )

                    val pPersonOppfolgingstilfelleVirksomhetList =
                        connection.getPersonOppfolgingstilfelleVirksomhetList(
                            pPersonOversikStatusId = pPersonOversiktStatus.id,
                        )

                    pPersonOppfolgingstilfelleVirksomhetList.size shouldBeEqualTo recordValueNewest.oppfolgingstilfelleList.first().virksomhetsnummerList.size

                    pPersonOppfolgingstilfelleVirksomhetList.first().virksomhetsnummer.value shouldBeEqualTo recordValueNewest.oppfolgingstilfelleList.first().virksomhetsnummerList.first()
                }
            }

            it("should first receive and create Virksomhet1 and Virksomhet2, then delete Virksomhet1, keep Viksomhet2 and create Virskomhet3") {
                val virksomhetsnummerListFirst = listOf(
                    Virksomhetsnummer(VIRKSOMHETSNUMMER),
                    Virksomhetsnummer(VIRKSOMHETSNUMMER_2),
                )
                val kafkaOppfolgingstilfellePersonServiceRelevantFirst = generateKafkaOppfolgingstilfellePerson(
                    personIdent = personIdentDefault,
                    virksomhetsnummerList = virksomhetsnummerListFirst
                )
                val kafkaOppfolgingstilfellePersonServiceRecordRelevantFirst = oppfolgingstilfellePersonConsumerRecord(
                    kafkaOppfolgingstilfellePerson = kafkaOppfolgingstilfellePersonServiceRelevantFirst,
                )

                mockConsumer(kafkaOppfolgingstilfellePersonServiceRecordRelevantFirst)

                kafkaOppfolgingstilfellePersonService.pollAndProcessRecords(
                    kafkaConsumer = mockKafkaConsumerOppfolgingstilfellePerson,
                )

                val virksomhetsnummerListSecond = listOf(
                    Virksomhetsnummer(VIRKSOMHETSNUMMER_2),
                    Virksomhetsnummer(VIRKSOMHETSNUMMER_3),
                )
                val kafkaOppfolgingstilfellePersonServiceRelevantSecond = generateKafkaOppfolgingstilfellePerson(
                    personIdent = personIdentDefault,
                    virksomhetsnummerList = virksomhetsnummerListSecond,
                )
                val kafkaOppfolgingstilfellePersonServiceRecordRelevantSecond = oppfolgingstilfellePersonConsumerRecord(
                    kafkaOppfolgingstilfellePerson = kafkaOppfolgingstilfellePersonServiceRelevantSecond,
                )

                mockConsumer(kafkaOppfolgingstilfellePersonServiceRecordRelevantSecond)

                kafkaOppfolgingstilfellePersonService.pollAndProcessRecords(
                    kafkaConsumer = mockKafkaConsumerOppfolgingstilfellePerson,
                )

                val recordValue = kafkaOppfolgingstilfellePersonServiceRecordRelevantSecond.value()

                database.connection.use { connection ->
                    val pPersonOversiktStatusList = connection.getPersonOversiktStatusList(
                        fnr = recordValue.personIdentNumber,
                    )

                    pPersonOversiktStatusList.size shouldBeEqualTo 1

                    val pPersonOversiktStatus = pPersonOversiktStatusList.first()

                    pPersonOversiktStatus.fnr shouldBeEqualTo recordValue.personIdentNumber
                    pPersonOversiktStatus.enhet.shouldBeNull()
                    pPersonOversiktStatus.veilederIdent.shouldBeNull()

                    pPersonOversiktStatus.motebehovUbehandlet.shouldBeNull()
                    pPersonOversiktStatus.oppfolgingsplanLPSBistandUbehandlet.shouldBeNull()
                    pPersonOversiktStatus.dialogmotekandidat.shouldBeNull()
                    pPersonOversiktStatus.dialogmotekandidatGeneratedAt.shouldBeNull()

                    checkPPersonOversiktStatusOppfolgingstilfelle(
                        pPersonOversiktStatus = pPersonOversiktStatus,
                        kafkaOppfolgingstilfellePerson = recordValue,
                    )

                    val pPersonOppfolgingstilfelleVirksomhetList =
                        connection.getPersonOppfolgingstilfelleVirksomhetList(
                            pPersonOversikStatusId = pPersonOversiktStatus.id,
                        )

                    pPersonOppfolgingstilfelleVirksomhetList.size shouldBeEqualTo 2

                    pPersonOppfolgingstilfelleVirksomhetList.first().virksomhetsnummer.value shouldBeEqualTo virksomhetsnummerListSecond.first().value
                    pPersonOppfolgingstilfelleVirksomhetList.last().virksomhetsnummer.value shouldBeEqualTo virksomhetsnummerListSecond.last().value
                }
            }

            it("creates new PersonOversiktStatus and handles antallSykedager = null") {
                val kafkaOppfolgingstilfellePerson = generateKafkaOppfolgingstilfellePerson(
                    personIdent = personIdentDefault,
                    antallSykedager = null,
                )
                val kafkaOppfolgingstilfellePersonRecord = oppfolgingstilfellePersonConsumerRecord(
                    kafkaOppfolgingstilfellePerson = kafkaOppfolgingstilfellePerson
                )
                mockConsumer(kafkaOppfolgingstilfellePersonRecord)

                kafkaOppfolgingstilfellePersonService.pollAndProcessRecords(
                    kafkaConsumer = mockKafkaConsumerOppfolgingstilfellePerson,
                )

                val recordValue = kafkaOppfolgingstilfellePersonRecord.value()

                database.connection.use { connection ->
                    val pPersonOversiktStatusList = connection.getPersonOversiktStatusList(
                        fnr = recordValue.personIdentNumber,
                    )

                    pPersonOversiktStatusList.size shouldBeEqualTo 1

                    val pPersonOversiktStatus = pPersonOversiktStatusList.first()

                    pPersonOversiktStatus.fnr shouldBeEqualTo recordValue.personIdentNumber
                    pPersonOversiktStatus.enhet.shouldBeNull()
                    pPersonOversiktStatus.veilederIdent.shouldBeNull()

                    pPersonOversiktStatus.motebehovUbehandlet.shouldBeNull()
                    pPersonOversiktStatus.oppfolgingsplanLPSBistandUbehandlet.shouldBeNull()
                    pPersonOversiktStatus.dialogmotekandidat.shouldBeNull()
                    pPersonOversiktStatus.dialogmotekandidatGeneratedAt.shouldBeNull()

                    checkPPersonOversiktStatusOppfolgingstilfelle(
                        pPersonOversiktStatus = pPersonOversiktStatus,
                        kafkaOppfolgingstilfellePerson = recordValue,
                    )
                }
            }
        }
    }
})
