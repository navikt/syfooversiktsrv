package no.nav.syfo.personoppgavehendelse.kafka

import io.ktor.util.*
import io.mockk.every
import no.nav.syfo.personstatus.domain.*
import no.nav.syfo.personstatus.db.*
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

        val internalMockEnvironment = InternalMockEnvironment.instance
        val database = ExternalMockEnvironment.instance.database
        val personoversiktStatusService = internalMockEnvironment.personoversiktStatusService
        val personoppgavehendelseConsumer = PersonoppgavehendelseConsumer(personoversiktStatusService)
        val mockPersonoppgavehendelseConsumer = TestKafkaModule.kafkaPersonoppgavehendelse
        val lpsbistandMottatt = KPersonoppgavehendelse(
            personident = UserConstants.ARBEIDSTAKER_FNR,
            hendelsetype = OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_MOTTATT,
        )
        val lpsbistandBehandlet = KPersonoppgavehendelse(
            personident = UserConstants.ARBEIDSTAKER_FNR,
            hendelsetype = OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_BEHANDLET,
        )

        beforeEachTest {
            every { mockPersonoppgavehendelseConsumer.commitSync() } returns Unit
        }
        afterEachTest {
            database.connection.dropData()
        }

        it("Create personoversiktstatus on read from topic personoppgavehendelser") {
            mockReceiveHendelse(lpsbistandMottatt, mockPersonoppgavehendelseConsumer)

            personoppgavehendelseConsumer.pollAndProcessRecords(kafkaConsumer = mockPersonoppgavehendelseConsumer)

            val personoversiktStatuser = database.getPersonOversiktStatusList(UserConstants.ARBEIDSTAKER_FNR)
            val firstStatus = personoversiktStatuser.first()
            val isUbehandlet = firstStatus.oppfolgingsplanLPSBistandUbehandlet!!
            isUbehandlet.shouldBeTrue()
        }

        it("Update personoversiktstatus on read from topic personoppgavehendelser") {
            mockReceiveHendelse(lpsbistandBehandlet, mockPersonoppgavehendelseConsumer)
            val personOversiktStatus = PersonOversiktStatus(UserConstants.ARBEIDSTAKER_FNR)
                .applyHendelse(OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_MOTTATT)
            database.createPersonOversiktStatus(personOversiktStatus)

            personoppgavehendelseConsumer.pollAndProcessRecords(kafkaConsumer = mockPersonoppgavehendelseConsumer)

            val personoversiktStatuser = database.getPersonOversiktStatusList(UserConstants.ARBEIDSTAKER_FNR)
            val firstStatus = personoversiktStatuser.first()
            val isUbehandlet = firstStatus.oppfolgingsplanLPSBistandUbehandlet!!
            isUbehandlet.shouldBeFalse()
        }

        it("Update personoversiktstatus with dialogmotesvar") {
            val dialogmotesvarBehandlet = KPersonoppgavehendelse(
                personident = UserConstants.ARBEIDSTAKER_FNR,
                hendelsetype = OversikthendelseType.DIALOGMOTESVAR_BEHANDLET,
            )
            mockReceiveHendelse(dialogmotesvarBehandlet, mockPersonoppgavehendelseConsumer)
            val personOversiktStatus = PersonOversiktStatus(UserConstants.ARBEIDSTAKER_FNR)
                .applyHendelse(OversikthendelseType.DIALOGMOTESVAR_MOTTATT)
            database.createPersonOversiktStatus(personOversiktStatus)

            personoppgavehendelseConsumer.pollAndProcessRecords(kafkaConsumer = mockPersonoppgavehendelseConsumer)

            val personoversiktStatuser = database.getPersonOversiktStatusList(UserConstants.ARBEIDSTAKER_FNR)
            val firstStatus = personoversiktStatuser.first()
            val isUbehandlet = firstStatus.dialogmotesvarUbehandlet
            isUbehandlet.shouldBeFalse()
        }

        it("Create personoversiktstatus from behandlerdialog svar mottatt") {
            val behandlerdialogSvarMottatt = KPersonoppgavehendelse(
                personident = UserConstants.ARBEIDSTAKER_FNR,
                hendelsetype = OversikthendelseType.BEHANDLERDIALOG_SVAR_MOTTATT,
            )
            mockReceiveHendelse(behandlerdialogSvarMottatt, mockPersonoppgavehendelseConsumer)

            personoppgavehendelseConsumer.pollAndProcessRecords(kafkaConsumer = mockPersonoppgavehendelseConsumer)

            val personoversiktStatuser = database.getPersonOversiktStatusList(UserConstants.ARBEIDSTAKER_FNR)
            val firstStatus = personoversiktStatuser.first()
            val isUbehandlet = firstStatus.behandlerdialogSvarUbehandlet
            isUbehandlet.shouldBeTrue()
        }

        it("Update personoversiktstatus from behandlerdialog svar behandlet") {
            val behandlerdialogSvarBehandlet = KPersonoppgavehendelse(
                personident = UserConstants.ARBEIDSTAKER_FNR,
                hendelsetype = OversikthendelseType.BEHANDLERDIALOG_SVAR_BEHANDLET,
            )
            mockReceiveHendelse(behandlerdialogSvarBehandlet, mockPersonoppgavehendelseConsumer)
            val personOversiktStatus = PersonOversiktStatus(UserConstants.ARBEIDSTAKER_FNR)
                .applyHendelse(OversikthendelseType.BEHANDLERDIALOG_SVAR_MOTTATT)
            database.createPersonOversiktStatus(personOversiktStatus)

            personoppgavehendelseConsumer.pollAndProcessRecords(kafkaConsumer = mockPersonoppgavehendelseConsumer)

            val personoversiktStatuser = database.getPersonOversiktStatusList(UserConstants.ARBEIDSTAKER_FNR)
            val firstStatus = personoversiktStatuser.first()
            val isUbehandlet = firstStatus.behandlerdialogSvarUbehandlet
            isUbehandlet.shouldBeFalse()
        }

        it("Create personoversiktstatus from behandlerdialog ubesvart mottatt") {
            val behandlerdialogUbesvartMottatt = KPersonoppgavehendelse(
                personident = UserConstants.ARBEIDSTAKER_FNR,
                hendelsetype = OversikthendelseType.BEHANDLERDIALOG_MELDING_UBESVART_MOTTATT,
            )
            mockReceiveHendelse(behandlerdialogUbesvartMottatt, mockPersonoppgavehendelseConsumer)

            personoppgavehendelseConsumer.pollAndProcessRecords(kafkaConsumer = mockPersonoppgavehendelseConsumer)

            val personoversiktStatuser = database.getPersonOversiktStatusList(UserConstants.ARBEIDSTAKER_FNR)
            val firstStatus = personoversiktStatuser.first()
            val isUbehandlet = firstStatus.behandlerdialogUbesvartUbehandlet
            isUbehandlet.shouldBeTrue()
        }

        it("Update personoversiktstatus from behandlerdialog ubesvart behandlet") {
            val behandlerdialogUbesvartBehandlet = KPersonoppgavehendelse(
                personident = UserConstants.ARBEIDSTAKER_FNR,
                hendelsetype = OversikthendelseType.BEHANDLERDIALOG_MELDING_UBESVART_BEHANDLET,
            )
            mockReceiveHendelse(behandlerdialogUbesvartBehandlet, mockPersonoppgavehendelseConsumer)
            val personOversiktStatus = PersonOversiktStatus(UserConstants.ARBEIDSTAKER_FNR)
                .applyHendelse(OversikthendelseType.BEHANDLERDIALOG_MELDING_UBESVART_MOTTATT)
            database.createPersonOversiktStatus(personOversiktStatus)

            personoppgavehendelseConsumer.pollAndProcessRecords(kafkaConsumer = mockPersonoppgavehendelseConsumer)

            val personoversiktStatuser = database.getPersonOversiktStatusList(UserConstants.ARBEIDSTAKER_FNR)
            val firstStatus = personoversiktStatuser.first()
            val isUbehandlet = firstStatus.behandlerdialogUbesvartUbehandlet
            isUbehandlet.shouldBeFalse()
        }

        it("Create personoversiktstatus from behandlerdialog avvist mottatt hendelse") {
            val behandlerdialogAvvistMottatt = KPersonoppgavehendelse(
                personident = UserConstants.ARBEIDSTAKER_FNR,
                hendelsetype = OversikthendelseType.BEHANDLERDIALOG_MELDING_AVVIST_MOTTATT,
            )
            mockReceiveHendelse(behandlerdialogAvvistMottatt, mockPersonoppgavehendelseConsumer)

            personoppgavehendelseConsumer.pollAndProcessRecords(kafkaConsumer = mockPersonoppgavehendelseConsumer)

            val personoversiktStatuser = database.getPersonOversiktStatusList(UserConstants.ARBEIDSTAKER_FNR)
            val firstStatus = personoversiktStatuser.first()
            firstStatus.behandlerdialogAvvistUbehandlet.shouldBeTrue()
        }

        it("Update personoversiktstatus from behandlerdialog avvist behandlet hendelse") {
            val behandlerdialogAvvistBehandlet = KPersonoppgavehendelse(
                personident = UserConstants.ARBEIDSTAKER_FNR,
                hendelsetype = OversikthendelseType.BEHANDLERDIALOG_MELDING_AVVIST_BEHANDLET,
            )
            mockReceiveHendelse(behandlerdialogAvvistBehandlet, mockPersonoppgavehendelseConsumer)
            val personOversiktStatus = PersonOversiktStatus(UserConstants.ARBEIDSTAKER_FNR)
                .applyHendelse(OversikthendelseType.BEHANDLERDIALOG_MELDING_AVVIST_MOTTATT)
            database.createPersonOversiktStatus(personOversiktStatus)

            personoppgavehendelseConsumer.pollAndProcessRecords(kafkaConsumer = mockPersonoppgavehendelseConsumer)

            val personoversiktStatuser = database.getPersonOversiktStatusList(UserConstants.ARBEIDSTAKER_FNR)
            val firstStatus = personoversiktStatuser.first()
            firstStatus.behandlerdialogAvvistUbehandlet.shouldBeFalse()
        }

        it("Update personoversiktstatus from aktivitetskrav varsel mottatt hendelse") {
            val aktivitetskravVarselHendelseMottatt = KPersonoppgavehendelse(
                personident = UserConstants.ARBEIDSTAKER_FNR,
                hendelsetype = OversikthendelseType.AKTIVITETSKRAV_VURDER_STANS_MOTTATT,
            )
            mockReceiveHendelse(aktivitetskravVarselHendelseMottatt, mockPersonoppgavehendelseConsumer)

            personoppgavehendelseConsumer.pollAndProcessRecords(kafkaConsumer = mockPersonoppgavehendelseConsumer)

            val personoversiktStatuser = database.getPersonOversiktStatusList(UserConstants.ARBEIDSTAKER_FNR)
            val firstStatus = personoversiktStatuser.first()
            firstStatus.aktivitetskravVurderStansUbehandlet.shouldBeTrue()
        }

        it("Update personoversiktstatus from aktivitetskrav varsel behandlet hendelse") {
            val aktivitetskravVarselHendelseBehandlet = KPersonoppgavehendelse(
                personident = UserConstants.ARBEIDSTAKER_FNR,
                hendelsetype = OversikthendelseType.AKTIVITETSKRAV_VURDER_STANS_BEHANDLET,
            )
            mockReceiveHendelse(aktivitetskravVarselHendelseBehandlet, mockPersonoppgavehendelseConsumer)
            val personOversiktStatus = PersonOversiktStatus(UserConstants.ARBEIDSTAKER_FNR)
                .applyHendelse(OversikthendelseType.AKTIVITETSKRAV_VURDER_STANS_MOTTATT)
            database.createPersonOversiktStatus(personOversiktStatus)

            personoppgavehendelseConsumer.pollAndProcessRecords(kafkaConsumer = mockPersonoppgavehendelseConsumer)

            val personoversiktStatuser = database.getPersonOversiktStatusList(UserConstants.ARBEIDSTAKER_FNR)
            val firstStatus = personoversiktStatuser.first()
            firstStatus.aktivitetskravVurderStansUbehandlet.shouldBeFalse()
        }

        it("Update personoversiktstatus from huskelapp mottatt hendelse") {
            val huskelappMottatt = KPersonoppgavehendelse(
                personident = UserConstants.ARBEIDSTAKER_FNR,
                hendelsetype = OversikthendelseType.HUSKELAPP_MOTTATT,
            )
            mockReceiveHendelse(huskelappMottatt, mockPersonoppgavehendelseConsumer)

            personoppgavehendelseConsumer.pollAndProcessRecords(kafkaConsumer = mockPersonoppgavehendelseConsumer)

            val personoversiktStatuser = database.getPersonOversiktStatusList(UserConstants.ARBEIDSTAKER_FNR)
            val firstStatus = personoversiktStatuser.first()
            firstStatus.huskelappActive.shouldBeTrue()
        }

        it("Update personoversiktstatus from huskelapp behandlet hendelse") {
            val personOversiktStatus = PersonOversiktStatus(UserConstants.ARBEIDSTAKER_FNR)
                .applyHendelse(OversikthendelseType.HUSKELAPP_MOTTATT)
            database.createPersonOversiktStatus(personOversiktStatus)

            val huskelappBehandlet = KPersonoppgavehendelse(
                personident = UserConstants.ARBEIDSTAKER_FNR,
                hendelsetype = OversikthendelseType.HUSKELAPP_BEHANDLET,
            )
            mockReceiveHendelse(huskelappBehandlet, mockPersonoppgavehendelseConsumer)
            personoppgavehendelseConsumer.pollAndProcessRecords(kafkaConsumer = mockPersonoppgavehendelseConsumer)

            val personoversiktStatuser = database.getPersonOversiktStatusList(UserConstants.ARBEIDSTAKER_FNR)
            val firstStatus = personoversiktStatuser.first()
            firstStatus.huskelappActive.shouldBeFalse()
        }
    }
})

fun mockReceiveHendelse(hendelse: KPersonoppgavehendelse, mockKafkaPersonoppgavehendelse: KafkaConsumer<String, KPersonoppgavehendelse>) {
    every { mockKafkaPersonoppgavehendelse.poll(any<Duration>()) } returns ConsumerRecords(
        mapOf(
            personoppgavehendelseTopicPartition() to listOf(
                personoppgavehendelseRecord(hendelse),
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
