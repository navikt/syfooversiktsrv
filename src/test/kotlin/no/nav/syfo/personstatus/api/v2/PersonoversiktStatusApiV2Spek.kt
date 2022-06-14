package no.nav.syfo.personstatus.api.v2

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.http.*
import io.ktor.server.testing.*
import io.ktor.util.*
import io.mockk.clearMocks
import io.mockk.every
import kotlinx.coroutines.runBlocking
import no.nav.syfo.dialogmotestatusendring.domain.DialogmoteStatusendringType
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.domain.Virksomhetsnummer
import no.nav.syfo.personstatus.*
import no.nav.syfo.personstatus.domain.*
import no.nav.syfo.testutil.*
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_NO_NAME_FNR
import no.nav.syfo.testutil.UserConstants.NAV_ENHET
import no.nav.syfo.testutil.UserConstants.VEILEDER_ID
import no.nav.syfo.testutil.UserConstants.VIRKSOMHETSNUMMER_2
import no.nav.syfo.testutil.assertion.checkPersonOppfolgingstilfelleDTO
import no.nav.syfo.testutil.generator.*
import no.nav.syfo.testutil.mock.behandlendeEnhetDTO
import no.nav.syfo.util.*
import org.amshove.kluent.*
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.Duration
import java.time.LocalDateTime

@InternalAPI
object PersonoversiktStatusApiV2Spek : Spek({
    val objectMapper: ObjectMapper = configuredJacksonMapper()

    describe("PersonoversiktApi") {

        with(TestApplicationEngine()) {
            start()

            val externalMockEnvironment = ExternalMockEnvironment.instance
            val database = externalMockEnvironment.database

            application.testApiModule(
                externalMockEnvironment = externalMockEnvironment
            )

            val internalMockEnvironment = InternalMockEnvironment.instance

            val personBehandlendeEnhetCronjob = internalMockEnvironment.personBehandlendeEnhetCronjob

            val personOppfolgingstilfelleVirksomhetnavnCronjob =
                internalMockEnvironment.personOppfolgingstilfelleVirksomhetnavnCronjob

            val oversiktHendelseService = TestKafkaModule.kafkaOversiktHendelseService

            val kafkaOppfolgingstilfellePersonService = TestKafkaModule.kafkaOppfolgingstilfellePersonService

            val personIdentDefault = PersonIdent(ARBEIDSTAKER_FNR)

            val mockKafkaConsumerOppfolgingstilfellePerson =
                TestKafkaModule.kafkaConsumerOppfolgingstilfellePerson

            val oppfolgingstilfellePersonTopicPartition = oppfolgingstilfellePersonTopicPartition()
            val kafkaOppfolgingstilfellePersonRelevant = generateKafkaOppfolgingstilfellePerson(
                personIdent = personIdentDefault,
                virksomhetsnummerList = listOf(
                    UserConstants.VIRKSOMHETSNUMMER_DEFAULT,
                    Virksomhetsnummer(VIRKSOMHETSNUMMER_2),
                )
            )
            val kafkaOppfolgingstilfellePersonRecordRelevant = oppfolgingstilfellePersonConsumerRecord(
                kafkaOppfolgingstilfellePerson = kafkaOppfolgingstilfellePersonRelevant,
            )

            val kafkaDialogmotekandidatEndringService = TestKafkaModule.kafkaDialogmotekandidatEndringService
            val mockKafkaConsumerDialogmotekandidatEndring =
                TestKafkaModule.kafkaConsumerDialogmotekandidatEndring
            val dialogmoteKandidatTopicPartition = dialogmotekandidatEndringTopicPartition()
            val kafkaDialogmotekandidatEndringStoppunktDelayPassed = generateKafkaDialogmotekandidatEndringStoppunkt(
                personIdent = ARBEIDSTAKER_FNR,
                createdAt = nowUTC().minusDays(7)
            )
            val kafkaDialogmotekandidatEndringStoppunktConsumerDelayPassedRecord = dialogmotekandidatEndringConsumerRecord(
                kafkaDialogmotekandidatEndring = kafkaDialogmotekandidatEndringStoppunktDelayPassed,
            )

            val kafkaDialogmoteStatusendringService = TestKafkaModule.kafkaDialogmoteStatusendringService
            val mockKafkaConsumerDialogmoteStatusendring = TestKafkaModule.kafkaConsumerDialogmoteStatusendring
            val dialogmoteStatusendringTopicPartition = dialogmoteStatusendringTopicPartition()
            val kafkaDialogmoteStatusendring = generateKafkaDialogmoteStatusendring(
                personIdent = ARBEIDSTAKER_FNR,
                type = DialogmoteStatusendringType.INNKALT,
                endringsTidspunkt = nowUTC()
            )
            val kafkaDialogmoteStatusendringConsumerRecord = dialogmoteStatusendringConsumerRecord(
                kafkaDialogmoteStatusendring = kafkaDialogmoteStatusendring
            )

            beforeEachTest {
                database.connection.dropData()

                clearMocks(mockKafkaConsumerOppfolgingstilfellePerson)
                every { mockKafkaConsumerOppfolgingstilfellePerson.commitSync() } returns Unit
                every { mockKafkaConsumerOppfolgingstilfellePerson.poll(any<Duration>()) } returns ConsumerRecords(
                    mapOf(
                        oppfolgingstilfellePersonTopicPartition to listOf(
                            kafkaOppfolgingstilfellePersonRecordRelevant,
                        )
                    )
                )
                clearMocks(mockKafkaConsumerDialogmotekandidatEndring)
                every { mockKafkaConsumerDialogmotekandidatEndring.commitSync() } returns Unit
                every { mockKafkaConsumerDialogmotekandidatEndring.poll(any<Duration>()) } returns ConsumerRecords(
                    mapOf(
                        dialogmoteKandidatTopicPartition to listOf(
                            kafkaDialogmotekandidatEndringStoppunktConsumerDelayPassedRecord,
                        )
                    )
                )
                clearMocks(mockKafkaConsumerDialogmoteStatusendring)
                every { mockKafkaConsumerDialogmoteStatusendring.commitSync() } returns Unit
                every { mockKafkaConsumerDialogmoteStatusendring.poll(any<Duration>()) } returns ConsumerRecords(
                    mapOf(
                        dialogmoteStatusendringTopicPartition to listOf(
                            kafkaDialogmoteStatusendringConsumerRecord,
                        )
                    )
                )
            }

            val validToken = generateJWT(
                audience = externalMockEnvironment.environment.azure.appClientId,
                issuer = externalMockEnvironment.wellKnownVeilederV2.issuer,
                navIdent = VEILEDER_ID,
            )

            describe("Hent personoversikt for enhet") {
                val url = "$personOversiktApiV2Path/enhet/$NAV_ENHET"

                it("skal returnere status NoContent om det ikke er noen personer som er tilknyttet enhet") {
                    with(
                        handleRequest(HttpMethod.Get, url) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.NoContent
                    }
                }

                it("skal returnere NoContent med ubehandlet motebehovsvar og ikke har oppfolgingstilfelle") {
                    val oversiktHendelse = KOversikthendelse(
                        ARBEIDSTAKER_FNR,
                        OversikthendelseType.MOTEBEHOV_SVAR_MOTTATT.name,
                        NAV_ENHET,
                        LocalDateTime.now()
                    )
                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelse)

                    runBlocking {
                        personBehandlendeEnhetCronjob.runJob()
                    }

                    val tilknytning = VeilederBrukerKnytning(VEILEDER_ID, ARBEIDSTAKER_FNR, NAV_ENHET)
                    database.lagreBrukerKnytningPaEnhet(tilknytning)

                    with(
                        handleRequest(HttpMethod.Get, url) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.NoContent
                    }
                }

                it("skal returnere NoContent med ubehandlet moteplanleggersvar og ikke har oppfolgingstilfelle") {
                    val oversiktHendelse = KOversikthendelse(
                        ARBEIDSTAKER_FNR,
                        OversikthendelseType.MOTEPLANLEGGER_ALLE_SVAR_MOTTATT.name,
                        NAV_ENHET,
                        LocalDateTime.now()
                    )
                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelse)

                    runBlocking {
                        personBehandlendeEnhetCronjob.runJob()
                    }

                    val tilknytning = VeilederBrukerKnytning(VEILEDER_ID, ARBEIDSTAKER_FNR, NAV_ENHET)
                    database.lagreBrukerKnytningPaEnhet(tilknytning)

                    with(
                        handleRequest(HttpMethod.Get, url) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.NoContent
                    }
                }

                it("skal returnere NoContent, om alle personer i personoversikt er behandlet og ikke har oppfolgingstilfelle") {
                    val oversiktHendelse = KOversikthendelse(
                        ARBEIDSTAKER_FNR,
                        OversikthendelseType.MOTEBEHOV_SVAR_MOTTATT.name,
                        NAV_ENHET,
                        LocalDateTime.now()
                    )
                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelse)

                    val oversiktHendelseNy = KOversikthendelse(
                        ARBEIDSTAKER_FNR,
                        OversikthendelseType.MOTEBEHOV_SVAR_BEHANDLET.name,
                        NAV_ENHET,
                        LocalDateTime.now()
                    )
                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelseNy)

                    val oversiktHendelseMoteplanleggerMottatt = KOversikthendelse(
                        ARBEIDSTAKER_FNR,
                        OversikthendelseType.MOTEPLANLEGGER_ALLE_SVAR_MOTTATT.name,
                        NAV_ENHET,
                        LocalDateTime.now()
                    )
                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelseMoteplanleggerMottatt)

                    val oversiktHendelseMoteplanleggerBehandlet = KOversikthendelse(
                        ARBEIDSTAKER_FNR,
                        OversikthendelseType.MOTEPLANLEGGER_ALLE_SVAR_BEHANDLET.name,
                        NAV_ENHET,
                        LocalDateTime.now()
                    )
                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelseMoteplanleggerBehandlet)

                    runBlocking {
                        personBehandlendeEnhetCronjob.runJob()
                    }

                    val tilknytning = VeilederBrukerKnytning(VEILEDER_ID, ARBEIDSTAKER_FNR, NAV_ENHET)
                    database.lagreBrukerKnytningPaEnhet(tilknytning)

                    with(
                        handleRequest(HttpMethod.Get, url) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.NoContent
                    }
                }

                it("should return NoContent, if there is a person with a relevant active Oppfolgingstilfelle, but neither MOTEBEHOV_SVAR_MOTTATT nor MOTEPLANLEGGER_ALLE_SVAR_MOTTATT nor DIALOGMOTEKANDIDAT") {
                    kafkaOppfolgingstilfellePersonService.pollAndProcessRecords(
                        kafkaConsumer = mockKafkaConsumerOppfolgingstilfellePerson,
                    )
                    runBlocking {
                        personOppfolgingstilfelleVirksomhetnavnCronjob.runJob()
                    }
                    runBlocking {
                        personBehandlendeEnhetCronjob.runJob()
                    }

                    with(
                        handleRequest(HttpMethod.Get, url) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.NoContent
                    }
                }

                it("should return list of PersonOversiktStatus, if MOTEBEHOV_SVAR_MOTTATT and MOTEPLANLEGGER_ALLE_SVAR_MOTTATT and OPPFOLGINGSPLANLPS_BISTAND_MOTTATT and DIALOGMOTESVAR_MOTTATT, and there is a person with a relevant active Oppfolgingstilfelle") {
                    kafkaOppfolgingstilfellePersonService.pollAndProcessRecords(
                        kafkaConsumer = mockKafkaConsumerOppfolgingstilfellePerson,
                    )

                    val oversiktHendelseMotebehovMottatt = KOversikthendelse(
                        ARBEIDSTAKER_FNR,
                        OversikthendelseType.MOTEBEHOV_SVAR_MOTTATT.name,
                        NAV_ENHET,
                        LocalDateTime.now()
                    )
                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelseMotebehovMottatt)

                    val oversiktHendelseMoteplanleggerMottatt = KOversikthendelse(
                        ARBEIDSTAKER_FNR,
                        OversikthendelseType.MOTEPLANLEGGER_ALLE_SVAR_MOTTATT.name,
                        NAV_ENHET,
                        LocalDateTime.now()
                    )
                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelseMoteplanleggerMottatt)

                    val oversiktHendelseOPLPSBistandMottatt =
                        generateKOversikthendelse(OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_MOTTATT)
                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelseOPLPSBistandMottatt)

                    val dialogmotesvarMottatt = generateKOversikthendelse(OversikthendelseType.DIALOGMOTESVAR_MOTTATT)
                    oversiktHendelseService.oppdaterPersonMedHendelse(dialogmotesvarMottatt)

                    runBlocking {
                        personOppfolgingstilfelleVirksomhetnavnCronjob.runJob()
                    }

                    runBlocking {
                        personBehandlendeEnhetCronjob.runJob()
                    }

                    with(
                        handleRequest(HttpMethod.Get, url) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.OK
                        val personOversiktStatus =
                            objectMapper.readValue<List<PersonOversiktStatusDTO>>(response.content!!).first()
                        personOversiktStatus.veilederIdent shouldBeEqualTo null
                        personOversiktStatus.fnr shouldBeEqualTo oversiktHendelseMotebehovMottatt.fnr
                        personOversiktStatus.enhet shouldBeEqualTo behandlendeEnhetDTO().enhetId
                        personOversiktStatus.motebehovUbehandlet shouldBeEqualTo true
                        personOversiktStatus.moteplanleggerUbehandlet shouldBeEqualTo true
                        personOversiktStatus.oppfolgingsplanLPSBistandUbehandlet shouldBeEqualTo true
                        personOversiktStatus.dialogmotesvarUbehandlet shouldBeEqualTo true
                        personOversiktStatus.dialogmotekandidat.shouldBeNull()
                        personOversiktStatus.motestatus.shouldBeNull()

                        personOversiktStatus.latestOppfolgingstilfelle.shouldNotBeNull()
                        checkPersonOppfolgingstilfelleDTO(
                            personOppfolgingstilfelleDTO = personOversiktStatus.latestOppfolgingstilfelle,
                            kafkaOppfolgingstilfellePerson = kafkaOppfolgingstilfellePersonRelevant,
                        )
                    }
                }

                it("should return list of PersonOversiktStatus, if there is a person with a relevant active Oppfolgingstilfelle, and person is DIALOGMOTEKANDIDAT and delay of 7 days has passed") {
                    kafkaOppfolgingstilfellePersonService.pollAndProcessRecords(
                        kafkaConsumer = mockKafkaConsumerOppfolgingstilfellePerson,
                    )
                    kafkaDialogmotekandidatEndringService.pollAndProcessRecords(
                        kafkaConsumer = mockKafkaConsumerDialogmotekandidatEndring,
                    )

                    runBlocking {
                        personBehandlendeEnhetCronjob.runJob()
                    }

                    with(
                        handleRequest(HttpMethod.Get, url) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.OK
                        val personOversiktStatus =
                            objectMapper.readValue<List<PersonOversiktStatusDTO>>(response.content!!).first()
                        personOversiktStatus.shouldNotBeNull()
                        personOversiktStatus.veilederIdent shouldBeEqualTo null
                        personOversiktStatus.fnr shouldBeEqualTo kafkaDialogmotekandidatEndringStoppunktDelayPassed.personIdentNumber
                        personOversiktStatus.enhet shouldBeEqualTo behandlendeEnhetDTO().enhetId
                        personOversiktStatus.motebehovUbehandlet.shouldBeNull()
                        personOversiktStatus.moteplanleggerUbehandlet.shouldBeNull()
                        personOversiktStatus.oppfolgingsplanLPSBistandUbehandlet.shouldBeNull()
                        personOversiktStatus.dialogmotesvarUbehandlet shouldBeEqualTo false
                        personOversiktStatus.dialogmotekandidat shouldBeEqualTo true
                        personOversiktStatus.motestatus.shouldBeNull()
                    }
                }

                it("should not return list of PersonOversiktStatus, if there is a person with a relevant active Oppfolgingstilfelle, and person is DIALOGMOTEKANDIDAT and delay of 7 days has not passed") {
                    val kafkaDialogmotekandidatEndringStoppunktDelayNotPassed = generateKafkaDialogmotekandidatEndringStoppunkt(
                        personIdent = ARBEIDSTAKER_FNR,
                        createdAt = nowUTC().minusDays(6),
                    )
                    val kafkaDialogmotekandidatEndringStoppunktConsumerDelayNotPassedRecord = dialogmotekandidatEndringConsumerRecord(
                        kafkaDialogmotekandidatEndring = kafkaDialogmotekandidatEndringStoppunktDelayNotPassed,
                    )

                    every { mockKafkaConsumerDialogmotekandidatEndring.poll(any<Duration>()) } returns ConsumerRecords(
                        mapOf(
                            dialogmoteKandidatTopicPartition to listOf(
                                kafkaDialogmotekandidatEndringStoppunktConsumerDelayNotPassedRecord,
                            )
                        )
                    )

                    kafkaOppfolgingstilfellePersonService.pollAndProcessRecords(
                        kafkaConsumer = mockKafkaConsumerOppfolgingstilfellePerson,
                    )
                    kafkaDialogmotekandidatEndringService.pollAndProcessRecords(
                        kafkaConsumer = mockKafkaConsumerDialogmotekandidatEndring,
                    )

                    runBlocking {
                        personBehandlendeEnhetCronjob.runJob()
                    }

                    with(
                        handleRequest(HttpMethod.Get, url) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.NoContent
                    }
                }

                it("should return list of PersonOversiktStatus, if MOTEBEHOV_SVAR_MOTTATT and if there is a person with 2 relevant active Oppfolgingstilfelle with different virksomhetsnummer") {
                    kafkaOppfolgingstilfellePersonService.pollAndProcessRecords(
                        kafkaConsumer = mockKafkaConsumerOppfolgingstilfellePerson,
                    )

                    val oversiktHendelseMotebehovMottatt = KOversikthendelse(
                        ARBEIDSTAKER_FNR,
                        OversikthendelseType.MOTEBEHOV_SVAR_MOTTATT.name,
                        NAV_ENHET,
                        LocalDateTime.now()
                    )
                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelseMotebehovMottatt)

                    runBlocking {
                        personOppfolgingstilfelleVirksomhetnavnCronjob.runJob()
                    }

                    runBlocking {
                        personBehandlendeEnhetCronjob.runJob()
                    }

                    with(
                        handleRequest(HttpMethod.Get, url) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.OK
                        val personOversiktStatus =
                            objectMapper.readValue<List<PersonOversiktStatusDTO>>(response.content!!).first()
                        personOversiktStatus.veilederIdent shouldBeEqualTo null
                        personOversiktStatus.fnr shouldBeEqualTo oversiktHendelseMotebehovMottatt.fnr
                        personOversiktStatus.navn shouldBeEqualTo getIdentName()
                        personOversiktStatus.enhet shouldBeEqualTo behandlendeEnhetDTO().enhetId
                        personOversiktStatus.motebehovUbehandlet shouldBeEqualTo true
                        personOversiktStatus.moteplanleggerUbehandlet shouldBeEqualTo null
                        personOversiktStatus.oppfolgingsplanLPSBistandUbehandlet shouldBeEqualTo null
                        personOversiktStatus.dialogmotesvarUbehandlet shouldBeEqualTo false
                        personOversiktStatus.dialogmotekandidat.shouldBeNull()
                        personOversiktStatus.motestatus.shouldBeNull()

                        personOversiktStatus.latestOppfolgingstilfelle.shouldNotBeNull()
                        checkPersonOppfolgingstilfelleDTO(
                            personOppfolgingstilfelleDTO = personOversiktStatus.latestOppfolgingstilfelle,
                            kafkaOppfolgingstilfellePerson = kafkaOppfolgingstilfellePersonRelevant,
                        )
                    }
                }

                it("should return list of PersonOversiktStatus, if MOTEPLANLEGGER_ALLE_SVAR_MOTTATT and if there is a person with a relevant Oppfolgingstilfelle valid in Arbeidsgiverperiode") {
                    kafkaOppfolgingstilfellePersonService.pollAndProcessRecords(
                        kafkaConsumer = mockKafkaConsumerOppfolgingstilfellePerson,
                    )

                    val oversiktHendelseMoteplanleggerMottatt = KOversikthendelse(
                        ARBEIDSTAKER_FNR,
                        OversikthendelseType.MOTEPLANLEGGER_ALLE_SVAR_MOTTATT.name,
                        NAV_ENHET,
                        LocalDateTime.now()
                    )
                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelseMoteplanleggerMottatt)

                    runBlocking {
                        personOppfolgingstilfelleVirksomhetnavnCronjob.runJob()
                    }

                    runBlocking {
                        personBehandlendeEnhetCronjob.runJob()
                    }

                    with(
                        handleRequest(HttpMethod.Get, url) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.OK
                        val personOversiktStatus =
                            objectMapper.readValue<List<PersonOversiktStatusDTO>>(response.content!!).first()
                        personOversiktStatus.veilederIdent shouldBeEqualTo null
                        personOversiktStatus.fnr shouldBeEqualTo oversiktHendelseMoteplanleggerMottatt.fnr
                        personOversiktStatus.navn shouldBeEqualTo getIdentName()
                        personOversiktStatus.enhet shouldBeEqualTo behandlendeEnhetDTO().enhetId
                        personOversiktStatus.motebehovUbehandlet shouldBeEqualTo null
                        personOversiktStatus.moteplanleggerUbehandlet shouldBeEqualTo true
                        personOversiktStatus.oppfolgingsplanLPSBistandUbehandlet shouldBeEqualTo null
                        personOversiktStatus.dialogmotesvarUbehandlet shouldBeEqualTo false
                        personOversiktStatus.dialogmotekandidat.shouldBeNull()
                        personOversiktStatus.motestatus.shouldBeNull()

                        personOversiktStatus.latestOppfolgingstilfelle.shouldNotBeNull()
                        checkPersonOppfolgingstilfelleDTO(
                            personOppfolgingstilfelleDTO = personOversiktStatus.latestOppfolgingstilfelle,
                            kafkaOppfolgingstilfellePerson = kafkaOppfolgingstilfellePersonRelevant,
                        )
                    }
                }

                it("should return Person, with MOTEBEHOV_SVAR_MOTTATT && MOTEPLANLEGGER_ALLE_SVAR_MOTTATT, and then receives Oppfolgingstilfelle and the OPPFOLGINGSPLANLPS_BISTAND_MOTTATT") {
                    val oversiktHendelse = KOversikthendelse(
                        ARBEIDSTAKER_FNR,
                        OversikthendelseType.MOTEBEHOV_SVAR_MOTTATT.name,
                        NAV_ENHET,
                        LocalDateTime.now()
                    )
                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelse)

                    val oversiktHendelseMoteplanleggerMottatt = KOversikthendelse(
                        ARBEIDSTAKER_FNR,
                        OversikthendelseType.MOTEPLANLEGGER_ALLE_SVAR_MOTTATT.name,
                        NAV_ENHET,
                        LocalDateTime.now()
                    )
                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelseMoteplanleggerMottatt)

                    val oversiktHendelseOPLPSBistandMottatt =
                        generateKOversikthendelse(OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_MOTTATT)
                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelseOPLPSBistandMottatt)

                    kafkaOppfolgingstilfellePersonService.pollAndProcessRecords(
                        kafkaConsumer = mockKafkaConsumerOppfolgingstilfellePerson,
                    )
                    runBlocking {
                        personOppfolgingstilfelleVirksomhetnavnCronjob.runJob()
                    }

                    runBlocking {
                        personBehandlendeEnhetCronjob.runJob()
                    }

                    val tilknytning = VeilederBrukerKnytning(VEILEDER_ID, ARBEIDSTAKER_FNR, NAV_ENHET)
                    database.lagreBrukerKnytningPaEnhet(tilknytning)

                    with(
                        handleRequest(HttpMethod.Get, url) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.OK
                        val personOversiktStatus =
                            objectMapper.readValue<List<PersonOversiktStatusDTO>>(response.content!!).first()
                        personOversiktStatus.veilederIdent shouldBeEqualTo tilknytning.veilederIdent
                        personOversiktStatus.fnr shouldBeEqualTo oversiktHendelseMoteplanleggerMottatt.fnr
                        personOversiktStatus.navn shouldBeEqualTo getIdentName()
                        personOversiktStatus.enhet shouldBeEqualTo behandlendeEnhetDTO().enhetId
                        personOversiktStatus.motebehovUbehandlet shouldBeEqualTo true
                        personOversiktStatus.moteplanleggerUbehandlet shouldBeEqualTo true
                        personOversiktStatus.oppfolgingsplanLPSBistandUbehandlet shouldBeEqualTo true
                        personOversiktStatus.dialogmotesvarUbehandlet shouldBeEqualTo false
                        personOversiktStatus.dialogmotekandidat.shouldBeNull()
                        personOversiktStatus.motestatus.shouldBeNull()

                        personOversiktStatus.latestOppfolgingstilfelle.shouldNotBeNull()
                        checkPersonOppfolgingstilfelleDTO(
                            personOppfolgingstilfelleDTO = personOversiktStatus.latestOppfolgingstilfelle,
                            kafkaOppfolgingstilfellePerson = kafkaOppfolgingstilfellePersonRelevant,
                        )
                    }
                }

                it("should return Person, receives Oppfolgingstilfelle, and then MOTEBEHOV_SVAR_MOTTATT and MOTEPLANLEGGER_ALLE_SVAR_MOTTATT and OPPFOLGINGSPLANLPS_BISTAND_MOTTATT") {
                    kafkaOppfolgingstilfellePersonService.pollAndProcessRecords(
                        kafkaConsumer = mockKafkaConsumerOppfolgingstilfellePerson,
                    )

                    val oversiktHendelse = KOversikthendelse(
                        ARBEIDSTAKER_FNR,
                        OversikthendelseType.MOTEBEHOV_SVAR_MOTTATT.name,
                        NAV_ENHET,
                        LocalDateTime.now()
                    )
                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelse)

                    val oversiktHendelseMoteplanleggerMottatt = KOversikthendelse(
                        ARBEIDSTAKER_FNR,
                        OversikthendelseType.MOTEPLANLEGGER_ALLE_SVAR_MOTTATT.name,
                        NAV_ENHET,
                        LocalDateTime.now()
                    )
                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelseMoteplanleggerMottatt)

                    val oversiktHendelseOPLPSBistandMottatt =
                        generateKOversikthendelse(OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_MOTTATT)
                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelseOPLPSBistandMottatt)

                    runBlocking {
                        personOppfolgingstilfelleVirksomhetnavnCronjob.runJob()
                    }

                    runBlocking {
                        personBehandlendeEnhetCronjob.runJob()
                    }

                    val tilknytning = VeilederBrukerKnytning(VEILEDER_ID, ARBEIDSTAKER_FNR, NAV_ENHET)
                    database.lagreBrukerKnytningPaEnhet(tilknytning)

                    with(
                        handleRequest(HttpMethod.Get, url) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.OK
                        val personOversiktStatus =
                            objectMapper.readValue<List<PersonOversiktStatusDTO>>(response.content!!).first()
                        personOversiktStatus.veilederIdent shouldBeEqualTo tilknytning.veilederIdent
                        personOversiktStatus.fnr shouldBeEqualTo oversiktHendelse.fnr
                        personOversiktStatus.navn shouldBeEqualTo getIdentName()
                        personOversiktStatus.enhet shouldBeEqualTo behandlendeEnhetDTO().enhetId
                        personOversiktStatus.motebehovUbehandlet shouldBeEqualTo true
                        personOversiktStatus.moteplanleggerUbehandlet shouldBeEqualTo true
                        personOversiktStatus.oppfolgingsplanLPSBistandUbehandlet shouldBeEqualTo true
                        personOversiktStatus.dialogmotesvarUbehandlet shouldBeEqualTo false
                        personOversiktStatus.dialogmotekandidat.shouldBeNull()
                        personOversiktStatus.motestatus.shouldBeNull()

                        personOversiktStatus.latestOppfolgingstilfelle.shouldNotBeNull()
                        checkPersonOppfolgingstilfelleDTO(
                            personOppfolgingstilfelleDTO = personOversiktStatus.latestOppfolgingstilfelle,
                            kafkaOppfolgingstilfellePerson = kafkaOppfolgingstilfellePersonRelevant,
                        )
                    }
                }

                it("should return Person, receives Oppfolgingstilfelle, then DIALOGMOTEKANDIDAT, and then receives DialogmoteStatusendring") {
                    kafkaOppfolgingstilfellePersonService.pollAndProcessRecords(
                        kafkaConsumer = mockKafkaConsumerOppfolgingstilfellePerson,
                    )
                    kafkaDialogmotekandidatEndringService.pollAndProcessRecords(
                        kafkaConsumer = mockKafkaConsumerDialogmotekandidatEndring,
                    )
                    kafkaDialogmoteStatusendringService.pollAndProcessRecords(
                        kafkaConsumer = mockKafkaConsumerDialogmoteStatusendring,
                    )
                    runBlocking {
                        personBehandlendeEnhetCronjob.runJob()
                    }

                    with(
                        handleRequest(HttpMethod.Get, url) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.OK
                        val personOversiktStatus =
                            objectMapper.readValue<List<PersonOversiktStatusDTO>>(response.content!!).first()
                        personOversiktStatus.shouldNotBeNull()
                        personOversiktStatus.veilederIdent shouldBeEqualTo null
                        personOversiktStatus.fnr shouldBeEqualTo kafkaDialogmoteStatusendring.getPersonIdent()
                        personOversiktStatus.enhet shouldBeEqualTo behandlendeEnhetDTO().enhetId
                        personOversiktStatus.motebehovUbehandlet.shouldBeNull()
                        personOversiktStatus.moteplanleggerUbehandlet.shouldBeNull()
                        personOversiktStatus.oppfolgingsplanLPSBistandUbehandlet.shouldBeNull()
                        personOversiktStatus.dialogmotesvarUbehandlet shouldBeEqualTo false
                        personOversiktStatus.dialogmotekandidat shouldBeEqualTo true
                        personOversiktStatus.motestatus shouldBeEqualTo kafkaDialogmoteStatusendring.getStatusEndringType()
                    }
                }

                it("should return Person, no Oppfolgingstilfelle, and then OPPFOLGINGSPLANLPS_BISTAND_MOTTATT") {
                    val oversiktHendelseOPLPSBistandMottatt = generateKOversikthendelse(
                        oversikthendelseType = OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_MOTTATT
                    )
                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelseOPLPSBistandMottatt)

                    runBlocking {
                        personBehandlendeEnhetCronjob.runJob()
                    }

                    with(
                        handleRequest(HttpMethod.Get, url) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.OK
                        val personOversiktStatus =
                            objectMapper.readValue<List<PersonOversiktStatusDTO>>(response.content!!).first()
                        personOversiktStatus.veilederIdent shouldBeEqualTo null
                        personOversiktStatus.fnr shouldBeEqualTo oversiktHendelseOPLPSBistandMottatt.fnr
                        personOversiktStatus.navn shouldBeEqualTo getIdentName()
                        personOversiktStatus.enhet shouldBeEqualTo behandlendeEnhetDTO().enhetId
                        personOversiktStatus.motebehovUbehandlet shouldBeEqualTo null
                        personOversiktStatus.moteplanleggerUbehandlet shouldBeEqualTo null
                        personOversiktStatus.oppfolgingsplanLPSBistandUbehandlet shouldBeEqualTo true
                        personOversiktStatus.dialogmotesvarUbehandlet shouldBeEqualTo false
                        personOversiktStatus.dialogmotekandidat.shouldBeNull()
                        personOversiktStatus.motestatus.shouldBeNull()

                        personOversiktStatus.latestOppfolgingstilfelle.shouldBeNull()
                    }
                }

                it("should return Person with no Oppfolgingstilfelle and no Navn for OPPFOLGINGSPLANLPS_BISTAND_MOTTATT") {
                    val oversiktHendelseOPLPSBistandMottatt =
                        generateKOversikthendelse(OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_MOTTATT).copy(
                            fnr = ARBEIDSTAKER_NO_NAME_FNR,
                        )
                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelseOPLPSBistandMottatt)

                    runBlocking {
                        personBehandlendeEnhetCronjob.runJob()
                    }

                    with(
                        handleRequest(HttpMethod.Get, url) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.OK
                        val personOversiktStatus =
                            objectMapper.readValue<List<PersonOversiktStatusDTO>>(response.content!!).first()
                        personOversiktStatus.veilederIdent shouldBeEqualTo null
                        personOversiktStatus.fnr shouldBeEqualTo oversiktHendelseOPLPSBistandMottatt.fnr
                        personOversiktStatus.navn shouldBeEqualTo ""
                        personOversiktStatus.enhet shouldBeEqualTo behandlendeEnhetDTO().enhetId
                        personOversiktStatus.motebehovUbehandlet shouldBeEqualTo null
                        personOversiktStatus.moteplanleggerUbehandlet shouldBeEqualTo null
                        personOversiktStatus.oppfolgingsplanLPSBistandUbehandlet shouldBeEqualTo true
                        personOversiktStatus.dialogmotesvarUbehandlet shouldBeEqualTo false
                        personOversiktStatus.dialogmotekandidat.shouldBeNull()
                        personOversiktStatus.motestatus.shouldBeNull()

                        personOversiktStatus.latestOppfolgingstilfelle.shouldBeNull()
                    }
                }

                it("should not return Person if OversikthendelseType for Behandling is received without existing Person") {
                    val oversiktHendelseMotebehovSvarBehandlet =
                        generateKOversikthendelse(OversikthendelseType.MOTEBEHOV_SVAR_BEHANDLET)
                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelseMotebehovSvarBehandlet)

                    val oversiktHendelseMoteplanleggerAlleSvarBehandlet =
                        generateKOversikthendelse(OversikthendelseType.MOTEPLANLEGGER_ALLE_SVAR_BEHANDLET)
                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelseMoteplanleggerAlleSvarBehandlet)

                    val oversiktHendelseOPLPSBistandMottatt =
                        generateKOversikthendelse(OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_BEHANDLET)
                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelseOPLPSBistandMottatt)

                    runBlocking {
                        personBehandlendeEnhetCronjob.runJob()
                    }

                    with(
                        handleRequest(HttpMethod.Get, url) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.NoContent
                    }
                }
            }
        }
    }
})
