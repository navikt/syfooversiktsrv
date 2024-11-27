package no.nav.syfo.trengeroppfolging.kafka

import io.ktor.server.testing.*
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.syfo.trengeroppfolging.TrengerOppfolgingService
import no.nav.syfo.personstatus.db.getPersonOversiktStatusList
import no.nav.syfo.personstatus.domain.PersonOversiktStatus
import no.nav.syfo.testutil.*
import no.nav.syfo.testutil.generator.generateKafkaHuskelapp
import no.nav.syfo.testutil.generator.huskelappConsumerRecord
import no.nav.syfo.testutil.generator.huskelappTopicPartition
import org.amshove.kluent.*
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
        val internalMockEnvironment = InternalMockEnvironment.instance
        val database = externalMockEnvironment.database

        val kafkaConsumerMock = mockk<KafkaConsumer<String, KafkaHuskelapp>>()
        val trengerOppfolgingService = TrengerOppfolgingService(
            database = database,
            personBehandlendeEnhetService = internalMockEnvironment.personBehandlendeEnhetService
        )
        val trengerOppfolgingConsumer = TrengerOppfolgingConsumer(trengerOppfolgingService)

        val frist = LocalDate.now().plusWeeks(1)

        beforeEachTest {
            database.dropData()

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

                    runBlocking { trengerOppfolgingConsumer.pollAndProcessRecords(kafkaConsumer = kafkaConsumerMock) }

                    verify(exactly = 1) {
                        kafkaConsumerMock.commitSync()
                    }

                    val pPersonOversiktStatusList =
                        database.connection.use { it.getPersonOversiktStatusList(UserConstants.ARBEIDSTAKER_FNR) }
                    pPersonOversiktStatusList.size shouldBeEqualTo 1
                    val pPersonOversiktStatus = pPersonOversiktStatusList.first()
                    pPersonOversiktStatus.fnr shouldBeEqualTo activeHuskelappWithFrist.personIdent
                    pPersonOversiktStatus.trengerOppfolging.shouldBeTrue()
                }
                it("creates new PersonOversiktStatus for personident from kafka record with active huskelapp no frist") {
                    val activeHuskelappNoFrist = generateKafkaHuskelapp(isActive = true, frist = null)
                    mockIncomingKafkaRecord(
                        kafkaRecord = activeHuskelappNoFrist,
                        kafkaConsumerMock = kafkaConsumerMock,
                    )

                    runBlocking {
                        trengerOppfolgingConsumer.pollAndProcessRecords(kafkaConsumer = kafkaConsumerMock)
                    }

                    verify(exactly = 1) {
                        kafkaConsumerMock.commitSync()
                    }

                    val pPersonOversiktStatusList =
                        database.connection.use { it.getPersonOversiktStatusList(UserConstants.ARBEIDSTAKER_FNR) }
                    pPersonOversiktStatusList.size shouldBeEqualTo 1
                    val pPersonOversiktStatus = pPersonOversiktStatusList.first()
                    pPersonOversiktStatus.fnr shouldBeEqualTo activeHuskelappNoFrist.personIdent
                    pPersonOversiktStatus.trengerOppfolging.shouldBeTrue()
                }
                it("updates PersonOversiktStatus tildeltEnhet for personident from kafka record with active huskelapp") {
                    val activeHuskelappNoFrist = generateKafkaHuskelapp(isActive = true, frist = null)
                    mockIncomingKafkaRecord(
                        kafkaRecord = activeHuskelappNoFrist,
                        kafkaConsumerMock = kafkaConsumerMock,
                    )

                    runBlocking {
                        trengerOppfolgingConsumer.pollAndProcessRecords(kafkaConsumer = kafkaConsumerMock)
                    }

                    verify(exactly = 1) {
                        kafkaConsumerMock.commitSync()
                    }

                    val pPersonOversiktStatusList =
                        database.connection.use { it.getPersonOversiktStatusList(UserConstants.ARBEIDSTAKER_FNR) }
                    pPersonOversiktStatusList.size shouldBeEqualTo 1
                    val pPersonOversiktStatus = pPersonOversiktStatusList.first()
                    pPersonOversiktStatus.enhet.shouldNotBeNull()
                }
                it("does not update PersonOversiktStatus tildeltEnhet for personident from kafka record with inactive huskelapp") {
                    val inactiveHuskelappNoFrist = generateKafkaHuskelapp(isActive = false, frist = null)
                    mockIncomingKafkaRecord(
                        kafkaRecord = inactiveHuskelappNoFrist,
                        kafkaConsumerMock = kafkaConsumerMock,
                    )

                    runBlocking {
                        trengerOppfolgingConsumer.pollAndProcessRecords(kafkaConsumer = kafkaConsumerMock)
                    }

                    verify(exactly = 1) {
                        kafkaConsumerMock.commitSync()
                    }

                    val pPersonOversiktStatusList =
                        database.connection.use { it.getPersonOversiktStatusList(UserConstants.ARBEIDSTAKER_FNR) }
                    pPersonOversiktStatusList.size shouldBeEqualTo 1
                    val pPersonOversiktStatus = pPersonOversiktStatusList.first()
                    pPersonOversiktStatus.enhet.shouldBeNull()
                }
                it("does not update PersonOversiktStatus tildeltEnhet for personident from kafka record with active huskelapp and failing call to behandlende enhet") {
                    val personIdent = UserConstants.ARBEIDSTAKER_ENHET_ERROR_PERSONIDENT.value
                    val activeHuskelappNoFrist = generateKafkaHuskelapp(isActive = true, frist = null, personIdent = personIdent)
                    mockIncomingKafkaRecord(
                        kafkaRecord = activeHuskelappNoFrist,
                        kafkaConsumerMock = kafkaConsumerMock,
                    )

                    runBlocking {
                        trengerOppfolgingConsumer.pollAndProcessRecords(kafkaConsumer = kafkaConsumerMock)
                    }

                    verify(exactly = 1) {
                        kafkaConsumerMock.commitSync()
                    }

                    val pPersonOversiktStatusList =
                        database.connection.use { it.getPersonOversiktStatusList(personIdent) }
                    pPersonOversiktStatusList.size shouldBeEqualTo 1
                    val pPersonOversiktStatus = pPersonOversiktStatusList.first()
                    pPersonOversiktStatus.enhet.shouldBeNull()
                }
            }
            describe("existing PersonOversikStatus for personident") {
                it("updates trenger_oppfolging from kafka record with active huskelapp and frist") {
                    val activeHuskelappWithFrist = generateKafkaHuskelapp(isActive = true, frist = frist)
                    val personident = UserConstants.ARBEIDSTAKER_FNR
                    database.createPersonOversiktStatus(
                        personOversiktStatus = PersonOversiktStatus(fnr = personident)
                    )
                    mockIncomingKafkaRecord(
                        kafkaRecord = activeHuskelappWithFrist,
                        kafkaConsumerMock = kafkaConsumerMock,
                    )

                    runBlocking {
                        trengerOppfolgingConsumer.pollAndProcessRecords(kafkaConsumer = kafkaConsumerMock)
                    }

                    verify(exactly = 1) {
                        kafkaConsumerMock.commitSync()
                    }
                    val pPersonOversiktStatusList =
                        database.connection.use { it.getPersonOversiktStatusList(personident) }
                    pPersonOversiktStatusList.size shouldBeEqualTo 1
                    val pPersonOversiktStatus = pPersonOversiktStatusList.first()
                    pPersonOversiktStatus.fnr shouldBeEqualTo activeHuskelappWithFrist.personIdent
                    pPersonOversiktStatus.trengerOppfolging.shouldBeTrue()
                }
                it("updates to trenger_oppfolging false from kafka record with inactive huskelapp and frist") {
                    val inactiveHuskelappWithFrist = generateKafkaHuskelapp(isActive = false, frist = frist)
                    val personident = UserConstants.ARBEIDSTAKER_FNR
                    database.createPersonOversiktStatus(
                        personOversiktStatus = PersonOversiktStatus(
                            fnr = personident,
                        ).copy(
                            trengerOppfolging = true,
                        )
                    )
                    mockIncomingKafkaRecord(
                        kafkaRecord = inactiveHuskelappWithFrist,
                        kafkaConsumerMock = kafkaConsumerMock,
                    )

                    runBlocking {
                        trengerOppfolgingConsumer.pollAndProcessRecords(kafkaConsumer = kafkaConsumerMock)
                    }

                    verify(exactly = 1) {
                        kafkaConsumerMock.commitSync()
                    }
                    val pPersonOversiktStatusList =
                        database.connection.use { it.getPersonOversiktStatusList(personident) }
                    pPersonOversiktStatusList.size shouldBeEqualTo 1
                    val pPersonOversiktStatus = pPersonOversiktStatusList.first()
                    pPersonOversiktStatus.fnr shouldBeEqualTo inactiveHuskelappWithFrist.personIdent
                    pPersonOversiktStatus.trengerOppfolging.shouldBeFalse()
                }
                it("updates to trenger_oppfolging false from kafka record with inactive huskelapp and no frist") {
                    val inactiveHuskelappNoFrist = generateKafkaHuskelapp(isActive = false, frist = null)
                    val personident = UserConstants.ARBEIDSTAKER_FNR
                    database.createPersonOversiktStatus(
                        personOversiktStatus = PersonOversiktStatus(
                            fnr = personident,
                        ).copy(
                            trengerOppfolging = true,
                        )
                    )
                    mockIncomingKafkaRecord(
                        kafkaRecord = inactiveHuskelappNoFrist,
                        kafkaConsumerMock = kafkaConsumerMock,
                    )

                    runBlocking {
                        trengerOppfolgingConsumer.pollAndProcessRecords(kafkaConsumer = kafkaConsumerMock)
                    }

                    verify(exactly = 1) {
                        kafkaConsumerMock.commitSync()
                    }
                    val pPersonOversiktStatusList =
                        database.connection.use { it.getPersonOversiktStatusList(personident) }
                    pPersonOversiktStatusList.size shouldBeEqualTo 1
                    val pPersonOversiktStatus = pPersonOversiktStatusList.first()
                    pPersonOversiktStatus.fnr shouldBeEqualTo inactiveHuskelappNoFrist.personIdent
                    pPersonOversiktStatus.trengerOppfolging.shouldBeFalse()
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
