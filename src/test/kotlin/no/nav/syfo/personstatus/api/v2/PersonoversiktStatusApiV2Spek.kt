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
import no.nav.syfo.oppfolgingstilfelle.domain.PersonOppfolgingstilfelleVirksomhet
import no.nav.syfo.personoppgavehendelse.kafka.KPersonoppgavehendelse
import no.nav.syfo.personstatus.api.v2.endpoints.personOversiktApiV2Path
import no.nav.syfo.personstatus.api.v2.model.PersonOversiktStatusDTO
import no.nav.syfo.personstatus.db.createPersonOversiktStatus
import no.nav.syfo.personstatus.db.getPersonOversiktStatusList
import no.nav.syfo.personstatus.db.lagreVeilederForBruker
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
import no.nav.syfo.testutil.mock.latestVurdering
import no.nav.syfo.util.*
import org.amshove.kluent.*
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.*
import java.util.*

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

            val personOppfolgingstilfelleVirksomhetnavnCronjob =
                internalMockEnvironment.personOppfolgingstilfelleVirksomhetnavnCronjob

            val personoversiktStatusService = internalMockEnvironment.personoversiktStatusService
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

            val mockKafkaConsumerDialogmotekandidatEndring =
                TestKafkaModule.kafkaConsumerDialogmotekandidatEndring
            val dialogmoteKandidatTopicPartition = dialogmotekandidatEndringTopicPartition()
            val kafkaDialogmotekandidatEndringStoppunktDelayPassed = generateKafkaDialogmotekandidatEndringStoppunkt(
                personIdent = ARBEIDSTAKER_FNR,
                createdAt = nowUTC().minusDays(7)
            )
            val kafkaDialogmotekandidatEndringStoppunktConsumerDelayPassedRecord =
                dialogmotekandidatEndringConsumerRecord(
                    kafkaDialogmotekandidatEndring = kafkaDialogmotekandidatEndringStoppunktDelayPassed,
                )

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
                database.dropData()

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

                it("skal returnere status Unauthorized om token mangler") {
                    with(
                        handleRequest(HttpMethod.Get, url)
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.Unauthorized
                    }
                }

                it("skal returnere status BadRequest om enhet ugyldig") {
                    with(
                        handleRequest(HttpMethod.Get, "$personOversiktApiV2Path/enhet/12345") {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.BadRequest
                    }
                }

                it("skal returnere NoContent med ubehandlet motebehovsvar og ikke har oppfolgingstilfelle") {
                    val oversiktHendelse = KPersonoppgavehendelse(
                        ARBEIDSTAKER_FNR,
                        OversikthendelseType.MOTEBEHOV_SVAR_MOTTATT,
                    )
                    personoversiktStatusService.createOrUpdatePersonoversiktStatuser(
                        personoppgavehendelser = listOf(oversiktHendelse)
                    )

                    database.setTildeltEnhet(
                        ident = PersonIdent(ARBEIDSTAKER_FNR),
                        enhet = NAV_ENHET,
                    )

                    val tilknytning = VeilederBrukerKnytning(VEILEDER_ID, ARBEIDSTAKER_FNR, NAV_ENHET)
                    database.lagreVeilederForBruker(tilknytning)

                    with(
                        handleRequest(HttpMethod.Get, url) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.NoContent
                    }
                }

                it("skal returnere NoContent, om alle personer i personoversikt er behandlet og ikke har oppfolgingstilfelle") {
                    val oversiktHendelse = KPersonoppgavehendelse(
                        ARBEIDSTAKER_FNR,
                        OversikthendelseType.MOTEBEHOV_SVAR_MOTTATT,
                    )
                    personoversiktStatusService.createOrUpdatePersonoversiktStatuser(
                        personoppgavehendelser = listOf(oversiktHendelse)
                    )
                    val oversiktHendelseNy = KPersonoppgavehendelse(
                        ARBEIDSTAKER_FNR,
                        OversikthendelseType.MOTEBEHOV_SVAR_BEHANDLET,
                    )
                    personoversiktStatusService.createOrUpdatePersonoversiktStatuser(
                        personoppgavehendelser = listOf(oversiktHendelseNy)
                    )

                    database.setTildeltEnhet(
                        ident = PersonIdent(ARBEIDSTAKER_FNR),
                        enhet = NAV_ENHET,
                    )

                    val tilknytning = VeilederBrukerKnytning(VEILEDER_ID, ARBEIDSTAKER_FNR, NAV_ENHET)
                    database.lagreVeilederForBruker(tilknytning)

                    with(
                        handleRequest(HttpMethod.Get, url) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.NoContent
                    }
                }

                it("should return NoContent, if there is a person with a relevant active Oppfolgingstilfelle, but neither MOTEBEHOV_SVAR_MOTTATT nor DIALOGMOTEKANDIDAT") {
                    kafkaOppfolgingstilfellePersonService.pollAndProcessRecords(
                        kafkaConsumer = mockKafkaConsumerOppfolgingstilfellePerson,
                    )
                    runBlocking {
                        personOppfolgingstilfelleVirksomhetnavnCronjob.runJob()
                    }
                    database.setTildeltEnhet(
                        ident = PersonIdent(ARBEIDSTAKER_FNR),
                        enhet = NAV_ENHET,
                    )

                    with(
                        handleRequest(HttpMethod.Get, url) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.NoContent
                    }
                }

                it("should return list of PersonOversiktStatus, if MOTEBEHOV_SVAR_MOTTATT and OPPFOLGINGSPLANLPS_BISTAND_MOTTATT and DIALOGMOTESVAR_MOTTATT, and there is a person with a relevant active Oppfolgingstilfelle") {
                    kafkaOppfolgingstilfellePersonService.pollAndProcessRecords(
                        kafkaConsumer = mockKafkaConsumerOppfolgingstilfellePerson,
                    )
                    val oversiktHendelse = KPersonoppgavehendelse(
                        ARBEIDSTAKER_FNR,
                        OversikthendelseType.MOTEBEHOV_SVAR_MOTTATT,
                    )
                    val oversiktHendelseOPLPSBistandMottatt = KPersonoppgavehendelse(
                        ARBEIDSTAKER_FNR,
                        OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_MOTTATT,
                    )
                    val dialogmotesvarMottatt = KPersonoppgavehendelse(
                        ARBEIDSTAKER_FNR,
                        OversikthendelseType.DIALOGMOTESVAR_MOTTATT,
                    )
                    personoversiktStatusService.createOrUpdatePersonoversiktStatuser(
                        personoppgavehendelser = listOf(
                            oversiktHendelse,
                            oversiktHendelseOPLPSBistandMottatt,
                            dialogmotesvarMottatt,
                        )
                    )

                    runBlocking {
                        personOppfolgingstilfelleVirksomhetnavnCronjob.runJob()
                    }
                    database.setTildeltEnhet(
                        ident = PersonIdent(ARBEIDSTAKER_FNR),
                        enhet = NAV_ENHET,
                    )

                    with(
                        handleRequest(HttpMethod.Get, url) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.OK
                        val personOversiktStatus =
                            objectMapper.readValue<List<PersonOversiktStatusDTO>>(response.content!!).first {
                                it.fnr == ARBEIDSTAKER_FNR
                            }
                        personOversiktStatus.veilederIdent shouldBeEqualTo null
                        personOversiktStatus.fnr shouldBeEqualTo ARBEIDSTAKER_FNR
                        personOversiktStatus.enhet shouldBeEqualTo behandlendeEnhetDTO().enhetId
                        personOversiktStatus.motebehovUbehandlet shouldBeEqualTo true
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

                it("should return list of PersonOversiktStatus, if MOTEBEHOV_SVAR_MOTTATT and if there is a person with 2 relevant active Oppfolgingstilfelle with different virksomhetsnummer") {
                    kafkaOppfolgingstilfellePersonService.pollAndProcessRecords(
                        kafkaConsumer = mockKafkaConsumerOppfolgingstilfellePerson,
                    )
                    val oversiktHendelse = KPersonoppgavehendelse(
                        ARBEIDSTAKER_FNR,
                        OversikthendelseType.MOTEBEHOV_SVAR_MOTTATT,
                    )
                    personoversiktStatusService.createOrUpdatePersonoversiktStatuser(
                        personoppgavehendelser = listOf(oversiktHendelse)
                    )

                    runBlocking {
                        personOppfolgingstilfelleVirksomhetnavnCronjob.runJob()
                    }

                    database.setTildeltEnhet(
                        ident = PersonIdent(ARBEIDSTAKER_FNR),
                        enhet = NAV_ENHET,
                    )

                    with(
                        handleRequest(HttpMethod.Get, url) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.OK
                        val personOversiktStatus =
                            objectMapper.readValue<List<PersonOversiktStatusDTO>>(response.content!!).first()
                        personOversiktStatus.veilederIdent shouldBeEqualTo null
                        personOversiktStatus.fnr shouldBeEqualTo oversiktHendelse.personident
                        personOversiktStatus.navn shouldBeEqualTo getIdentName()
                        personOversiktStatus.enhet shouldBeEqualTo behandlendeEnhetDTO().enhetId
                        personOversiktStatus.motebehovUbehandlet shouldBeEqualTo true
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

                it("should return Person, with MOTEBEHOV_SVAR_MOTTATT, and then receives Oppfolgingstilfelle and the OPPFOLGINGSPLANLPS_BISTAND_MOTTATT") {
                    val oversiktHendelse = KPersonoppgavehendelse(
                        ARBEIDSTAKER_FNR,
                        OversikthendelseType.MOTEBEHOV_SVAR_MOTTATT,
                    )
                    val oversiktHendelseOPLPSBistandMottatt = KPersonoppgavehendelse(
                        ARBEIDSTAKER_FNR,
                        OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_MOTTATT,
                    )
                    personoversiktStatusService.createOrUpdatePersonoversiktStatuser(
                        personoppgavehendelser = listOf(oversiktHendelse, oversiktHendelseOPLPSBistandMottatt)
                    )

                    kafkaOppfolgingstilfellePersonService.pollAndProcessRecords(
                        kafkaConsumer = mockKafkaConsumerOppfolgingstilfellePerson,
                    )
                    runBlocking {
                        personOppfolgingstilfelleVirksomhetnavnCronjob.runJob()
                    }

                    database.setTildeltEnhet(
                        ident = PersonIdent(ARBEIDSTAKER_FNR),
                        enhet = NAV_ENHET,
                    )

                    val tilknytning = VeilederBrukerKnytning(VEILEDER_ID, ARBEIDSTAKER_FNR, NAV_ENHET)
                    database.lagreVeilederForBruker(tilknytning)

                    with(
                        handleRequest(HttpMethod.Get, url) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.OK
                        val personOversiktStatus =
                            objectMapper.readValue<List<PersonOversiktStatusDTO>>(response.content!!).first()
                        personOversiktStatus.veilederIdent shouldBeEqualTo tilknytning.veilederIdent
                        personOversiktStatus.fnr shouldBeEqualTo oversiktHendelse.personident
                        personOversiktStatus.navn shouldBeEqualTo getIdentName()
                        personOversiktStatus.enhet shouldBeEqualTo behandlendeEnhetDTO().enhetId
                        personOversiktStatus.motebehovUbehandlet shouldBeEqualTo true
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

                it("should return Person, receives Oppfolgingstilfelle, and then MOTEBEHOV_SVAR_MOTTATT and OPPFOLGINGSPLANLPS_BISTAND_MOTTATT") {
                    kafkaOppfolgingstilfellePersonService.pollAndProcessRecords(
                        kafkaConsumer = mockKafkaConsumerOppfolgingstilfellePerson,
                    )

                    val oversiktHendelse = KPersonoppgavehendelse(
                        ARBEIDSTAKER_FNR,
                        OversikthendelseType.MOTEBEHOV_SVAR_MOTTATT,
                    )
                    val oversiktHendelseOPLPSBistandMottatt = KPersonoppgavehendelse(
                        ARBEIDSTAKER_FNR,
                        OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_MOTTATT,
                    )
                    personoversiktStatusService.createOrUpdatePersonoversiktStatuser(
                        personoppgavehendelser = listOf(oversiktHendelse, oversiktHendelseOPLPSBistandMottatt)
                    )

                    runBlocking {
                        personOppfolgingstilfelleVirksomhetnavnCronjob.runJob()
                    }

                    database.setTildeltEnhet(
                        ident = PersonIdent(ARBEIDSTAKER_FNR),
                        enhet = NAV_ENHET,
                    )

                    val tilknytning = VeilederBrukerKnytning(VEILEDER_ID, ARBEIDSTAKER_FNR, NAV_ENHET)
                    database.lagreVeilederForBruker(tilknytning)

                    with(
                        handleRequest(HttpMethod.Get, url) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.OK
                        val personOversiktStatus =
                            objectMapper.readValue<List<PersonOversiktStatusDTO>>(response.content!!).first()
                        personOversiktStatus.veilederIdent shouldBeEqualTo tilknytning.veilederIdent
                        personOversiktStatus.fnr shouldBeEqualTo oversiktHendelse.personident
                        personOversiktStatus.navn shouldBeEqualTo getIdentName()
                        personOversiktStatus.enhet shouldBeEqualTo behandlendeEnhetDTO().enhetId
                        personOversiktStatus.motebehovUbehandlet shouldBeEqualTo true
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

                it("should return Person, no Oppfolgingstilfelle, and then OPPFOLGINGSPLANLPS_BISTAND_MOTTATT") {
                    val oversiktHendelseOPLPSBistandMottatt = KPersonoppgavehendelse(
                        ARBEIDSTAKER_FNR,
                        OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_MOTTATT,
                    )
                    personoversiktStatusService.createOrUpdatePersonoversiktStatuser(
                        personoppgavehendelser = listOf(oversiktHendelseOPLPSBistandMottatt)
                    )

                    database.setTildeltEnhet(
                        ident = PersonIdent(ARBEIDSTAKER_FNR),
                        enhet = NAV_ENHET,
                    )

                    with(
                        handleRequest(HttpMethod.Get, url) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.OK
                        val personOversiktStatus =
                            objectMapper.readValue<List<PersonOversiktStatusDTO>>(response.content!!).first()
                        personOversiktStatus.veilederIdent shouldBeEqualTo null
                        personOversiktStatus.fnr shouldBeEqualTo oversiktHendelseOPLPSBistandMottatt.personident
                        personOversiktStatus.navn shouldBeEqualTo getIdentName()
                        personOversiktStatus.enhet shouldBeEqualTo behandlendeEnhetDTO().enhetId
                        personOversiktStatus.motebehovUbehandlet shouldBeEqualTo null
                        personOversiktStatus.oppfolgingsplanLPSBistandUbehandlet shouldBeEqualTo true
                        personOversiktStatus.dialogmotesvarUbehandlet shouldBeEqualTo false
                        personOversiktStatus.dialogmotekandidat.shouldBeNull()
                        personOversiktStatus.motestatus.shouldBeNull()

                        personOversiktStatus.latestOppfolgingstilfelle.shouldBeNull()
                    }
                }

                it("should return Person with no Oppfolgingstilfelle and no Navn for OPPFOLGINGSPLANLPS_BISTAND_MOTTATT") {
                    val oversiktHendelseOPLPSBistandMottatt = KPersonoppgavehendelse(
                        ARBEIDSTAKER_NO_NAME_FNR,
                        OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_MOTTATT,
                    )
                    personoversiktStatusService.createOrUpdatePersonoversiktStatuser(
                        personoppgavehendelser = listOf(oversiktHendelseOPLPSBistandMottatt)
                    )

                    database.setTildeltEnhet(
                        ident = PersonIdent(ARBEIDSTAKER_NO_NAME_FNR),
                        enhet = NAV_ENHET,
                    )

                    with(
                        handleRequest(HttpMethod.Get, url) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.OK
                        val personOversiktStatus =
                            objectMapper.readValue<List<PersonOversiktStatusDTO>>(response.content!!).first()
                        personOversiktStatus.veilederIdent shouldBeEqualTo null
                        personOversiktStatus.fnr shouldBeEqualTo oversiktHendelseOPLPSBistandMottatt.personident
                        personOversiktStatus.navn shouldBeEqualTo ""
                        personOversiktStatus.enhet shouldBeEqualTo behandlendeEnhetDTO().enhetId
                        personOversiktStatus.motebehovUbehandlet shouldBeEqualTo null
                        personOversiktStatus.oppfolgingsplanLPSBistandUbehandlet shouldBeEqualTo true
                        personOversiktStatus.dialogmotesvarUbehandlet shouldBeEqualTo false
                        personOversiktStatus.dialogmotekandidat.shouldBeNull()
                        personOversiktStatus.motestatus.shouldBeNull()

                        personOversiktStatus.latestOppfolgingstilfelle.shouldBeNull()
                    }
                }

                it("should not return Person if OversikthendelseType for Behandling is received without existing Person") {
                    val oversiktHendelseMotebehovSvarBehandlet = KPersonoppgavehendelse(
                        ARBEIDSTAKER_FNR,
                        OversikthendelseType.MOTEBEHOV_SVAR_BEHANDLET,
                    )
                    val oversiktHendelseOPLPSBistandBehandlet = KPersonoppgavehendelse(
                        ARBEIDSTAKER_FNR,
                        OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_BEHANDLET,
                    )
                    personoversiktStatusService.createOrUpdatePersonoversiktStatuser(
                        personoppgavehendelser = listOf(
                            oversiktHendelseMotebehovSvarBehandlet,
                            oversiktHendelseOPLPSBistandBehandlet,
                        )
                    )

                    database.setTildeltEnhet(
                        ident = PersonIdent(ARBEIDSTAKER_FNR),
                        enhet = NAV_ENHET,
                    )

                    with(
                        handleRequest(HttpMethod.Get, url) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.NoContent
                    }
                }

                it("return person with dialogmotesvar_ubehandlet true") {
                    val oversikthendelseDialogmotesvarMottatt = KPersonoppgavehendelse(
                        ARBEIDSTAKER_FNR,
                        OversikthendelseType.DIALOGMOTESVAR_MOTTATT,
                    )
                    personoversiktStatusService.createOrUpdatePersonoversiktStatuser(
                        personoppgavehendelser = listOf(oversikthendelseDialogmotesvarMottatt)
                    )
                    database.setTildeltEnhet(
                        ident = PersonIdent(ARBEIDSTAKER_FNR),
                        enhet = NAV_ENHET,
                    )

                    with(
                        handleRequest(HttpMethod.Get, url) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.OK

                        val personOversiktStatus =
                            objectMapper.readValue<List<PersonOversiktStatusDTO>>(response.content!!).first()
                        personOversiktStatus.fnr shouldBeEqualTo oversikthendelseDialogmotesvarMottatt.personident
                        personOversiktStatus.enhet shouldBeEqualTo behandlendeEnhetDTO().enhetId
                        personOversiktStatus.dialogmotesvarUbehandlet shouldBeEqualTo true
                    }
                }
                it("return person with friskmelding til arbeidsformidling starting tomorrow") {
                    val personident = PersonIdent(ARBEIDSTAKER_FNR)
                    val tomorrow = LocalDate.now().plusDays(1)
                    with(database) {
                        createPersonOversiktStatus(PersonOversiktStatus(fnr = personident.value))
                        setTildeltEnhet(personident, NAV_ENHET)
                        setFriskmeldingTilArbeidsformidlingFom(personident, tomorrow)
                    }

                    with(
                        handleRequest(HttpMethod.Get, url) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.OK

                        val personOversiktStatus =
                            objectMapper.readValue<List<PersonOversiktStatusDTO>>(response.content!!).first()
                        personOversiktStatus.fnr shouldBeEqualTo personident.value
                        personOversiktStatus.enhet shouldBeEqualTo behandlendeEnhetDTO().enhetId
                        personOversiktStatus.friskmeldingTilArbeidsformidlingFom!! shouldBeEqualTo tomorrow
                    }
                }
                it("return person with friskmelding til arbeidsformidling starting today") {
                    val personident = PersonIdent(ARBEIDSTAKER_FNR)
                    val today = LocalDate.now()
                    with(database) {
                        createPersonOversiktStatus(PersonOversiktStatus(fnr = personident.value))
                        setTildeltEnhet(personident, NAV_ENHET)
                        setFriskmeldingTilArbeidsformidlingFom(personident, today)
                    }

                    with(
                        handleRequest(HttpMethod.Get, url) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.OK

                        val personOversiktStatus =
                            objectMapper.readValue<List<PersonOversiktStatusDTO>>(response.content!!).first()
                        personOversiktStatus.fnr shouldBeEqualTo personident.value
                        personOversiktStatus.enhet shouldBeEqualTo behandlendeEnhetDTO().enhetId
                        personOversiktStatus.friskmeldingTilArbeidsformidlingFom!! shouldBeEqualTo today
                    }
                }
                it("return person with friskmelding til arbeidsformidling starting yesterday") {
                    val personident = PersonIdent(ARBEIDSTAKER_FNR)
                    val yesterday = LocalDate.now().minusDays(1)
                    with(database) {
                        createPersonOversiktStatus(PersonOversiktStatus(fnr = personident.value))
                        setTildeltEnhet(personident, NAV_ENHET)
                        setFriskmeldingTilArbeidsformidlingFom(personident, yesterday)
                    }
                    with(
                        handleRequest(HttpMethod.Get, url) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                        }
                    ) {
                        val personOversiktStatus =
                            objectMapper.readValue<List<PersonOversiktStatusDTO>>(response.content!!).first()
                        personOversiktStatus.fnr shouldBeEqualTo personident.value
                        personOversiktStatus.enhet shouldBeEqualTo behandlendeEnhetDTO().enhetId
                        personOversiktStatus.friskmeldingTilArbeidsformidlingFom!! shouldBeEqualTo yesterday
                    }
                }

                it("Should update name in database") {
                    val personIdent = PersonIdent(ARBEIDSTAKER_FNR)
                    val oversikthendelseDialogmotesvarMottatt = KPersonoppgavehendelse(
                        ARBEIDSTAKER_FNR,
                        OversikthendelseType.DIALOGMOTESVAR_MOTTATT,
                    )
                    personoversiktStatusService.createOrUpdatePersonoversiktStatuser(
                        personoppgavehendelser = listOf(oversikthendelseDialogmotesvarMottatt)
                    )

                    val tilknytning = VeilederBrukerKnytning(VEILEDER_ID, personIdent.value, NAV_ENHET)
                    database.lagreVeilederForBruker(tilknytning)
                    database.setTildeltEnhet(
                        ident = personIdent,
                        enhet = NAV_ENHET,
                    )
                    with(
                        handleRequest(HttpMethod.Get, url) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.OK
                        val personOversiktStatus =
                            objectMapper.readValue<List<PersonOversiktStatusDTO>>(response.content!!).first()
                        personOversiktStatus.navn shouldBeEqualTo "Fornavn${personIdent.value} Mellomnavn${personIdent.value} Etternavn${personIdent.value}"
                    }

                    val personOversiktStatusList = database.getPersonOversiktStatusList(personIdent.value)
                    personOversiktStatusList.first().navn shouldBeEqualTo "Fornavn${personIdent.value} Mellomnavn${personIdent.value} Etternavn${personIdent.value}"
                }

                it("return person with aktivitetskrav_vurder_stans true when oppgave mottatt") {
                    val aktivitetskravVurderStansMottatt = KPersonoppgavehendelse(
                        personident = ARBEIDSTAKER_FNR,
                        hendelsetype = OversikthendelseType.AKTIVITETSKRAV_VURDER_STANS_MOTTATT,
                    )
                    val personoversiktStatus = PersonOversiktStatus(
                        fnr = aktivitetskravVurderStansMottatt.personident
                    ).applyHendelse(aktivitetskravVurderStansMottatt.hendelsetype)

                    database.createPersonOversiktStatus(personoversiktStatus)

                    database.setTildeltEnhet(
                        ident = PersonIdent(ARBEIDSTAKER_FNR),
                        enhet = NAV_ENHET,
                    )

                    with(
                        handleRequest(HttpMethod.Get, url) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.OK

                        val personOversiktStatus =
                            objectMapper.readValue<List<PersonOversiktStatusDTO>>(response.content!!).first()
                        personOversiktStatus.fnr shouldBeEqualTo aktivitetskravVurderStansMottatt.personident
                        personOversiktStatus.enhet shouldBeEqualTo behandlendeEnhetDTO().enhetId
                        personOversiktStatus.aktivitetskravVurderStansUbehandlet shouldBeEqualTo true
                    }
                }

                it("return no person when aktivitetskrav_vurder_stans oppgave behandlet") {
                    val aktivitetskravVurderStansBehandlet = KPersonoppgavehendelse(
                        personident = ARBEIDSTAKER_FNR,
                        hendelsetype = OversikthendelseType.AKTIVITETSKRAV_VURDER_STANS_BEHANDLET,
                    )
                    val personoversiktStatus = PersonOversiktStatus(
                        fnr = aktivitetskravVurderStansBehandlet.personident
                    ).applyHendelse(aktivitetskravVurderStansBehandlet.hendelsetype)

                    database.createPersonOversiktStatus(personoversiktStatus)

                    database.setTildeltEnhet(
                        ident = PersonIdent(ARBEIDSTAKER_FNR),
                        enhet = NAV_ENHET,
                    )

                    with(
                        handleRequest(HttpMethod.Get, url) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.NoContent
                    }
                }

                it("return person when trenger_oppfolging true") {
                    val personident = ARBEIDSTAKER_FNR
                    val personoversiktStatus = PersonOversiktStatus(
                        fnr = personident,
                    ).copy(trengerOppfolging = true)

                    database.createPersonOversiktStatus(personoversiktStatus)

                    database.setTildeltEnhet(
                        ident = PersonIdent(ARBEIDSTAKER_FNR),
                        enhet = NAV_ENHET,
                    )

                    with(
                        handleRequest(HttpMethod.Get, url) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.OK

                        val personOversiktStatus =
                            objectMapper.readValue<List<PersonOversiktStatusDTO>>(response.content!!).first()
                        personOversiktStatus.fnr shouldBeEqualTo personident
                        personOversiktStatus.enhet shouldBeEqualTo behandlendeEnhetDTO().enhetId
                        personOversiktStatus.oppfolgingsoppgave shouldNotBe null
                        personOversiktStatus.oppfolgingsoppgave?.oppfolgingsgrunn shouldBeEqualTo "FOLG_OPP_ETTER_NESTE_SYKMELDING"
                    }
                }

                it("return no person when trenger_oppfolging false") {
                    val personident = ARBEIDSTAKER_FNR
                    val personoversiktStatus = PersonOversiktStatus(
                        fnr = personident,
                    )

                    database.createPersonOversiktStatus(personoversiktStatus)

                    database.setTildeltEnhet(
                        ident = PersonIdent(ARBEIDSTAKER_FNR),
                        enhet = NAV_ENHET,
                    )

                    with(
                        handleRequest(HttpMethod.Get, url) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.NoContent
                    }
                }

                it("return person with behandler_ber_om_bistand_ubehandlet true when oppgave mottatt") {
                    val personoversiktStatus = PersonOversiktStatus(
                        fnr = ARBEIDSTAKER_FNR
                    ).applyHendelse(OversikthendelseType.BEHANDLER_BER_OM_BISTAND_MOTTATT)

                    database.createPersonOversiktStatus(personoversiktStatus)

                    database.setTildeltEnhet(
                        ident = PersonIdent(ARBEIDSTAKER_FNR),
                        enhet = NAV_ENHET,
                    )

                    with(
                        handleRequest(HttpMethod.Get, url) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.OK

                        val personOversiktStatus =
                            objectMapper.readValue<List<PersonOversiktStatusDTO>>(response.content!!).first()
                        personOversiktStatus.fnr shouldBeEqualTo ARBEIDSTAKER_FNR
                        personOversiktStatus.enhet shouldBeEqualTo behandlendeEnhetDTO().enhetId
                        personOversiktStatus.behandlerBerOmBistandUbehandlet shouldBeEqualTo true
                    }
                }

                it("return no person when behandler_ber_om_bistand-oppgave behandlet") {
                    val personoversiktStatus = PersonOversiktStatus(
                        fnr = ARBEIDSTAKER_FNR
                    ).applyHendelse(OversikthendelseType.BEHANDLER_BER_OM_BISTAND_BEHANDLET)

                    database.createPersonOversiktStatus(personoversiktStatus)

                    database.setTildeltEnhet(
                        ident = PersonIdent(ARBEIDSTAKER_FNR),
                        enhet = NAV_ENHET,
                    )

                    with(
                        handleRequest(HttpMethod.Get, url) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.NoContent
                    }
                }

                it("return person with correct varighetUker based on antallSykedager") {
                    val oppfolgingstilfelle = generateOppfolgingstilfelle(
                        start = LocalDate.now().minusDays(30),
                        end = LocalDate.now().minusDays(1),
                        antallSykedager = 14,
                    )
                    val personoversiktStatus = PersonOversiktStatus(
                        fnr = ARBEIDSTAKER_FNR
                    ).copy(
                        latestOppfolgingstilfelle = oppfolgingstilfelle,
                        motebehovUbehandlet = true,
                    )

                    database.createPersonOversiktStatus(personoversiktStatus)

                    database.setTildeltEnhet(
                        ident = PersonIdent(ARBEIDSTAKER_FNR),
                        enhet = NAV_ENHET,
                    )

                    with(
                        handleRequest(HttpMethod.Get, url) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.OK

                        val personOversiktStatus =
                            objectMapper.readValue<List<PersonOversiktStatusDTO>>(response.content!!).first()
                        personOversiktStatus.fnr shouldBeEqualTo ARBEIDSTAKER_FNR
                        personOversiktStatus.latestOppfolgingstilfelle?.varighetUker shouldBeEqualTo 2
                    }
                }

                it("return person with correct virksomhetslist") {
                    val virksomhetList = listOf(
                        PersonOppfolgingstilfelleVirksomhet(
                            uuid = UUID.randomUUID(),
                            createdAt = OffsetDateTime.now(),
                            virksomhetsnummer = Virksomhetsnummer("123456789"),
                            virksomhetsnavn = "Virksomhet AS",
                        ),
                        PersonOppfolgingstilfelleVirksomhet(
                            uuid = UUID.randomUUID(),
                            createdAt = OffsetDateTime.now(),
                            virksomhetsnummer = Virksomhetsnummer("123456000"),
                            virksomhetsnavn = null,
                        )
                    )
                    val oppfolgingstilfelle = generateOppfolgingstilfelle(
                        start = LocalDate.now().minusDays(30),
                        end = LocalDate.now().minusDays(1),
                        antallSykedager = 14,
                        virksomhetList = virksomhetList,
                    )
                    val personoversiktStatus = PersonOversiktStatus(
                        fnr = ARBEIDSTAKER_FNR
                    ).copy(
                        latestOppfolgingstilfelle = oppfolgingstilfelle,
                        motebehovUbehandlet = true,
                    )

                    database.createPersonOversiktStatus(personoversiktStatus)
                    database.setTildeltEnhet(
                        ident = PersonIdent(ARBEIDSTAKER_FNR),
                        enhet = NAV_ENHET,
                    )

                    with(
                        handleRequest(HttpMethod.Get, url) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.OK

                        val personOversiktStatus =
                            objectMapper.readValue<List<PersonOversiktStatusDTO>>(response.content!!).first()
                        personOversiktStatus.fnr shouldBeEqualTo ARBEIDSTAKER_FNR
                        personOversiktStatus.latestOppfolgingstilfelle?.virksomhetList?.get(0)?.virksomhetsnavn shouldBeEqualTo "Virksomhet AS"
                        personOversiktStatus.latestOppfolgingstilfelle?.virksomhetList?.get(1)?.virksomhetsnavn shouldBeEqualTo null
                    }
                }

                it("return person when isAktivSenOppfolgingKandidat true") {
                    val personident = ARBEIDSTAKER_FNR
                    val personoversiktStatus = PersonOversiktStatus(
                        fnr = personident,
                    ).copy(isAktivSenOppfolgingKandidat = true)

                    database.createPersonOversiktStatus(personoversiktStatus)

                    database.setTildeltEnhet(
                        ident = PersonIdent(ARBEIDSTAKER_FNR),
                        enhet = NAV_ENHET,
                    )

                    with(
                        handleRequest(HttpMethod.Get, url) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.OK

                        val personOversiktStatus =
                            objectMapper.readValue<List<PersonOversiktStatusDTO>>(response.content!!).first()
                        personOversiktStatus.fnr shouldBeEqualTo personident
                        personOversiktStatus.enhet shouldBeEqualTo NAV_ENHET
                        personOversiktStatus.isAktivSenOppfolgingKandidat shouldBeEqualTo true
                    }
                }

                it("return no person when isAktivSenOppfolgingKandidat false") {
                    val personident = ARBEIDSTAKER_FNR
                    val personoversiktStatus = PersonOversiktStatus(fnr = personident)

                    database.createPersonOversiktStatus(personoversiktStatus)
                    database.setTildeltEnhet(
                        ident = PersonIdent(ARBEIDSTAKER_FNR),
                        enhet = NAV_ENHET,
                    )

                    with(
                        handleRequest(HttpMethod.Get, url) {
                            addHeader(HttpHeaders.Authorization, bearerHeader(validToken))
                        }
                    ) {
                        response.status() shouldBeEqualTo HttpStatusCode.NoContent
                    }
                }

                describe("arbeidsuforhetvurdering") {
                    it("returns person with active arbeidsuforhetvurdering") {
                        val newPersonOversiktStatus = PersonOversiktStatus(fnr = ARBEIDSTAKER_FNR)
                            .copy(isAktivArbeidsuforhetvurdering = true)
                        database.connection.use { connection ->
                            connection.createPersonOversiktStatus(commit = true, personOversiktStatus = newPersonOversiktStatus)
                        }
                        database.setTildeltEnhet(
                            ident = PersonIdent(ARBEIDSTAKER_FNR),
                            enhet = NAV_ENHET,
                        )

                        with(
                            handleRequest(HttpMethod.Get, url) { addHeader(HttpHeaders.Authorization, bearerHeader(validToken)) }
                        ) {
                            response.status() shouldBeEqualTo HttpStatusCode.OK

                            val personOversiktStatus =
                                objectMapper.readValue<List<PersonOversiktStatusDTO>>(response.content!!).first()
                            personOversiktStatus.fnr shouldBeEqualTo ARBEIDSTAKER_FNR
                            personOversiktStatus.arbeidsuforhetvurdering shouldNotBe null
                            personOversiktStatus.arbeidsuforhetvurdering?.varsel shouldNotBe null
                            personOversiktStatus.arbeidsuforhetvurdering?.createdAt shouldBeEqualTo latestVurdering
                        }
                    }
                }
            }
        }
    }
})
