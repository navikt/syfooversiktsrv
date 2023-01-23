package no.nav.syfo.personstatus.api.v2

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.http.*
import io.ktor.server.testing.*
import io.ktor.util.*
import io.mockk.clearMocks
import io.mockk.every
import kotlinx.coroutines.runBlocking
import no.nav.syfo.aktivitetskravvurdering.domain.Aktivitetskrav
import no.nav.syfo.aktivitetskravvurdering.domain.AktivitetskravStatus
import no.nav.syfo.aktivitetskravvurdering.persistAktivitetskrav
import no.nav.syfo.dialogmotestatusendring.domain.DialogmoteStatusendringType
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.domain.Virksomhetsnummer
import no.nav.syfo.personoppgavehendelse.kafka.KPersonoppgavehendelse
import no.nav.syfo.personoppgavehendelse.kafka.PersonoppgavehendelseService
import no.nav.syfo.personstatus.db.getPersonOversiktStatusList
import no.nav.syfo.personstatus.db.lagreBrukerKnytningPaEnhet
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
import java.time.*
import java.time.temporal.ChronoUnit
import java.util.UUID

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

            val personoppgavehendelseService = PersonoppgavehendelseService(database)
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
            val kafkaDialogmotekandidatEndringStoppunktConsumerDelayPassedRecord =
                dialogmotekandidatEndringConsumerRecord(
                    kafkaDialogmotekandidatEndring = kafkaDialogmotekandidatEndringStoppunktDelayPassed,
                )
            val kafkaDialogmotekandidatEndringStoppunktHistoric = generateKafkaDialogmotekandidatEndringStoppunkt(
                personIdent = ARBEIDSTAKER_FNR,
                createdAt = nowUTC().minusDays(365)
            )
            val kafkaDialogmotekandidatEndringStoppunktConsumerHistoricRecord = dialogmotekandidatEndringConsumerRecord(
                kafkaDialogmotekandidatEndring = kafkaDialogmotekandidatEndringStoppunktHistoric,
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
                        OversikthendelseType.MOTEBEHOV_SVAR_MOTTATT.name,
                    )
                    database.connection.use {
                        personoppgavehendelseService.processPersonoppgavehendelse(
                            connection = it,
                            kPersonoppgavehendelse = oversiktHendelse,
                            callId = UUID.randomUUID().toString(),
                        )
                        it.commit()
                    }

                    database.setTildeltEnhet(
                        ident = PersonIdent(ARBEIDSTAKER_FNR),
                        enhet = NAV_ENHET,
                    )

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
                    val oversiktHendelse = KPersonoppgavehendelse(
                        ARBEIDSTAKER_FNR,
                        OversikthendelseType.MOTEBEHOV_SVAR_MOTTATT.name,
                    )
                    database.connection.use {
                        personoppgavehendelseService.processPersonoppgavehendelse(
                            connection = it,
                            kPersonoppgavehendelse = oversiktHendelse,
                            callId = UUID.randomUUID().toString(),
                        )
                        it.commit()
                    }
                    val oversiktHendelseNy = KPersonoppgavehendelse(
                        ARBEIDSTAKER_FNR,
                        OversikthendelseType.MOTEBEHOV_SVAR_BEHANDLET.name,
                    )
                    database.connection.use {
                        personoppgavehendelseService.processPersonoppgavehendelse(
                            connection = it,
                            kPersonoppgavehendelse = oversiktHendelseNy,
                            callId = UUID.randomUUID().toString(),
                        )
                        it.commit()
                    }

                    database.setTildeltEnhet(
                        ident = PersonIdent(ARBEIDSTAKER_FNR),
                        enhet = NAV_ENHET,
                    )

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
                        OversikthendelseType.MOTEBEHOV_SVAR_MOTTATT.name,
                    )
                    val oversiktHendelseOPLPSBistandMottatt = KPersonoppgavehendelse(
                        ARBEIDSTAKER_FNR,
                        OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_MOTTATT.name,
                    )
                    val dialogmotesvarMottatt = KPersonoppgavehendelse(
                        ARBEIDSTAKER_FNR,
                        OversikthendelseType.DIALOGMOTESVAR_MOTTATT.name,
                    )
                    database.connection.use {
                        personoppgavehendelseService.processPersonoppgavehendelse(
                            connection = it,
                            kPersonoppgavehendelse = oversiktHendelse,
                            callId = UUID.randomUUID().toString(),
                        )
                        personoppgavehendelseService.processPersonoppgavehendelse(
                            connection = it,
                            kPersonoppgavehendelse = oversiktHendelseOPLPSBistandMottatt,
                            callId = UUID.randomUUID().toString(),
                        )
                        personoppgavehendelseService.processPersonoppgavehendelse(
                            connection = it,
                            kPersonoppgavehendelse = dialogmotesvarMottatt,
                            callId = UUID.randomUUID().toString(),
                        )
                        it.commit()
                    }

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
                            objectMapper.readValue<List<PersonOversiktStatusDTO>>(response.content!!).filter {
                                it.fnr == ARBEIDSTAKER_FNR
                            }.first()
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

                it("should return list of PersonOversiktStatus, if there is a person with a relevant active Oppfolgingstilfelle, and person is DIALOGMOTEKANDIDAT and delay of 7 days has passed") {
                    kafkaOppfolgingstilfellePersonService.pollAndProcessRecords(
                        kafkaConsumer = mockKafkaConsumerOppfolgingstilfellePerson,
                    )
                    kafkaDialogmotekandidatEndringService.pollAndProcessRecords(
                        kafkaConsumer = mockKafkaConsumerDialogmotekandidatEndring,
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
                        personOversiktStatus.shouldNotBeNull()
                        personOversiktStatus.veilederIdent shouldBeEqualTo null
                        personOversiktStatus.fnr shouldBeEqualTo kafkaDialogmotekandidatEndringStoppunktDelayPassed.personIdentNumber
                        personOversiktStatus.enhet shouldBeEqualTo behandlendeEnhetDTO().enhetId
                        personOversiktStatus.motebehovUbehandlet.shouldBeNull()
                        personOversiktStatus.oppfolgingsplanLPSBistandUbehandlet.shouldBeNull()
                        personOversiktStatus.dialogmotesvarUbehandlet shouldBeEqualTo false
                        personOversiktStatus.dialogmotekandidat shouldBeEqualTo true
                        personOversiktStatus.motestatus.shouldBeNull()
                    }
                }

                it("should not return list of PersonOversiktStatus, if there is a person with a relevant active Oppfolgingstilfelle, and person is historic DIALOGMOTEKANDIDAT") {
                    every { mockKafkaConsumerDialogmotekandidatEndring.poll(any<Duration>()) } returns ConsumerRecords(
                        mapOf(
                            dialogmoteKandidatTopicPartition to listOf(
                                kafkaDialogmotekandidatEndringStoppunktConsumerHistoricRecord,
                            )
                        )
                    )
                    kafkaOppfolgingstilfellePersonService.pollAndProcessRecords(
                        kafkaConsumer = mockKafkaConsumerOppfolgingstilfellePerson,
                    )
                    kafkaDialogmotekandidatEndringService.pollAndProcessRecords(
                        kafkaConsumer = mockKafkaConsumerDialogmotekandidatEndring,
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

                it("should not return list of PersonOversiktStatus, if there is a person with a relevant active Oppfolgingstilfelle, and person is DIALOGMOTEKANDIDAT and delay of 7 days has not passed") {
                    val kafkaDialogmotekandidatEndringStoppunktDelayNotPassed =
                        generateKafkaDialogmotekandidatEndringStoppunkt(
                            personIdent = ARBEIDSTAKER_FNR,
                            createdAt = nowUTC().minusDays(6),
                        )
                    val kafkaDialogmotekandidatEndringStoppunktConsumerDelayNotPassedRecord =
                        dialogmotekandidatEndringConsumerRecord(
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

                it("should return list of PersonOversiktStatus, if MOTEBEHOV_SVAR_MOTTATT and if there is a person with 2 relevant active Oppfolgingstilfelle with different virksomhetsnummer") {
                    kafkaOppfolgingstilfellePersonService.pollAndProcessRecords(
                        kafkaConsumer = mockKafkaConsumerOppfolgingstilfellePerson,
                    )
                    val oversiktHendelse = KPersonoppgavehendelse(
                        ARBEIDSTAKER_FNR,
                        OversikthendelseType.MOTEBEHOV_SVAR_MOTTATT.name,
                    )
                    database.connection.use {
                        personoppgavehendelseService.processPersonoppgavehendelse(
                            connection = it,
                            kPersonoppgavehendelse = oversiktHendelse,
                            callId = UUID.randomUUID().toString(),
                        )
                        it.commit()
                    }

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
                        OversikthendelseType.MOTEBEHOV_SVAR_MOTTATT.name,
                    )
                    val oversiktHendelseOPLPSBistandMottatt = KPersonoppgavehendelse(
                        ARBEIDSTAKER_FNR,
                        OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_MOTTATT.name,
                    )
                    database.connection.use {
                        personoppgavehendelseService.processPersonoppgavehendelse(
                            connection = it,
                            kPersonoppgavehendelse = oversiktHendelse,
                            callId = UUID.randomUUID().toString(),
                        )
                        personoppgavehendelseService.processPersonoppgavehendelse(
                            connection = it,
                            kPersonoppgavehendelse = oversiktHendelseOPLPSBistandMottatt,
                            callId = UUID.randomUUID().toString(),
                        )
                        it.commit()
                    }

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
                        OversikthendelseType.MOTEBEHOV_SVAR_MOTTATT.name,
                    )
                    val oversiktHendelseOPLPSBistandMottatt = KPersonoppgavehendelse(
                        ARBEIDSTAKER_FNR,
                        OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_MOTTATT.name,
                    )
                    database.connection.use {
                        personoppgavehendelseService.processPersonoppgavehendelse(
                            connection = it,
                            kPersonoppgavehendelse = oversiktHendelse,
                            callId = UUID.randomUUID().toString(),
                        )
                        personoppgavehendelseService.processPersonoppgavehendelse(
                            connection = it,
                            kPersonoppgavehendelse = oversiktHendelseOPLPSBistandMottatt,
                            callId = UUID.randomUUID().toString(),
                        )
                        it.commit()
                    }

                    runBlocking {
                        personOppfolgingstilfelleVirksomhetnavnCronjob.runJob()
                    }

                    database.setTildeltEnhet(
                        ident = PersonIdent(ARBEIDSTAKER_FNR),
                        enhet = NAV_ENHET,
                    )

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
                        personOversiktStatus.shouldNotBeNull()
                        personOversiktStatus.veilederIdent shouldBeEqualTo null
                        personOversiktStatus.fnr shouldBeEqualTo kafkaDialogmoteStatusendring.getPersonIdent()
                        personOversiktStatus.enhet shouldBeEqualTo behandlendeEnhetDTO().enhetId
                        personOversiktStatus.motebehovUbehandlet.shouldBeNull()
                        personOversiktStatus.oppfolgingsplanLPSBistandUbehandlet.shouldBeNull()
                        personOversiktStatus.dialogmotesvarUbehandlet shouldBeEqualTo false
                        personOversiktStatus.dialogmotekandidat shouldBeEqualTo true
                        personOversiktStatus.motestatus shouldBeEqualTo kafkaDialogmoteStatusendring.getStatusEndringType()
                    }
                }

                it("should return Person, no Oppfolgingstilfelle, and then OPPFOLGINGSPLANLPS_BISTAND_MOTTATT") {
                    val oversiktHendelseOPLPSBistandMottatt = KPersonoppgavehendelse(
                        ARBEIDSTAKER_FNR,
                        OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_MOTTATT.name,
                    )
                    database.connection.use {
                        personoppgavehendelseService.processPersonoppgavehendelse(
                            connection = it,
                            kPersonoppgavehendelse = oversiktHendelseOPLPSBistandMottatt,
                            callId = UUID.randomUUID().toString(),
                        )
                        it.commit()
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
                        OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_MOTTATT.name,
                    )
                    database.connection.use {
                        personoppgavehendelseService.processPersonoppgavehendelse(
                            connection = it,
                            kPersonoppgavehendelse = oversiktHendelseOPLPSBistandMottatt,
                            callId = UUID.randomUUID().toString(),
                        )
                        it.commit()
                    }

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
                        OversikthendelseType.MOTEBEHOV_SVAR_BEHANDLET.name,
                    )
                    val oversiktHendelseOPLPSBistandBehandlet = KPersonoppgavehendelse(
                        ARBEIDSTAKER_FNR,
                        OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_BEHANDLET.name,
                    )
                    database.connection.use {
                        personoppgavehendelseService.processPersonoppgavehendelse(
                            connection = it,
                            kPersonoppgavehendelse = oversiktHendelseMotebehovSvarBehandlet,
                            callId = UUID.randomUUID().toString(),
                        )
                        personoppgavehendelseService.processPersonoppgavehendelse(
                            connection = it,
                            kPersonoppgavehendelse = oversiktHendelseOPLPSBistandBehandlet,
                            callId = UUID.randomUUID().toString(),
                        )
                        it.commit()
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

                it("return person with dialogmotesvar_ubehandlet true") {
                    val oversikthendelseDialogmotesvarMottatt = KPersonoppgavehendelse(
                        ARBEIDSTAKER_FNR,
                        OversikthendelseType.DIALOGMOTESVAR_MOTTATT.name,
                    )
                    database.connection.use {
                        personoppgavehendelseService.processPersonoppgavehendelse(
                            connection = it,
                            kPersonoppgavehendelse = oversikthendelseDialogmotesvarMottatt,
                            callId = UUID.randomUUID().toString(),
                        )
                        it.commit()
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
                        personOversiktStatus.fnr shouldBeEqualTo oversikthendelseDialogmotesvarMottatt.personident
                        personOversiktStatus.enhet shouldBeEqualTo behandlendeEnhetDTO().enhetId
                        personOversiktStatus.dialogmotesvarUbehandlet shouldBeEqualTo true
                    }
                }

                it("return person with aktivitetskrav status NY created this tilfelle") {
                    kafkaOppfolgingstilfellePersonService.pollAndProcessRecords(
                        kafkaConsumer = mockKafkaConsumerOppfolgingstilfellePerson,
                    )
                    val personIdent = PersonIdent(ARBEIDSTAKER_FNR)
                    val updatedAt = OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS)
                    val stoppunkt = LocalDate.now()
                    val aktivitetskrav = Aktivitetskrav(
                        personIdent = personIdent,
                        status = AktivitetskravStatus.NY,
                        sistVurdert = updatedAt,
                        stoppunkt = stoppunkt,
                    )
                    database.connection.use { connection ->
                        persistAktivitetskrav(
                            connection = connection,
                            aktivitetskrav = aktivitetskrav,
                        )
                        connection.commit()
                    }
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
                        personOversiktStatus.fnr shouldBeEqualTo personIdent.value
                        personOversiktStatus.enhet shouldBeEqualTo behandlendeEnhetDTO().enhetId
                        personOversiktStatus.aktivitetskrav shouldBeEqualTo AktivitetskravStatus.NY.name
                        personOversiktStatus.aktivitetskravSistVurdert shouldBeEqualTo updatedAt.toLocalDateTimeOslo()
                        personOversiktStatus.aktivitetskravStoppunkt shouldBeEqualTo stoppunkt
                    }
                }

                it("return person with aktivitetskrav status AVVENT") {
                    kafkaOppfolgingstilfellePersonService.pollAndProcessRecords(
                        kafkaConsumer = mockKafkaConsumerOppfolgingstilfellePerson,
                    )
                    val personIdent = PersonIdent(ARBEIDSTAKER_FNR)
                    val updatedAt = OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS)
                    val stoppunkt = LocalDate.now()
                    val aktivitetskrav = Aktivitetskrav(
                        personIdent = personIdent,
                        status = AktivitetskravStatus.AVVENT,
                        sistVurdert = updatedAt,
                        stoppunkt = stoppunkt,
                    )
                    database.connection.use { connection ->
                        persistAktivitetskrav(
                            connection = connection,
                            aktivitetskrav = aktivitetskrav,
                        )
                        connection.commit()
                    }
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
                        personOversiktStatus.fnr shouldBeEqualTo personIdent.value
                        personOversiktStatus.enhet shouldBeEqualTo behandlendeEnhetDTO().enhetId
                        personOversiktStatus.aktivitetskrav shouldBeEqualTo AktivitetskravStatus.AVVENT.name
                        personOversiktStatus.aktivitetskravSistVurdert shouldBeEqualTo updatedAt.toLocalDateTimeOslo()
                        personOversiktStatus.aktivitetskravStoppunkt shouldBeEqualTo stoppunkt
                    }
                }

                it("return no content when aktivitetskrav has status AUTOMATISK_OPPFYLT") {
                    kafkaOppfolgingstilfellePersonService.pollAndProcessRecords(
                        kafkaConsumer = mockKafkaConsumerOppfolgingstilfellePerson,
                    )
                    val personIdent = PersonIdent(ARBEIDSTAKER_FNR)
                    val aktivitetskrav = Aktivitetskrav(
                        personIdent = personIdent,
                        status = AktivitetskravStatus.AUTOMATISK_OPPFYLT,
                        sistVurdert = OffsetDateTime.now(),
                        stoppunkt = LocalDate.now(),
                    )
                    database.connection.use { connection ->
                        persistAktivitetskrav(
                            connection = connection,
                            aktivitetskrav = aktivitetskrav,
                        )
                        connection.commit()
                    }
                    database.setTildeltEnhet(
                        ident = personIdent,
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

                it("return person with aktivitetskrav status NY created before the current tilfelle") {
                    kafkaOppfolgingstilfellePersonService.pollAndProcessRecords(
                        kafkaConsumer = mockKafkaConsumerOppfolgingstilfellePerson,
                    )
                    val tilfelleStart = kafkaOppfolgingstilfellePersonRelevant.oppfolgingstilfelleList[0].start
                    val stoppunkt = tilfelleStart.minusDays(10)
                    val updatedAt = OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS)
                    val personIdent = PersonIdent(ARBEIDSTAKER_FNR)
                    val aktivitetskrav = Aktivitetskrav(
                        personIdent = personIdent,
                        status = AktivitetskravStatus.NY,
                        sistVurdert = updatedAt,
                        stoppunkt = stoppunkt,
                    )
                    database.connection.use { connection ->
                        persistAktivitetskrav(
                            connection = connection,
                            aktivitetskrav = aktivitetskrav,
                        )
                        connection.commit()
                    }
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
                        personOversiktStatus.fnr shouldBeEqualTo personIdent.value
                        personOversiktStatus.enhet shouldBeEqualTo behandlendeEnhetDTO().enhetId
                        personOversiktStatus.aktivitetskrav shouldBeEqualTo AktivitetskravStatus.NY.name
                        personOversiktStatus.aktivitetskravSistVurdert shouldBeEqualTo updatedAt.toLocalDateTimeOslo()
                        personOversiktStatus.aktivitetskravStoppunkt shouldBeEqualTo stoppunkt
                    }
                }

                it("Should update name in database") {
                    val personIdent = PersonIdent(ARBEIDSTAKER_FNR)
                    val oversikthendelseDialogmotesvarMottatt = KPersonoppgavehendelse(
                        ARBEIDSTAKER_FNR,
                        OversikthendelseType.DIALOGMOTESVAR_MOTTATT.name,
                    )
                    database.connection.use {
                        personoppgavehendelseService.processPersonoppgavehendelse(
                            connection = it,
                            kPersonoppgavehendelse = oversikthendelseDialogmotesvarMottatt,
                            callId = UUID.randomUUID().toString(),
                        )
                        it.commit()
                    }

                    val tilknytning = VeilederBrukerKnytning(VEILEDER_ID, personIdent.value, NAV_ENHET)
                    database.lagreBrukerKnytningPaEnhet(tilknytning)
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
            }
        }
    }
})
