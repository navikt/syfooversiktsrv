package no.nav.syfo.trengeroppfolging.kafka

import io.ktor.server.testing.*
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.isActive
import no.nav.syfo.trengeroppfolging.TrengerOppfolgingService
import no.nav.syfo.personstatus.db.getPersonOversiktStatusList
import no.nav.syfo.personstatus.domain.PersonOversiktStatus
import no.nav.syfo.testutil.ExternalMockEnvironment
import no.nav.syfo.testutil.UserConstants
import no.nav.syfo.testutil.createPersonOversiktStatus
import no.nav.syfo.testutil.dropData
import no.nav.syfo.testutil.generator.generateKafkaHuskelapp
import no.nav.syfo.testutil.generator.huskelappConsumerRecord
import no.nav.syfo.testutil.generator.huskelappTopicPartition
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.Duration
import java.time.LocalDate

class TrengerOppfolgingConsumerSpek : Spek({
    with(TestApplicationEngine()) {
        start()

        val externalMockEnvironment = ExternalMockEnvironment.instance
        val database = externalMockEnvironment.database

        val kafkaConsumerMock = mockk<KafkaConsumer<String, KafkaHuskelapp>>()
        val trengerOppfolgingService = TrengerOppfolgingService(database)
        val trengerOppfolgingConsumer = TrengerOppfolgingConsumer(trengerOppfolgingService)

        val frist = LocalDate.now().plusWeeks(1)

        beforeEachTest {
            database.connection.dropData()

            clearMocks(kafkaConsumerMock)
            every { kafkaConsumerMock.commitSync() } returns Unit
        }

        describe("${TrengerOppfolgingConsumer::class.java.simpleName}: pollAndProcessRecords") {
            describe("no PersonOversiktStatus exists for personident") {
                it("creates new PersonOversiktStatus for personident from kafka record with active huskelapp and frist") {
                    val activeHuskelappWithFrist = generateKafkaHuskelapp(isActive = true, frist = frist)
                    mockIncomingKafkaRecord(
                        kafkaRecord = activeHuskelappWithFrist,
                        kafkaConsumerMock = kafkaConsumerMock,
                    )

                    trengerOppfolgingConsumer.pollAndProcessRecords(
                        kafkaConsumer = kafkaConsumerMock,
                    )

                    verify(exactly = 1) {
                        kafkaConsumerMock.commitSync()
                    }

                    val pPersonOversiktStatusList =
                        database.connection.use { it.getPersonOversiktStatusList(UserConstants.ARBEIDSTAKER_FNR) }
                    pPersonOversiktStatusList.size shouldBeEqualTo 1
                    val pPersonOversiktStatus = pPersonOversiktStatusList.first()
                    pPersonOversiktStatus.fnr shouldBeEqualTo activeHuskelappWithFrist.personIdent
                    pPersonOversiktStatus.trengerOppfolging.shouldBeTrue()
                    pPersonOversiktStatus.trengerOppfolgingFrist shouldBeEqualTo frist
                }
                it("creates new PersonOversiktStatus for personident from kafka record with active huskelapp no frist") {
                    val activeHuskelappNoFrist = generateKafkaHuskelapp(isActive = true, frist = null)
                    mockIncomingKafkaRecord(
                        kafkaRecord = activeHuskelappNoFrist,
                        kafkaConsumerMock = kafkaConsumerMock,
                    )

                    trengerOppfolgingConsumer.pollAndProcessRecords(
                        kafkaConsumer = kafkaConsumerMock,
                    )

                    verify(exactly = 1) {
                        kafkaConsumerMock.commitSync()
                    }

                    val pPersonOversiktStatusList =
                        database.connection.use { it.getPersonOversiktStatusList(UserConstants.ARBEIDSTAKER_FNR) }
                    pPersonOversiktStatusList.size shouldBeEqualTo 1
                    val pPersonOversiktStatus = pPersonOversiktStatusList.first()
                    pPersonOversiktStatus.fnr shouldBeEqualTo activeHuskelappNoFrist.personIdent
                    pPersonOversiktStatus.trengerOppfolging.shouldBeTrue()
                    pPersonOversiktStatus.trengerOppfolgingFrist.shouldBeNull()
                }
            }
            describe("existing PersonOversikStatus for personident") {
                it("updates trenger_oppfolging and trenger_oppfolging_frist from kafka record with active huskelapp and frist") {
                    val activeHuskelappWithFrist = generateKafkaHuskelapp(isActive = true, frist = frist)
                    val personident = UserConstants.ARBEIDSTAKER_FNR
                    database.createPersonOversiktStatus(
                        personOversiktStatus = PersonOversiktStatus(fnr = personident)
                    )
                    mockIncomingKafkaRecord(
                        kafkaRecord = activeHuskelappWithFrist,
                        kafkaConsumerMock = kafkaConsumerMock,
                    )

                    trengerOppfolgingConsumer.pollAndProcessRecords(
                        kafkaConsumer = kafkaConsumerMock,
                    )

                    verify(exactly = 1) {
                        kafkaConsumerMock.commitSync()
                    }
                    val pPersonOversiktStatusList =
                        database.connection.use { it.getPersonOversiktStatusList(personident) }
                    pPersonOversiktStatusList.size shouldBeEqualTo 1
                    val pPersonOversiktStatus = pPersonOversiktStatusList.first()
                    pPersonOversiktStatus.fnr shouldBeEqualTo activeHuskelappWithFrist.personIdent
                    pPersonOversiktStatus.trengerOppfolging.shouldBeTrue()
                    pPersonOversiktStatus.trengerOppfolgingFrist shouldBeEqualTo frist
                }
                it("updates to trenger_oppfolging false and trenger_oppfolging_frist null from kafka record with inactive huskelapp and frist") {
                    val inActiveHuskelappWithFrist = generateKafkaHuskelapp(isActive = false, frist = frist)
                    val personident = UserConstants.ARBEIDSTAKER_FNR
                    database.createPersonOversiktStatus(
                        personOversiktStatus = PersonOversiktStatus(
                            fnr = personident,
                        ).copy(
                            trengerOppfolging = true,
                        )
                    )
                    mockIncomingKafkaRecord(
                        kafkaRecord = inActiveHuskelappWithFrist,
                        kafkaConsumerMock = kafkaConsumerMock,
                    )

                    trengerOppfolgingConsumer.pollAndProcessRecords(
                        kafkaConsumer = kafkaConsumerMock,
                    )

                    verify(exactly = 1) {
                        kafkaConsumerMock.commitSync()
                    }
                    val pPersonOversiktStatusList =
                        database.connection.use { it.getPersonOversiktStatusList(personident) }
                    pPersonOversiktStatusList.size shouldBeEqualTo 1
                    val pPersonOversiktStatus = pPersonOversiktStatusList.first()
                    pPersonOversiktStatus.fnr shouldBeEqualTo inActiveHuskelappWithFrist.personIdent
                    pPersonOversiktStatus.trengerOppfolging.shouldBeFalse()
                    pPersonOversiktStatus.trengerOppfolgingFrist.shouldBeNull()
                }
                it("updates to trenger_oppfolging false and trenger_oppfolging_frist null from kafka record with inactive huskelapp and no frist") {
                    val inActiveHuskelappNoFrist = generateKafkaHuskelapp(isActive = false, frist = null)
                    val personident = UserConstants.ARBEIDSTAKER_FNR
                    database.createPersonOversiktStatus(
                        personOversiktStatus = PersonOversiktStatus(
                            fnr = personident,
                        ).copy(
                            trengerOppfolging = true,
                        )
                    )
                    mockIncomingKafkaRecord(
                        kafkaRecord = inActiveHuskelappNoFrist,
                        kafkaConsumerMock = kafkaConsumerMock,
                    )

                    trengerOppfolgingConsumer.pollAndProcessRecords(
                        kafkaConsumer = kafkaConsumerMock,
                    )

                    verify(exactly = 1) {
                        kafkaConsumerMock.commitSync()
                    }
                    val pPersonOversiktStatusList =
                        database.connection.use { it.getPersonOversiktStatusList(personident) }
                    pPersonOversiktStatusList.size shouldBeEqualTo 1
                    val pPersonOversiktStatus = pPersonOversiktStatusList.first()
                    pPersonOversiktStatus.fnr shouldBeEqualTo inActiveHuskelappNoFrist.personIdent
                    pPersonOversiktStatus.trengerOppfolging.shouldBeFalse()
                    pPersonOversiktStatus.trengerOppfolgingFrist.shouldBeNull()
                }
            }
        }
    }
})

fun mockIncomingKafkaRecord(kafkaRecord: KafkaHuskelapp, kafkaConsumerMock: KafkaConsumer<String, KafkaHuskelapp>) {
    every { kafkaConsumerMock.poll(any<Duration>()) } returns ConsumerRecords(
        mapOf(
            huskelappTopicPartition() to listOf(
                huskelappConsumerRecord(
                    kafkaHuskelapp = kafkaRecord,
                ),
            )
        )
    )
}
