package no.nav.syfo.oppfolgingstilfelle.kafka

import io.mockk.clearMocks
import io.mockk.every
import kotlinx.coroutines.runBlocking
import no.nav.syfo.personstatus.domain.PersonIdent
import no.nav.syfo.personstatus.domain.Virksomhetsnummer
import no.nav.syfo.personoppgavehendelse.kafka.KPersonoppgavehendelse
import no.nav.syfo.personstatus.domain.OversikthendelseType
import no.nav.syfo.personstatus.domain.PersonOversiktStatus
import no.nav.syfo.personstatus.domain.applyHendelse
import no.nav.syfo.personstatus.infrastructure.database.queries.getPersonOppfolgingstilfelleVirksomhetList
import no.nav.syfo.personstatus.infrastructure.database.queries.getPersonOversiktStatusList
import no.nav.syfo.testutil.*
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testutil.UserConstants.VIRKSOMHETSNUMMER
import no.nav.syfo.testutil.UserConstants.VIRKSOMHETSNUMMER_2
import no.nav.syfo.testutil.UserConstants.VIRKSOMHETSNUMMER_3
import no.nav.syfo.testutil.assertion.checkPPersonOversiktStatusOppfolgingstilfelle
import no.nav.syfo.testutil.generator.*
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBe
import org.amshove.kluent.shouldNotBeNull
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.Duration

object OppfolgingstilfelleConsumerSpek : Spek({
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

    beforeEachTest {
        database.dropData()

        clearMocks(mockKafkaConsumerOppfolgingstilfellePerson)
        every { mockKafkaConsumerOppfolgingstilfellePerson.commitSync() } returns Unit
    }

    describe("Read KafkaOppfolgingstilfellePerson") {

        it("should create new PersonOversiktStatus if no PersonOversiktStatus exists for PersonIdent") {
            mockConsumer(oppfolgingstilfellePersonConsumerRecordRelevant)

            runBlocking {
                oppfolgingstilfelleConsumer.pollAndProcessRecords(
                    kafkaConsumer = mockKafkaConsumerOppfolgingstilfellePerson,
                )
            }

            val recordValue = oppfolgingstilfellePersonConsumerRecordRelevant.value()

            val pPersonStatus = personOversiktStatusRepository.getPersonOversiktStatus(PersonIdent(recordValue.personIdentNumber))

            pPersonStatus?.fnr shouldBeEqualTo recordValue.personIdentNumber
            pPersonStatus?.navn shouldNotBe null
            pPersonStatus?.fodselsdato shouldNotBe null
        }

        it("should create new PersonOversiktStatus with navn and fodselsdato from PDL") {
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
                    oppfolgingstilfellePersonRecord = recordValue,
                )
            }
        }

        it("should create new PersonOversiktStatus if no PersonOversiktStatus exists for PersonIdent even if not arbeidstaker") {
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
                    oppfolgingstilfellePersonRecord = recordValue,
                )
            }
        }

        it("should update existing PersonOversiktStatus with OPPFOLGINGSPLANLPS_BISTAND_MOTTATT, with data from KafkaOppfolgingstilfellePerson") {
            mockConsumer(oppfolgingstilfellePersonConsumerRecordRelevant)

            val oversiktHendelseOPLPSBistandMottatt = KPersonoppgavehendelse(
                personIdentDefault.value,
                OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_MOTTATT,
            )
            val personoversiktStatus = PersonOversiktStatus(
                fnr = oversiktHendelseOPLPSBistandMottatt.personident
            ).applyHendelse(oversiktHendelseOPLPSBistandMottatt.hendelsetype)

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
                oppfolgingstilfellePersonRecord = recordValue,
            )
        }

        it("should only update latest OppfolgingstilfellPerson with the newest referanseTilfelleBitInntruffet, if multiple relevant records on same personIdent is received in same poll") {
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
                    oppfolgingstilfellePersonRecord = recordValue,
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
                    oppfolgingstilfellePersonRecord = recordValue,
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
                    oppfolgingstilfellePersonRecord = recordValueSecond,
                )

                val pPersonOppfolgingstilfelleVirksomhetList =
                    connection.getPersonOppfolgingstilfelleVirksomhetList(
                        pPersonOversikStatusId = pPersonOversiktStatus.id,
                    )

                pPersonOppfolgingstilfelleVirksomhetList.size shouldBeEqualTo recordValueSecond.oppfolgingstilfelleList.first().virksomhetsnummerList.size

                pPersonOppfolgingstilfelleVirksomhetList.first().virksomhetsnummer.value shouldBeEqualTo recordValueSecond.oppfolgingstilfelleList.first().virksomhetsnummerList.first()
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
                    oppfolgingstilfellePersonRecord = recordValueNewest,
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
                    oppfolgingstilfellePersonRecord = recordValueSecond,
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
                    oppfolgingstilfellePersonRecord = recordValueSecond,
                )

                val pPersonOppfolgingstilfelleVirksomhetList =
                    connection.getPersonOppfolgingstilfelleVirksomhetList(
                        pPersonOversikStatusId = pPersonOversiktStatus.id,
                    )

                pPersonOppfolgingstilfelleVirksomhetList.size shouldBeEqualTo recordValueSecond.oppfolgingstilfelleList.first().virksomhetsnummerList.size

                pPersonOppfolgingstilfelleVirksomhetList.first().virksomhetsnummer.value shouldBeEqualTo recordValueSecond.oppfolgingstilfelleList.first().virksomhetsnummerList.first()
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
                    oppfolgingstilfellePersonRecord = recordValueNewest,
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
                    oppfolgingstilfellePersonRecord = recordValue,
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
                    oppfolgingstilfellePersonRecord = recordValue,
                )
            }
        }

        it("updates oppfolgingstilfelle and removes tildelt veileder from existing PersonOversiktStatus when tildelt veileder not found") {
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

            personOversiktStatus.latestOppfolgingstilfelle.shouldNotBeNull()
            personOversiktStatus.veilederIdent.shouldBeNull()
        }

        it("updates oppfolgingstilfelle and removes tildelt veileder from existing PersonOversiktStatus when tildelt veileder not enabled") {
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

            personOversiktStatus.latestOppfolgingstilfelle.shouldNotBeNull()
            personOversiktStatus.veilederIdent.shouldBeNull()
        }

        it("updates oppfolgingstilfelle and does not remove tildelt veileder from existing PersonOversiktStatus when tildelt veileder found and enabled") {
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

            personOversiktStatus.latestOppfolgingstilfelle.shouldNotBeNull()
            personOversiktStatus.veilederIdent shouldBeEqualTo UserConstants.VEILEDER_ID
        }

        it("updates oppfolgingstilfelle and does not remove tildelt veileder from existing PersonOversiktStatus when request to get tildelt veileder fails") {
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

            personOversiktStatus.latestOppfolgingstilfelle.shouldNotBeNull()
            personOversiktStatus.veilederIdent shouldBeEqualTo UserConstants.VEILEDER_ID_WITH_ERROR
        }
    }
})
