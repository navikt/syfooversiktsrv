package no.nav.syfo.personoppgavehendelse.kafka

import io.ktor.util.*
import io.mockk.every
import no.nav.syfo.personstatus.domain.*
import no.nav.syfo.personstatus.getPersonOversiktStatusList
import no.nav.syfo.testutil.*
import org.amshove.kluent.*
import org.apache.kafka.clients.consumer.*
import org.apache.kafka.common.TopicPartition
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.Duration

@InternalAPI
object PersonoppgavehendelseServiceSpek : Spek({

    describe("Add personoppgavehendelser in syfooversikt") {

        val database = TestDatabase()
        val personoppgavehendelseService = PersonoppgavehendelseService(database)
        val mockPersonoppgavehendelse = TestKafkaModule.kafkaPersonoppgavehendelse
        val lpsbistandMottatt = KPersonoppgavehendelse(
            personident = UserConstants.ARBEIDSTAKER_FNR,
            hendelsetype = OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_MOTTATT.name,
        )
        val lpsbistandBehandlet = KPersonoppgavehendelse(
            personident = UserConstants.ARBEIDSTAKER_FNR,
            hendelsetype = OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_BEHANDLET.name,
        )
        val lpsbistandUnknownHendelsetype = KPersonoppgavehendelse(
            personident = UserConstants.ARBEIDSTAKER_FNR,
            hendelsetype = "UNKNOWN_TYPE",
        )

        beforeEachTest {
            every { mockPersonoppgavehendelse.commitSync() } returns Unit
        }
        afterEachTest {
            database.connection.dropData()
        }

        it("Create personoversiktstatus on read from topic personoppgavehendelser") {
            mockReceiveLpsbistandHendelse(lpsbistandMottatt, mockPersonoppgavehendelse)

            personoppgavehendelseService.pollAndProcessRecords(kafkaConsumer = mockPersonoppgavehendelse)

            val personoversiktStatuser = database.getPersonOversiktStatusList(UserConstants.ARBEIDSTAKER_FNR)
            val firstStatus = personoversiktStatuser.first()
            val isUbehandlet = firstStatus.oppfolgingsplanLPSBistandUbehandlet!!
            isUbehandlet.shouldBeTrue()
        }

        it("Update personoversiktstatus on read from topic personoppgavehendelser") {
            mockReceiveLpsbistandHendelse(lpsbistandBehandlet, mockPersonoppgavehendelse)
            val personOversiktStatus = PersonOversiktStatus(UserConstants.ARBEIDSTAKER_FNR)
                .applyHendelse(OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_MOTTATT)
            database.createPersonOversiktStatus(personOversiktStatus)

            personoppgavehendelseService.pollAndProcessRecords(kafkaConsumer = mockPersonoppgavehendelse)

            val personoversiktStatuser = database.getPersonOversiktStatusList(UserConstants.ARBEIDSTAKER_FNR)
            val firstStatus = personoversiktStatuser.first()
            val isUbehandlet = firstStatus.oppfolgingsplanLPSBistandUbehandlet!!
            isUbehandlet.shouldBeFalse()
        }

        it("Ignore records with unknown hendelsetype") {
            mockReceiveLpsbistandHendelse(lpsbistandUnknownHendelsetype, mockPersonoppgavehendelse)

            personoppgavehendelseService.pollAndProcessRecords(kafkaConsumer = mockPersonoppgavehendelse)

            val personoversiktStatuser = database.getPersonOversiktStatusList(UserConstants.ARBEIDSTAKER_FNR)
            personoversiktStatuser.size shouldBeEqualTo 0
        }
    }
})

fun mockReceiveLpsbistandHendelse(lpsBistandHendelse: KPersonoppgavehendelse, mockKafkaPersonoppgavehendelse: KafkaConsumer<String, KPersonoppgavehendelse>) {
    every { mockKafkaPersonoppgavehendelse.poll(any<Duration>()) } returns ConsumerRecords(
        mapOf(
            personoppgavehendelseTopicPartition() to listOf(
                personoppgavehendelseRecord(lpsBistandHendelse),
            )
        )
    )
}

fun personoppgavehendelseTopicPartition() = TopicPartition(
    PERSONOPPGAVEHENDELSE_TOPIC,
    0
)

fun personoppgavehendelseRecord(
    kPersonoppgavehendelse: KPersonoppgavehendelse,
) = ConsumerRecord(
    PERSONOPPGAVEHENDELSE_TOPIC,
    0,
    1,
    "key1",
    kPersonoppgavehendelse
)
