package no.nav.syfo.dialogmotestatusendring.kafka

import io.ktor.server.testing.*
import io.mockk.*
import no.nav.syfo.dialogmotestatusendring.domain.DialogmoteStatusendring
import no.nav.syfo.dialogmotestatusendring.domain.DialogmoteStatusendringType
import no.nav.syfo.oppfolgingstilfelle.kafka.toPersonOversiktStatus
import no.nav.syfo.personstatus.getPersonOversiktStatusList
import no.nav.syfo.testutil.*
import no.nav.syfo.testutil.generator.*
import no.nav.syfo.util.nowUTC
import org.amshove.kluent.*
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.Duration

class KafkaDialogmoteStatusendringServiceSpek : Spek({
    with(TestApplicationEngine()) {
        start()

        val externalMockEnvironment = ExternalMockEnvironment.instance
        val database = externalMockEnvironment.database

        val kafkaDialogmoteStatusendringService = TestKafkaModule.kafkaDialogmoteStatusendringService
        val mockKafkaConsumerDialogmoteStatusendring = TestKafkaModule.kafkaConsumerDialogmoteStatusendring

        val dialogmoteStatusendringTopicPartition = dialogmoteStatusendringTopicPartition()
        val kafkaDialogmoteStatusendringToday = generateKafkaDialogmoteStatusendring(
            personIdent = UserConstants.ARBEIDSTAKER_FNR,
            type = DialogmoteStatusendringType.INNKALT,
            endringsTidspunkt = nowUTC(),
        )
        val kafkaDialogmoteStatusendringLastYear = generateKafkaDialogmoteStatusendring(
            personIdent = UserConstants.ARBEIDSTAKER_FNR,
            type = DialogmoteStatusendringType.AVLYST,
            endringsTidspunkt = nowUTC().minusYears(1),
        )
        val kafkaDialogmoteStatusendringTodayConsumerRecord = dialogmoteStatusendringConsumerRecord(
            kafkaDialogmoteStatusendring = kafkaDialogmoteStatusendringToday,
        )
        val kafkaDialogmoteStatusendringLastYearConsumerRecord = dialogmoteStatusendringConsumerRecord(
            kafkaDialogmoteStatusendring = kafkaDialogmoteStatusendringLastYear,
        )

        beforeEachTest {
            database.connection.dropData()

            clearMocks(mockKafkaConsumerDialogmoteStatusendring)
            every { mockKafkaConsumerDialogmoteStatusendring.commitSync() } returns Unit
        }

        describe("${KafkaDialogmoteStatusendringService::class.java.simpleName}: pollAndProcessRecords") {
            it("creates new PersonOversiktStatus if no PersonOversiktStatus exists for personident") {
                every { mockKafkaConsumerDialogmoteStatusendring.poll(any<Duration>()) } returns ConsumerRecords(
                    mapOf(
                        dialogmoteStatusendringTopicPartition to listOf(
                            kafkaDialogmoteStatusendringTodayConsumerRecord,
                        )
                    )
                )

                kafkaDialogmoteStatusendringService.pollAndProcessRecords(
                    kafkaConsumer = mockKafkaConsumerDialogmoteStatusendring
                )

                verify(exactly = 1) {
                    mockKafkaConsumerDialogmoteStatusendring.commitSync()
                }

                val pPersonOversiktStatusList =
                    database.connection.getPersonOversiktStatusList(UserConstants.ARBEIDSTAKER_FNR)

                pPersonOversiktStatusList.size shouldBeEqualTo 1

                val pPersonOversiktStatus = pPersonOversiktStatusList.first()

                pPersonOversiktStatus.fnr shouldBeEqualTo kafkaDialogmoteStatusendringToday.getPersonIdent()
                pPersonOversiktStatus.motestatus shouldBeEqualTo kafkaDialogmoteStatusendringToday.getStatusEndringType()
                pPersonOversiktStatus.motestatusGeneratedAt.shouldNotBeNull()

                pPersonOversiktStatus.dialogmotekandidat.shouldBeNull()
                pPersonOversiktStatus.enhet.shouldBeNull()
                pPersonOversiktStatus.veilederIdent.shouldBeNull()
                pPersonOversiktStatus.motebehovUbehandlet.shouldBeNull()
                pPersonOversiktStatus.moteplanleggerUbehandlet.shouldBeNull()
                pPersonOversiktStatus.oppfolgingsplanLPSBistandUbehandlet.shouldBeNull()
            }
            it("updates existing PersonOversikStatus when PersonOversiktStatus without motestatus exists for personident") {
                every { mockKafkaConsumerDialogmoteStatusendring.poll(any<Duration>()) } returns ConsumerRecords(
                    mapOf(
                        dialogmoteStatusendringTopicPartition to listOf(
                            kafkaDialogmoteStatusendringTodayConsumerRecord,
                        )
                    )
                )

                val kafkaOppfolgingstilfellePerson = generateKafkaOppfolgingstilfellePerson()
                val kafkaOppfolgingstilfelle = kafkaOppfolgingstilfellePerson.oppfolgingstilfelleList.first()
                database.createPersonOversiktStatus(
                    personOversiktStatus = kafkaOppfolgingstilfellePerson.toPersonOversiktStatus(kafkaOppfolgingstilfelle)
                )

                kafkaDialogmoteStatusendringService.pollAndProcessRecords(
                    kafkaConsumer = mockKafkaConsumerDialogmoteStatusendring
                )

                verify(exactly = 1) {
                    mockKafkaConsumerDialogmoteStatusendring.commitSync()
                }

                val pPersonOversiktStatusList =
                    database.connection.getPersonOversiktStatusList(UserConstants.ARBEIDSTAKER_FNR)

                pPersonOversiktStatusList.size shouldBeEqualTo 1
                val pPersonOversiktStatus = pPersonOversiktStatusList.first()
                pPersonOversiktStatus.oppfolgingstilfelleGeneratedAt.shouldNotBeNull()
                pPersonOversiktStatus.oppfolgingstilfelleBitReferanseUuid.shouldNotBeNull()

                pPersonOversiktStatus.fnr shouldBeEqualTo kafkaDialogmoteStatusendringToday.getPersonIdent()
                pPersonOversiktStatus.motestatus shouldBeEqualTo kafkaDialogmoteStatusendringToday.getStatusEndringType()
                pPersonOversiktStatus.motestatusGeneratedAt.shouldNotBeNull()
            }
            it("updates PersonOversiktStatus if received motestatus-endring created after existing PersonOversiktStatus-motestatus") {
                every { mockKafkaConsumerDialogmoteStatusendring.poll(any<Duration>()) } returns ConsumerRecords(
                    mapOf(
                        dialogmoteStatusendringTopicPartition to listOf(
                            kafkaDialogmoteStatusendringTodayConsumerRecord,
                        )
                    )
                )

                val existingPersonOversiktStatus = DialogmoteStatusendring.create(
                    kafkaDialogmoteStatusEndring = kafkaDialogmoteStatusendringLastYear
                ).toPersonOversiktStatus()
                database.createPersonOversiktStatus(
                    personOversiktStatus = existingPersonOversiktStatus
                )

                kafkaDialogmoteStatusendringService.pollAndProcessRecords(
                    kafkaConsumer = mockKafkaConsumerDialogmoteStatusendring
                )

                verify(exactly = 1) {
                    mockKafkaConsumerDialogmoteStatusendring.commitSync()
                }

                val pPersonOversiktStatusList =
                    database.connection.getPersonOversiktStatusList(UserConstants.ARBEIDSTAKER_FNR)

                pPersonOversiktStatusList.size shouldBeEqualTo 1
                val pPersonOversiktStatus = pPersonOversiktStatusList.first()

                pPersonOversiktStatus.fnr shouldBeEqualTo kafkaDialogmoteStatusendringToday.getPersonIdent()
                pPersonOversiktStatus.motestatus shouldBeEqualTo kafkaDialogmoteStatusendringToday.getStatusEndringType()
                pPersonOversiktStatus.motestatusGeneratedAt.shouldNotBeNull()
            }
            it("do not update PersonOversiktStatus if received motestatus-endring created before existing PersonOversiktStatus-motestatus") {
                every { mockKafkaConsumerDialogmoteStatusendring.poll(any<Duration>()) } returns ConsumerRecords(
                    mapOf(
                        dialogmoteStatusendringTopicPartition to listOf(
                            kafkaDialogmoteStatusendringLastYearConsumerRecord,
                        )
                    )
                )

                val existingPersonOversiktStatus = DialogmoteStatusendring.create(
                    kafkaDialogmoteStatusEndring = kafkaDialogmoteStatusendringToday
                ).toPersonOversiktStatus()
                database.createPersonOversiktStatus(
                    personOversiktStatus = existingPersonOversiktStatus
                )

                kafkaDialogmoteStatusendringService.pollAndProcessRecords(
                    kafkaConsumer = mockKafkaConsumerDialogmoteStatusendring
                )

                verify(exactly = 1) {
                    mockKafkaConsumerDialogmoteStatusendring.commitSync()
                }

                val pPersonOversiktStatusList =
                    database.connection.getPersonOversiktStatusList(UserConstants.ARBEIDSTAKER_FNR)

                pPersonOversiktStatusList.size shouldBeEqualTo 1
                val pPersonOversiktStatus = pPersonOversiktStatusList.first()

                pPersonOversiktStatus.fnr shouldBeEqualTo kafkaDialogmoteStatusendringToday.getPersonIdent()
                pPersonOversiktStatus.motestatus shouldBeEqualTo kafkaDialogmoteStatusendringToday.getStatusEndringType()
                pPersonOversiktStatus.motestatusGeneratedAt.shouldNotBeNull()
            }
        }
    }
})
