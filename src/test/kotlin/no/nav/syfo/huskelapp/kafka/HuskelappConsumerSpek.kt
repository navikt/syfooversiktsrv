package no.nav.syfo.huskelapp.kafka

import io.ktor.server.testing.*
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.syfo.huskelapp.HuskelappService
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
import org.amshove.kluent.shouldBeTrue
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.Duration

class HuskelappConsumerSpek : Spek({
    with(TestApplicationEngine()) {
        start()

        val externalMockEnvironment = ExternalMockEnvironment.instance
        val database = externalMockEnvironment.database

        val kafkaConsumerMock = mockk<KafkaConsumer<String, KafkaHuskelapp>>()
        val huskelappService = HuskelappService(database)
        val huskelappConsumer = HuskelappConsumer(huskelappService)

        val kafkaHuskelapp = generateKafkaHuskelapp()

        beforeEachTest {
            database.connection.dropData()

            clearMocks(kafkaConsumerMock)
            every { kafkaConsumerMock.commitSync() } returns Unit
        }

        describe("${HuskelappConsumer::class.java.simpleName}: pollAndProcessRecords") {
            it("creates new PersonOversiktStatus with huskelappActive true if no PersonOversiktStatus exists for personident") {
                mockIncomingKafkaRecord(
                    kafkaRecord = kafkaHuskelapp,
                    kafkaConsumerMock = kafkaConsumerMock,
                )

                huskelappConsumer.pollAndProcessRecords(
                    kafkaConsumer = kafkaConsumerMock,
                )

                verify(exactly = 1) {
                    kafkaConsumerMock.commitSync()
                }

                val pPersonOversiktStatusList =
                    database.connection.use { it.getPersonOversiktStatusList(UserConstants.ARBEIDSTAKER_FNR) }
                pPersonOversiktStatusList.size shouldBeEqualTo 1
                val pPersonOversiktStatus = pPersonOversiktStatusList.first()
                pPersonOversiktStatus.fnr shouldBeEqualTo kafkaHuskelapp.personIdent
                pPersonOversiktStatus.huskelappActive.shouldBeTrue()
            }

            it("updates existing PersonOversikStatus with huskelappActive true when PersonOversiktStatus exists for personident") {
                val personident = UserConstants.ARBEIDSTAKER_FNR
                database.createPersonOversiktStatus(
                    personOversiktStatus = PersonOversiktStatus(fnr = personident)
                )
                mockIncomingKafkaRecord(
                    kafkaRecord = kafkaHuskelapp,
                    kafkaConsumerMock = kafkaConsumerMock,
                )

                huskelappConsumer.pollAndProcessRecords(
                    kafkaConsumer = kafkaConsumerMock,
                )

                verify(exactly = 1) {
                    kafkaConsumerMock.commitSync()
                }
                val pPersonOversiktStatusList =
                    database.connection.use { it.getPersonOversiktStatusList(personident) }
                pPersonOversiktStatusList.size shouldBeEqualTo 1
                val pPersonOversiktStatus = pPersonOversiktStatusList.first()
                pPersonOversiktStatus.fnr shouldBeEqualTo kafkaHuskelapp.personIdent
                pPersonOversiktStatus.huskelappActive.shouldBeTrue()
            }

            it("updates existing PersonOversikStatus with huskelappActive false when PersonOversiktStatus exists for personident") {
                val personident = UserConstants.ARBEIDSTAKER_FNR
                database.createPersonOversiktStatus(
                    personOversiktStatus = PersonOversiktStatus(
                        fnr = personident,
                    ).copy(
                        huskelappActive = true,
                    )
                )
                mockIncomingKafkaRecord(
                    kafkaRecord = kafkaHuskelapp.copy(isActive = false),
                    kafkaConsumerMock = kafkaConsumerMock,
                )

                huskelappConsumer.pollAndProcessRecords(
                    kafkaConsumer = kafkaConsumerMock,
                )

                verify(exactly = 1) {
                    kafkaConsumerMock.commitSync()
                }
                val pPersonOversiktStatusList =
                    database.connection.use { it.getPersonOversiktStatusList(personident) }
                pPersonOversiktStatusList.size shouldBeEqualTo 1
                val pPersonOversiktStatus = pPersonOversiktStatusList.first()
                pPersonOversiktStatus.fnr shouldBeEqualTo kafkaHuskelapp.personIdent
                pPersonOversiktStatus.huskelappActive.shouldBeFalse()
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
