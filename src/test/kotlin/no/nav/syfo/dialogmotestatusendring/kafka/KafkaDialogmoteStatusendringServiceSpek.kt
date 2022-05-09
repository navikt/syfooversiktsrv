package no.nav.syfo.dialogmotestatusendring.kafka

import io.ktor.server.testing.*
import io.mockk.*
import no.nav.syfo.dialogmotestatusendring.domain.DialogmoteStatusendringType
import no.nav.syfo.testutil.*
import no.nav.syfo.testutil.generator.*
import no.nav.syfo.util.nowUTC
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
        val kafkaDialogmoteStatusendring = generateKafkaDialogmoteStatusendring(
            personIdent = UserConstants.ARBEIDSTAKER_FNR,
            statusEndringType = DialogmoteStatusendringType.INNKALT,
            endringsTidspunkt = nowUTC(),
        )
        val kafkaDialogmoteStatusendringConsumerRecord = dialogmoteStatusendringConsumerRecord(
            kafkaDialogmoteStatusendring = kafkaDialogmoteStatusendring,
        )

        beforeEachTest {
            database.connection.dropData()

            clearMocks(mockKafkaConsumerDialogmoteStatusendring)
            every { mockKafkaConsumerDialogmoteStatusendring.commitSync() } returns Unit
        }

        describe("${KafkaDialogmoteStatusendringService::class.java.simpleName}: pollAndProcessRecords") {
            it("process records") {
                every { mockKafkaConsumerDialogmoteStatusendring.poll(any<Duration>()) } returns ConsumerRecords(
                    mapOf(
                        dialogmoteStatusendringTopicPartition to listOf(
                            kafkaDialogmoteStatusendringConsumerRecord,
                        )
                    )
                )

                kafkaDialogmoteStatusendringService.pollAndProcessRecords(
                    kafkaConsumer = mockKafkaConsumerDialogmoteStatusendring
                )

                verify(exactly = 1) {
                    mockKafkaConsumerDialogmoteStatusendring.commitSync()
                }
            }
        }
    }
})
