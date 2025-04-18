package no.nav.syfo.dialogmotekandidat.kafka

import io.mockk.*
import kotlinx.coroutines.runBlocking
import no.nav.syfo.personstatus.infrastructure.database.queries.getPersonOversiktStatusList
import no.nav.syfo.testutil.*
import no.nav.syfo.testutil.generator.*
import no.nav.syfo.util.nowUTC
import org.amshove.kluent.*
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.Duration

class KafkaDialogmotekandidatEndringServiceSpek : Spek({
    val externalMockEnvironment = ExternalMockEnvironment.instance
    val database = externalMockEnvironment.database

    val kafkaDialogmotekandidatEndringService = TestKafkaModule.kafkaDialogmotekandidatEndringService
    val mockKafkaConsumerDialogmotekandidatEndring = TestKafkaModule.kafkaConsumerDialogmotekandidatEndring

    val dialogmoteKandidatTopicPartition = dialogmotekandidatEndringTopicPartition()
    val kafkaDialogmotekandidatEndringStoppunktYesterday = generateKafkaDialogmotekandidatEndringStoppunkt(
        personIdent = UserConstants.ARBEIDSTAKER_FNR,
        createdAt = nowUTC().minusDays(1)
    )
    val kafkaDialogmotekandidatEndringStoppunktConsumerRecord = dialogmotekandidatEndringConsumerRecord(
        kafkaDialogmotekandidatEndring = kafkaDialogmotekandidatEndringStoppunktYesterday
    )
    val kafkaDialogmotekandidatEndringUnntakToday = generateKafkaDialogmotekandidatEndringUnntak(
        personIdent = UserConstants.ARBEIDSTAKER_FNR,
        createdAt = nowUTC()
    )
    val kafkaDialogmotekandidatEndringUnntakConsumerRecord = dialogmotekandidatEndringConsumerRecord(
        kafkaDialogmotekandidatEndring = kafkaDialogmotekandidatEndringUnntakToday
    )

    beforeEachTest {
        database.dropData()

        clearMocks(mockKafkaConsumerDialogmotekandidatEndring)
        every { mockKafkaConsumerDialogmotekandidatEndring.commitSync() } returns Unit
    }

    describe("${KafkaDialogmotekandidatEndringService::class.java.simpleName}: pollAndProcessRecords") {
        it("creates new PersonOversiktStatus if no PersonOversiktStatus exists for personident") {
            every { mockKafkaConsumerDialogmotekandidatEndring.poll(any<Duration>()) } returns ConsumerRecords(
                mapOf(
                    dialogmoteKandidatTopicPartition to listOf(
                        kafkaDialogmotekandidatEndringStoppunktConsumerRecord,
                    )
                )
            )

            runBlocking {
                kafkaDialogmotekandidatEndringService.pollAndProcessRecords(kafkaConsumer = mockKafkaConsumerDialogmotekandidatEndring)
            }

            verify(exactly = 1) {
                mockKafkaConsumerDialogmotekandidatEndring.commitSync()
            }

            val pPersonOversiktStatusList =
                database.connection.getPersonOversiktStatusList(UserConstants.ARBEIDSTAKER_FNR)

            pPersonOversiktStatusList.size shouldBeEqualTo 1

            val pPersonOversiktStatus = pPersonOversiktStatusList.first()

            pPersonOversiktStatus.fnr shouldBeEqualTo kafkaDialogmotekandidatEndringStoppunktYesterday.personIdentNumber
            pPersonOversiktStatus.dialogmotekandidat shouldBeEqualTo kafkaDialogmotekandidatEndringStoppunktYesterday.kandidat
            pPersonOversiktStatus.dialogmotekandidatGeneratedAt.shouldNotBeNull()

            pPersonOversiktStatus.enhet.shouldBeNull()
            pPersonOversiktStatus.veilederIdent.shouldBeNull()
            pPersonOversiktStatus.motebehovUbehandlet.shouldBeNull()
            pPersonOversiktStatus.oppfolgingsplanLPSBistandUbehandlet.shouldBeNull()
        }
        it("updates existing PersonOversikStatus when PersonOversiktStatus exists for personident") {
            every { mockKafkaConsumerDialogmotekandidatEndring.poll(any<Duration>()) } returns ConsumerRecords(
                mapOf(
                    dialogmoteKandidatTopicPartition to listOf(
                        kafkaDialogmotekandidatEndringStoppunktConsumerRecord,
                    )
                )
            )

            val kafkaOppfolgingstilfellePerson = generateKafkaOppfolgingstilfellePerson()
            val kafkaOppfolgingstilfelle = kafkaOppfolgingstilfellePerson.oppfolgingstilfelleList.first()
            database.createPersonOversiktStatus(
                personOversiktStatus = kafkaOppfolgingstilfellePerson.toPersonOversiktStatus(kafkaOppfolgingstilfelle)
            )

            runBlocking {
                kafkaDialogmotekandidatEndringService.pollAndProcessRecords(kafkaConsumer = mockKafkaConsumerDialogmotekandidatEndring)
            }

            verify(exactly = 1) {
                mockKafkaConsumerDialogmotekandidatEndring.commitSync()
            }

            val pPersonOversiktStatusList =
                database.connection.getPersonOversiktStatusList(UserConstants.ARBEIDSTAKER_FNR)

            pPersonOversiktStatusList.size shouldBeEqualTo 1
            val pPersonOversiktStatus = pPersonOversiktStatusList.first()
            pPersonOversiktStatus.oppfolgingstilfelleGeneratedAt.shouldNotBeNull()
            pPersonOversiktStatus.oppfolgingstilfelleBitReferanseUuid.shouldNotBeNull()

            pPersonOversiktStatus.fnr shouldBeEqualTo kafkaDialogmotekandidatEndringStoppunktYesterday.personIdentNumber
            pPersonOversiktStatus.dialogmotekandidat shouldBeEqualTo kafkaDialogmotekandidatEndringStoppunktYesterday.kandidat
            pPersonOversiktStatus.dialogmotekandidatGeneratedAt.shouldNotBeNull()
        }
        it("updates PersonOversiktStatus if received dialogmotekandidat-endring created after existing PersonOversiktStatus-dialogmotekandidat") {
            every { mockKafkaConsumerDialogmotekandidatEndring.poll(any<Duration>()) } returns ConsumerRecords(
                mapOf(
                    dialogmoteKandidatTopicPartition to listOf(
                        kafkaDialogmotekandidatEndringUnntakConsumerRecord,
                    )
                )
            )

            val existingPersonOversiktStatus = kafkaDialogmotekandidatEndringStoppunktYesterday.toPersonOversiktStatus()
            database.createPersonOversiktStatus(
                personOversiktStatus = existingPersonOversiktStatus
            )
            runBlocking {
                kafkaDialogmotekandidatEndringService.pollAndProcessRecords(kafkaConsumer = mockKafkaConsumerDialogmotekandidatEndring)
            }

            verify(exactly = 1) {
                mockKafkaConsumerDialogmotekandidatEndring.commitSync()
            }

            val pPersonOversiktStatusList =
                database.connection.getPersonOversiktStatusList(UserConstants.ARBEIDSTAKER_FNR)

            pPersonOversiktStatusList.size shouldBeEqualTo 1
            val pPersonOversiktStatus = pPersonOversiktStatusList.first()
            pPersonOversiktStatus.fnr shouldBeEqualTo kafkaDialogmotekandidatEndringUnntakToday.personIdentNumber
            pPersonOversiktStatus.dialogmotekandidat shouldBeEqualTo kafkaDialogmotekandidatEndringUnntakToday.kandidat
            pPersonOversiktStatus.dialogmotekandidatGeneratedAt.shouldNotBeNull()
        }
        it("do not update PersonOversiktStatus if received dialogmotekandidat-endring created before existing PersonOversiktStatus-dialogmotekandidat") {
            every { mockKafkaConsumerDialogmotekandidatEndring.poll(any<Duration>()) } returns ConsumerRecords(
                mapOf(
                    dialogmoteKandidatTopicPartition to listOf(
                        kafkaDialogmotekandidatEndringStoppunktConsumerRecord,
                    )
                )
            )

            val existingPersonOversiktStatus = kafkaDialogmotekandidatEndringUnntakToday.toPersonOversiktStatus()
            database.createPersonOversiktStatus(
                personOversiktStatus = existingPersonOversiktStatus
            )
            runBlocking {
                kafkaDialogmotekandidatEndringService.pollAndProcessRecords(kafkaConsumer = mockKafkaConsumerDialogmotekandidatEndring)
            }

            verify(exactly = 1) {
                mockKafkaConsumerDialogmotekandidatEndring.commitSync()
            }

            val pPersonOversiktStatusList =
                database.connection.getPersonOversiktStatusList(UserConstants.ARBEIDSTAKER_FNR)

            pPersonOversiktStatusList.size shouldBeEqualTo 1
            val pPersonOversiktStatus = pPersonOversiktStatusList.first()
            pPersonOversiktStatus.fnr shouldBeEqualTo kafkaDialogmotekandidatEndringUnntakToday.personIdentNumber
            pPersonOversiktStatus.dialogmotekandidat shouldBeEqualTo kafkaDialogmotekandidatEndringUnntakToday.kandidat
            pPersonOversiktStatus.dialogmotekandidatGeneratedAt.shouldNotBeNull()
        }
    }
})
