package no.nav.syfo.personstatus

import io.ktor.server.testing.*
import io.ktor.util.*
import io.mockk.clearMocks
import io.mockk.every
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.domain.Virksomhetsnummer
import no.nav.syfo.oppfolgingstilfelle.kafka.OPPFOLGINGSTILFELLE_PERSON_TOPIC
import no.nav.syfo.personstatus.domain.OversikthendelseType
import no.nav.syfo.testutil.*
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testutil.UserConstants.VIRKSOMHETSNUMMER
import no.nav.syfo.testutil.UserConstants.VIRKSOMHETSNUMMER_2
import no.nav.syfo.testutil.UserConstants.VIRKSOMHETSNUMMER_3
import no.nav.syfo.testutil.assertion.checkPPersonOversiktStatusOppfolgingstilfelle
import no.nav.syfo.testutil.generator.generateKOversikthendelse
import no.nav.syfo.testutil.generator.generateKafkaOppfolgingstilfellePerson
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.common.TopicPartition
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

        val internalMockEnvironment = InternalMockEnvironment.instance

        val oversiktHendelseService = internalMockEnvironment.oversiktHendelseService
        val kafkaOppfolgingstilfellePersonService = internalMockEnvironment.kafkaOppfolgingstilfellePersonService

        val mockKafkaConsumerOppfolgingstilfellePerson = internalMockEnvironment.kafkaConsumerOppfolgingstilfellePerson

        val partition = 0
        val oppfolgingstilfellePersonTopicPartition = TopicPartition(
            OPPFOLGINGSTILFELLE_PERSON_TOPIC,
            partition,
        )
        val personIdentDefault = PersonIdent(ARBEIDSTAKER_FNR)

        val kafkaOppfolgingstilfellePersonServiceRelevant = generateKafkaOppfolgingstilfellePerson(
            personIdent = personIdentDefault,
        )
        val kafkaOppfolgingstilfellePersonServiceRecordRelevant = ConsumerRecord(
            OPPFOLGINGSTILFELLE_PERSON_TOPIC,
            partition,
            1,
            "key1",
            kafkaOppfolgingstilfellePersonServiceRelevant,
        )

        beforeEachTest {
            database.connection.dropData()

            clearMocks(mockKafkaConsumerOppfolgingstilfellePerson)
            every { mockKafkaConsumerOppfolgingstilfellePerson.commitSync() } returns Unit
        }

        describe("Read KafkaOppfolgingstilfellePerson") {

            it("should create new PersonOversiktStatus if no PersonOversiktStatus exists for PersonIdent") {
                every { mockKafkaConsumerOppfolgingstilfellePerson.poll(any<Duration>()) } returns ConsumerRecords(
                    mapOf(
                        oppfolgingstilfellePersonTopicPartition to listOf(
                            kafkaOppfolgingstilfellePersonServiceRecordRelevant,
                        )
                    )
                )

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

                    pPersonOversiktStatus.fnr shouldBeEqualTo recordValue.personIdentNumber
                    pPersonOversiktStatus.enhet.shouldBeNull()
                    pPersonOversiktStatus.veilederIdent.shouldBeNull()

                    pPersonOversiktStatus.motebehovUbehandlet.shouldBeNull()
                    pPersonOversiktStatus.moteplanleggerUbehandlet.shouldBeNull()
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
                every { mockKafkaConsumerOppfolgingstilfellePerson.poll(any<Duration>()) } returns ConsumerRecords(
                    mapOf(
                        oppfolgingstilfellePersonTopicPartition to listOf(
                            kafkaOppfolgingstilfellePersonServiceRecordRelevant,
                        )
                    )
                )

                val oversiktHendelseOPLPSBistandMottatt = generateKOversikthendelse(
                    oversikthendelseType = OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_MOTTATT,
                    personIdent = personIdentDefault.value,
                )
                oversiktHendelseService.oppdaterPersonMedHendelse(
                    oversiktHendelse = oversiktHendelseOPLPSBistandMottatt,
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

                pPersonOversiktStatus.fnr shouldBeEqualTo recordValue.personIdentNumber
                pPersonOversiktStatus.enhet.shouldBeNull()
                pPersonOversiktStatus.veilederIdent.shouldBeNull()

                pPersonOversiktStatus.motebehovUbehandlet.shouldBeNull()
                pPersonOversiktStatus.moteplanleggerUbehandlet.shouldBeNull()
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
                val kafkaOppfolgingstilfellePersonServiceRecordRelevantFirst = ConsumerRecord(
                    OPPFOLGINGSTILFELLE_PERSON_TOPIC,
                    partition,
                    1,
                    "key1",
                    kafkaOppfolgingstilfellePersonServiceRelevantFirst,
                )

                val kafkaOppfolgingstilfellePersonServiceRelevantNewest = generateKafkaOppfolgingstilfellePerson(
                    personIdent = PersonIdent(kafkaOppfolgingstilfellePersonServiceRelevantFirst.personIdentNumber),
                ).copy(
                    referanseTilfelleBitInntruffet = kafkaOppfolgingstilfellePersonServiceRelevantFirst.referanseTilfelleBitInntruffet.plusSeconds(
                        1
                    )
                )
                val kafkaOppfolgingstilfellePersonServiceRecordRelevantNewest = ConsumerRecord(
                    OPPFOLGINGSTILFELLE_PERSON_TOPIC,
                    partition,
                    2,
                    "key2",
                    kafkaOppfolgingstilfellePersonServiceRelevantNewest,
                )

                every { mockKafkaConsumerOppfolgingstilfellePerson.poll(any<Duration>()) } returns ConsumerRecords(
                    mapOf(
                        oppfolgingstilfellePersonTopicPartition to listOf(
                            kafkaOppfolgingstilfellePersonServiceRecordRelevantNewest,
                            kafkaOppfolgingstilfellePersonServiceRecordRelevantFirst,
                        )
                    )
                )
                kafkaOppfolgingstilfellePersonService.pollAndProcessRecords(
                    kafkaConsumerOppfolgingstilfellePerson = mockKafkaConsumerOppfolgingstilfellePerson,
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
                    pPersonOversiktStatus.moteplanleggerUbehandlet.shouldBeNull()
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
                val kafkaOppfolgingstilfellePersonServiceRecordRelevantFirst = ConsumerRecord(
                    OPPFOLGINGSTILFELLE_PERSON_TOPIC,
                    partition,
                    1,
                    "key1",
                    kafkaOppfolgingstilfellePersonServiceRelevantFirst,
                )

                val kafkaOppfolgingstilfellePersonServiceRelevantNewest = generateKafkaOppfolgingstilfellePerson(
                    personIdent = PersonIdent(kafkaOppfolgingstilfellePersonServiceRelevantFirst.personIdentNumber),
                ).copy(
                    createdAt = kafkaOppfolgingstilfellePersonServiceRelevantFirst.createdAt.plusSeconds(1),
                    referanseTilfelleBitInntruffet = kafkaOppfolgingstilfellePersonServiceRelevantFirst.referanseTilfelleBitInntruffet
                )
                val kafkaOppfolgingstilfellePersonServiceRecordRelevantNewest = ConsumerRecord(
                    OPPFOLGINGSTILFELLE_PERSON_TOPIC,
                    partition,
                    2,
                    "key2",
                    kafkaOppfolgingstilfellePersonServiceRelevantNewest,
                )

                every { mockKafkaConsumerOppfolgingstilfellePerson.poll(any<Duration>()) } returns ConsumerRecords(
                    mapOf(
                        oppfolgingstilfellePersonTopicPartition to listOf(
                            kafkaOppfolgingstilfellePersonServiceRecordRelevantNewest,
                            kafkaOppfolgingstilfellePersonServiceRecordRelevantFirst,
                        )
                    )
                )
                kafkaOppfolgingstilfellePersonService.pollAndProcessRecords(
                    kafkaConsumerOppfolgingstilfellePerson = mockKafkaConsumerOppfolgingstilfellePerson,
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
                    pPersonOversiktStatus.moteplanleggerUbehandlet.shouldBeNull()
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
                val kafkaOppfolgingstilfellePersonServiceRecordRelevantFirst = ConsumerRecord(
                    OPPFOLGINGSTILFELLE_PERSON_TOPIC,
                    partition,
                    1,
                    "key1",
                    kafkaOppfolgingstilfellePersonServiceRelevantFirst,
                )

                val kafkaOppfolgingstilfellePersonServiceRelevantSecond = generateKafkaOppfolgingstilfellePerson(
                    personIdent = PersonIdent(kafkaOppfolgingstilfellePersonServiceRelevantFirst.personIdentNumber),
                ).copy(
                    referanseTilfelleBitInntruffet = kafkaOppfolgingstilfellePersonServiceRelevantFirst.referanseTilfelleBitInntruffet.plusSeconds(
                        1
                    )
                )
                val kafkaOppfolgingstilfellePersonServiceRecordRelevantSecond = ConsumerRecord(
                    OPPFOLGINGSTILFELLE_PERSON_TOPIC,
                    partition,
                    2,
                    "key3",
                    kafkaOppfolgingstilfellePersonServiceRelevantSecond,
                )

                val kafkaOppfolgingstilfellePersonServiceRelevantNewest = generateKafkaOppfolgingstilfellePerson(
                    personIdent = PersonIdent(kafkaOppfolgingstilfellePersonServiceRelevantFirst.personIdentNumber),
                ).copy(
                    referanseTilfelleBitInntruffet = kafkaOppfolgingstilfellePersonServiceRelevantSecond.referanseTilfelleBitInntruffet.plusSeconds(
                        1
                    )
                )
                val kafkaOppfolgingstilfellePersonServiceRecordRelevantNewest = ConsumerRecord(
                    OPPFOLGINGSTILFELLE_PERSON_TOPIC,
                    partition,
                    3,
                    "key3",
                    kafkaOppfolgingstilfellePersonServiceRelevantNewest,
                )

                every { mockKafkaConsumerOppfolgingstilfellePerson.poll(any<Duration>()) } returns ConsumerRecords(
                    mapOf(
                        oppfolgingstilfellePersonTopicPartition to listOf(
                            kafkaOppfolgingstilfellePersonServiceRecordRelevantSecond,
                        )
                    )
                )
                kafkaOppfolgingstilfellePersonService.pollAndProcessRecords(
                    kafkaConsumerOppfolgingstilfellePerson = mockKafkaConsumerOppfolgingstilfellePerson,
                )

                every { mockKafkaConsumerOppfolgingstilfellePerson.poll(any<Duration>()) } returns ConsumerRecords(
                    mapOf(
                        oppfolgingstilfellePersonTopicPartition to listOf(
                            kafkaOppfolgingstilfellePersonServiceRecordRelevantFirst,
                        )
                    )
                )
                kafkaOppfolgingstilfellePersonService.pollAndProcessRecords(
                    kafkaConsumerOppfolgingstilfellePerson = mockKafkaConsumerOppfolgingstilfellePerson,
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
                    pPersonOversiktStatus.moteplanleggerUbehandlet.shouldBeNull()
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

                every { mockKafkaConsumerOppfolgingstilfellePerson.poll(any<Duration>()) } returns ConsumerRecords(
                    mapOf(
                        oppfolgingstilfellePersonTopicPartition to listOf(
                            kafkaOppfolgingstilfellePersonServiceRecordRelevantNewest,
                        )
                    )
                )
                kafkaOppfolgingstilfellePersonService.pollAndProcessRecords(
                    kafkaConsumerOppfolgingstilfellePerson = mockKafkaConsumerOppfolgingstilfellePerson,
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
                    pPersonOversiktStatus.moteplanleggerUbehandlet.shouldBeNull()
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

            it("should only update latest OppfolgingstilfellPerson with the newest createdAt, if multiple relevant records on same personIdent with same referanseTilfelleBitUuid is received in different polls ") {
                val kafkaOppfolgingstilfellePersonServiceRelevantFirst = generateKafkaOppfolgingstilfellePerson(
                    personIdent = personIdentDefault,
                )
                val kafkaOppfolgingstilfellePersonServiceRecordRelevantFirst = ConsumerRecord(
                    OPPFOLGINGSTILFELLE_PERSON_TOPIC,
                    partition,
                    1,
                    "key1",
                    kafkaOppfolgingstilfellePersonServiceRelevantFirst,
                )

                val kafkaOppfolgingstilfellePersonServiceRelevantSecond = generateKafkaOppfolgingstilfellePerson(
                    personIdent = PersonIdent(kafkaOppfolgingstilfellePersonServiceRelevantFirst.personIdentNumber),
                ).copy(
                    createdAt = kafkaOppfolgingstilfellePersonServiceRelevantFirst.createdAt.plusSeconds(1),
                    referanseTilfelleBitInntruffet = kafkaOppfolgingstilfellePersonServiceRelevantFirst.referanseTilfelleBitInntruffet,
                    referanseTilfelleBitUuid = kafkaOppfolgingstilfellePersonServiceRelevantFirst.referanseTilfelleBitUuid,
                )
                val kafkaOppfolgingstilfellePersonServiceRecordRelevantSecond = ConsumerRecord(
                    OPPFOLGINGSTILFELLE_PERSON_TOPIC,
                    partition,
                    2,
                    "key2",
                    kafkaOppfolgingstilfellePersonServiceRelevantSecond,
                )

                val kafkaOppfolgingstilfellePersonServiceRelevantNewest = generateKafkaOppfolgingstilfellePerson(
                    personIdent = PersonIdent(kafkaOppfolgingstilfellePersonServiceRelevantFirst.personIdentNumber),
                ).copy(
                    createdAt = kafkaOppfolgingstilfellePersonServiceRelevantSecond.createdAt.plusSeconds(1),
                    referanseTilfelleBitInntruffet = kafkaOppfolgingstilfellePersonServiceRelevantFirst.referanseTilfelleBitInntruffet,
                    referanseTilfelleBitUuid = kafkaOppfolgingstilfellePersonServiceRelevantFirst.referanseTilfelleBitUuid,
                )
                val kafkaOppfolgingstilfellePersonServiceRecordRelevantNewest = ConsumerRecord(
                    OPPFOLGINGSTILFELLE_PERSON_TOPIC,
                    partition,
                    3,
                    "key3",
                    kafkaOppfolgingstilfellePersonServiceRelevantNewest,
                )

                every { mockKafkaConsumerOppfolgingstilfellePerson.poll(any<Duration>()) } returns ConsumerRecords(
                    mapOf(
                        oppfolgingstilfellePersonTopicPartition to listOf(
                            kafkaOppfolgingstilfellePersonServiceRecordRelevantSecond,
                        )
                    )
                )
                kafkaOppfolgingstilfellePersonService.pollAndProcessRecords(
                    kafkaConsumerOppfolgingstilfellePerson = mockKafkaConsumerOppfolgingstilfellePerson,
                )

                every { mockKafkaConsumerOppfolgingstilfellePerson.poll(any<Duration>()) } returns ConsumerRecords(
                    mapOf(
                        oppfolgingstilfellePersonTopicPartition to listOf(
                            kafkaOppfolgingstilfellePersonServiceRecordRelevantFirst,
                        )
                    )
                )
                kafkaOppfolgingstilfellePersonService.pollAndProcessRecords(
                    kafkaConsumerOppfolgingstilfellePerson = mockKafkaConsumerOppfolgingstilfellePerson,
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
                    pPersonOversiktStatus.moteplanleggerUbehandlet.shouldBeNull()
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

                every { mockKafkaConsumerOppfolgingstilfellePerson.poll(any<Duration>()) } returns ConsumerRecords(
                    mapOf(
                        oppfolgingstilfellePersonTopicPartition to listOf(
                            kafkaOppfolgingstilfellePersonServiceRecordRelevantNewest,
                        )
                    )
                )
                kafkaOppfolgingstilfellePersonService.pollAndProcessRecords(
                    kafkaConsumerOppfolgingstilfellePerson = mockKafkaConsumerOppfolgingstilfellePerson,
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
                    pPersonOversiktStatus.moteplanleggerUbehandlet.shouldBeNull()
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
                val kafkaOppfolgingstilfellePersonServiceRecordRelevantFirst = ConsumerRecord(
                    OPPFOLGINGSTILFELLE_PERSON_TOPIC,
                    partition,
                    1,
                    "key1",
                    kafkaOppfolgingstilfellePersonServiceRelevantFirst,
                )

                every { mockKafkaConsumerOppfolgingstilfellePerson.poll(any<Duration>()) } returns ConsumerRecords(
                    mapOf(
                        oppfolgingstilfellePersonTopicPartition to listOf(
                            kafkaOppfolgingstilfellePersonServiceRecordRelevantFirst,
                        )
                    )
                )
                kafkaOppfolgingstilfellePersonService.pollAndProcessRecords(
                    kafkaConsumerOppfolgingstilfellePerson = mockKafkaConsumerOppfolgingstilfellePerson,
                )

                val virksomhetsnummerListSecond = listOf(
                    Virksomhetsnummer(VIRKSOMHETSNUMMER_2),
                    Virksomhetsnummer(VIRKSOMHETSNUMMER_3),
                )
                val kafkaOppfolgingstilfellePersonServiceRelevantSecond = generateKafkaOppfolgingstilfellePerson(
                    personIdent = personIdentDefault,
                    virksomhetsnummerList = virksomhetsnummerListSecond,
                )
                val kafkaOppfolgingstilfellePersonServiceRecordRelevantSecond = ConsumerRecord(
                    OPPFOLGINGSTILFELLE_PERSON_TOPIC,
                    partition,
                    2,
                    "key2",
                    kafkaOppfolgingstilfellePersonServiceRelevantSecond,
                )

                every { mockKafkaConsumerOppfolgingstilfellePerson.poll(any<Duration>()) } returns ConsumerRecords(
                    mapOf(
                        oppfolgingstilfellePersonTopicPartition to listOf(
                            kafkaOppfolgingstilfellePersonServiceRecordRelevantSecond,
                        )
                    )
                )
                kafkaOppfolgingstilfellePersonService.pollAndProcessRecords(
                    kafkaConsumerOppfolgingstilfellePerson = mockKafkaConsumerOppfolgingstilfellePerson,
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
                    pPersonOversiktStatus.moteplanleggerUbehandlet.shouldBeNull()
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
        }
    }
})
