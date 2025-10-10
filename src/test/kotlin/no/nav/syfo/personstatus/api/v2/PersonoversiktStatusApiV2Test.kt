package no.nav.syfo.personstatus.api.v2

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.clearMocks
import io.mockk.every
import kotlinx.coroutines.runBlocking
import no.nav.syfo.dialogmotestatusendring.domain.DialogmoteStatusendringType
import no.nav.syfo.personstatus.api.v2.endpoints.personOversiktApiV2Path
import no.nav.syfo.personstatus.api.v2.model.PersonOversiktStatusDTO
import no.nav.syfo.personstatus.application.manglendemedvirkning.ManglendeMedvirkningVurderingType
import no.nav.syfo.personstatus.domain.*
import no.nav.syfo.personstatus.infrastructure.database.queries.createPersonOversiktStatus
import no.nav.syfo.personstatus.infrastructure.kafka.personoppgavehendelse.KPersonoppgavehendelse
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
import no.nav.syfo.util.nowUTC
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDate

class PersonoversiktStatusApiV2Test {
    val externalMockEnvironment = ExternalMockEnvironment.instance
    val database = externalMockEnvironment.database
    val personOversiktStatusRepository = externalMockEnvironment.personOversiktStatusRepository
    val internalMockEnvironment = InternalMockEnvironment.instance

    val personOppfolgingstilfelleVirksomhetnavnCronjob =
        internalMockEnvironment.personOppfolgingstilfelleVirksomhetnavnCronjob

    val personoversiktStatusService = internalMockEnvironment.personoversiktStatusService
    val oppfolgingstilfelleConsumer = TestKafkaModule.oppfolgingstilfelleConsumer

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
        oppfolgingstilfellePersonRecord = kafkaOppfolgingstilfellePersonRelevant,
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

    val validToken = generateJWT(
        audience = externalMockEnvironment.environment.azure.appClientId,
        issuer = externalMockEnvironment.wellKnownVeilederV2.issuer,
        navIdent = VEILEDER_ID,
    )

    val url = "$personOversiktApiV2Path/enhet/$NAV_ENHET"

    @BeforeEach
    fun setUp() {
        database.resetDatabase()

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

    @Test
    fun `Skal returnere status NoContent om det ikke er noen personer som er tilknyttet enhet`() {
        testApplication {
            val client = setupApiAndClient()
            val response = client.get(url) {
                bearerAuth(validToken)
            }
            assertEquals(HttpStatusCode.NoContent, response.status)
        }
    }

    @Test
    fun `Skal returnere status Unauthorized om token mangler`() {
        testApplication {
            val client = setupApiAndClient()
            val response = client.get(url) {}
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
    }

    @Test
    fun `Skal returnere status BadRequest om enhet ugyldig`() {
        testApplication {
            val client = setupApiAndClient()
            val response = client.get("$personOversiktApiV2Path/enhet/12345") {
                bearerAuth(validToken)
            }
            assertEquals(HttpStatusCode.BadRequest, response.status)
        }
    }

    @Test
    fun `Skal returnere NoContent med ubehandlet motebehovsvar og ikke har oppfolgingstilfelle`() {
        testApplication {
            val client = setupApiAndClient()
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

            val tilknytning = VeilederBrukerKnytning(VEILEDER_ID, ARBEIDSTAKER_FNR)
            personOversiktStatusRepository.lagreVeilederForBruker(tilknytning, VEILEDER_ID)

            val response = client.get(url) {
                bearerAuth(validToken)
            }
            assertEquals(HttpStatusCode.NoContent, response.status)
        }
    }

    @Test
    fun `Skal returnere NoContent, om alle personer i personoversikt er behandlet og ikke har oppfolgingstilfelle`() {
        testApplication {
            val client = setupApiAndClient()
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

            val tilknytning = VeilederBrukerKnytning(VEILEDER_ID, ARBEIDSTAKER_FNR)
            personOversiktStatusRepository.lagreVeilederForBruker(tilknytning, VEILEDER_ID)

            val response = client.get(url) {
                bearerAuth(validToken)
            }
            assertEquals(HttpStatusCode.NoContent, response.status)
        }
    }

    @Test
    fun `Should return NoContent, if there is a person with a relevant active Oppfolgingstilfelle, but neither MOTEBEHOV_SVAR_MOTTATT nor DIALOGMOTEKANDIDAT`() {
        testApplication {
            val client = setupApiAndClient()
            runBlocking {
                oppfolgingstilfelleConsumer.pollAndProcessRecords(kafkaConsumer = mockKafkaConsumerOppfolgingstilfellePerson)
            }
            runBlocking {
                personOppfolgingstilfelleVirksomhetnavnCronjob.runJob()
            }
            database.setTildeltEnhet(
                ident = PersonIdent(ARBEIDSTAKER_FNR),
                enhet = NAV_ENHET,
            )

            val response = client.get(url) {
                bearerAuth(validToken)
            }
            assertEquals(HttpStatusCode.NoContent, response.status)
        }
    }

    @Test
    fun `Should return list of PersonOversiktStatus, if MOTEBEHOV_SVAR_MOTTATT and OPPFOLGINGSPLANLPS_BISTAND_MOTTATT and DIALOGMOTESVAR_MOTTATT, and there is a person with a relevant active Oppfolgingstilfelle`() {
        testApplication {
            val client = setupApiAndClient()
            runBlocking {
                oppfolgingstilfelleConsumer.pollAndProcessRecords(kafkaConsumer = mockKafkaConsumerOppfolgingstilfellePerson)
            }
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

            val response = client.get(url) {
                bearerAuth(validToken)
            }
            assertEquals(HttpStatusCode.OK, response.status)
            val personOversiktStatus = response.body<List<PersonOversiktStatusDTO>>().first {
                it.fnr == ARBEIDSTAKER_FNR
            }
            assertNull(personOversiktStatus.veilederIdent)
            assertEquals(ARBEIDSTAKER_FNR, personOversiktStatus.fnr)
            assertEquals(behandlendeEnhetDTO.geografiskEnhet.enhetId, personOversiktStatus.enhet)
            assertTrue(personOversiktStatus.motebehovUbehandlet!!)
            assertTrue(personOversiktStatus.oppfolgingsplanLPSBistandUbehandlet!!)
            assertTrue(personOversiktStatus.dialogmotesvarUbehandlet)
            assertNull(personOversiktStatus.dialogmotekandidat)
            assertNull(personOversiktStatus.motestatus)

            assertNotNull(personOversiktStatus.latestOppfolgingstilfelle)
            checkPersonOppfolgingstilfelleDTO(
                personOppfolgingstilfelleDTO = personOversiktStatus.latestOppfolgingstilfelle,
                oppfolgingstilfellePersonRecord = kafkaOppfolgingstilfellePersonRelevant,
            )
        }
    }

    @Test
    fun `Should return list of PersonOversiktStatus, if MOTEBEHOV_SVAR_MOTTATT and if there is a person with 2 relevant active Oppfolgingstilfelle with different virksomhetsnummer`() {
        testApplication {
            val client = setupApiAndClient()
            runBlocking {
                oppfolgingstilfelleConsumer.pollAndProcessRecords(kafkaConsumer = mockKafkaConsumerOppfolgingstilfellePerson)
            }
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
            val response = client.get(url) {
                bearerAuth(validToken)
            }
            assertEquals(HttpStatusCode.OK, response.status)
            val personOversiktStatus = response.body<List<PersonOversiktStatusDTO>>().first()
            assertNull(personOversiktStatus.veilederIdent)
            assertEquals(oversiktHendelse.personident, personOversiktStatus.fnr)
            assertEquals(getIdentName(), personOversiktStatus.navn)
            assertEquals(behandlendeEnhetDTO.geografiskEnhet.enhetId, personOversiktStatus.enhet)
            assertTrue(personOversiktStatus.motebehovUbehandlet!!)
            assertNull(personOversiktStatus.oppfolgingsplanLPSBistandUbehandlet)
            assertFalse(personOversiktStatus.dialogmotesvarUbehandlet)
            assertNull(personOversiktStatus.dialogmotekandidat)
            assertNull(personOversiktStatus.motestatus)

            assertNotNull(personOversiktStatus.latestOppfolgingstilfelle)
            checkPersonOppfolgingstilfelleDTO(
                personOppfolgingstilfelleDTO = personOversiktStatus.latestOppfolgingstilfelle,
                oppfolgingstilfellePersonRecord = kafkaOppfolgingstilfellePersonRelevant,
            )
        }
    }

    @Test
    fun `Should return Person, with MOTEBEHOV_SVAR_MOTTATT, and then receives Oppfolgingstilfelle and the OPPFOLGINGSPLANLPS_BISTAND_MOTTATT`() {
        testApplication {
            val client = setupApiAndClient()
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
                oppfolgingstilfelleConsumer.pollAndProcessRecords(kafkaConsumer = mockKafkaConsumerOppfolgingstilfellePerson)
                personOppfolgingstilfelleVirksomhetnavnCronjob.runJob()
            }

            database.setTildeltEnhet(
                ident = PersonIdent(ARBEIDSTAKER_FNR),
                enhet = NAV_ENHET,
            )

            val tilknytning = VeilederBrukerKnytning(VEILEDER_ID, ARBEIDSTAKER_FNR)
            personOversiktStatusRepository.lagreVeilederForBruker(tilknytning, VEILEDER_ID)
            val response = client.get(url) {
                bearerAuth(validToken)
            }
            assertEquals(HttpStatusCode.OK, response.status)
            val personOversiktStatus = response.body<List<PersonOversiktStatusDTO>>().first()
            assertEquals(tilknytning.veilederIdent, personOversiktStatus.veilederIdent)
            assertEquals(oversiktHendelse.personident, personOversiktStatus.fnr)
            assertEquals(getIdentName(), personOversiktStatus.navn)
            assertEquals(behandlendeEnhetDTO.geografiskEnhet.enhetId, personOversiktStatus.enhet)
            assertTrue(personOversiktStatus.motebehovUbehandlet!!)
            assertTrue(personOversiktStatus.oppfolgingsplanLPSBistandUbehandlet!!)
            assertFalse(personOversiktStatus.dialogmotesvarUbehandlet)
            assertNull(personOversiktStatus.dialogmotekandidat)
            assertNull(personOversiktStatus.motestatus)

            assertNotNull(personOversiktStatus.latestOppfolgingstilfelle)
            checkPersonOppfolgingstilfelleDTO(
                personOppfolgingstilfelleDTO = personOversiktStatus.latestOppfolgingstilfelle,
                oppfolgingstilfellePersonRecord = kafkaOppfolgingstilfellePersonRelevant,
            )
        }
    }

    @Test
    fun `Should return Person, receives Oppfolgingstilfelle, and then MOTEBEHOV_SVAR_MOTTATT and OPPFOLGINGSPLANLPS_BISTAND_MOTTATT`() {
        testApplication {
            val client = setupApiAndClient()
            runBlocking {
                oppfolgingstilfelleConsumer.pollAndProcessRecords(kafkaConsumer = mockKafkaConsumerOppfolgingstilfellePerson)
            }

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

            val tilknytning = VeilederBrukerKnytning(VEILEDER_ID, ARBEIDSTAKER_FNR)
            personOversiktStatusRepository.lagreVeilederForBruker(tilknytning, VEILEDER_ID)
            val response = client.get(url) {
                bearerAuth(validToken)
            }
            assertEquals(HttpStatusCode.OK, response.status)
            val personOversiktStatus = response.body<List<PersonOversiktStatusDTO>>().first()
            assertEquals(tilknytning.veilederIdent, personOversiktStatus.veilederIdent)
            assertEquals(oversiktHendelse.personident, personOversiktStatus.fnr)
            assertEquals(getIdentName(), personOversiktStatus.navn)
            assertEquals(behandlendeEnhetDTO.geografiskEnhet.enhetId, personOversiktStatus.enhet)
            assertTrue(personOversiktStatus.motebehovUbehandlet!!)
            assertTrue(personOversiktStatus.oppfolgingsplanLPSBistandUbehandlet!!)
            assertFalse(personOversiktStatus.dialogmotesvarUbehandlet)
            assertNull(personOversiktStatus.dialogmotekandidat)
            assertNull(personOversiktStatus.motestatus)

            assertNotNull(personOversiktStatus.latestOppfolgingstilfelle)
            checkPersonOppfolgingstilfelleDTO(
                personOppfolgingstilfelleDTO = personOversiktStatus.latestOppfolgingstilfelle,
                oppfolgingstilfellePersonRecord = kafkaOppfolgingstilfellePersonRelevant,
            )
        }
    }

    @Test
    fun `Should return Person, no Oppfolgingstilfelle, and then OPPFOLGINGSPLANLPS_BISTAND_MOTTATT`() {
        testApplication {
            val client = setupApiAndClient()
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
            val response = client.get(url) {
                bearerAuth(validToken)
            }
            assertEquals(HttpStatusCode.OK, response.status)
            val personOversiktStatus = response.body<List<PersonOversiktStatusDTO>>().first()
            assertNull(personOversiktStatus.veilederIdent)
            assertEquals(oversiktHendelseOPLPSBistandMottatt.personident, personOversiktStatus.fnr)
            assertEquals(getIdentName(), personOversiktStatus.navn)
            assertEquals(behandlendeEnhetDTO.geografiskEnhet.enhetId, personOversiktStatus.enhet)
            assertNull(personOversiktStatus.motebehovUbehandlet)
            assertTrue(personOversiktStatus.oppfolgingsplanLPSBistandUbehandlet!!)
            assertFalse(personOversiktStatus.dialogmotesvarUbehandlet)
            assertNull(personOversiktStatus.dialogmotekandidat)
            assertNull(personOversiktStatus.motestatus)

            assertNull(personOversiktStatus.latestOppfolgingstilfelle)
        }
    }

    @Test
    fun `Should return Person with no Oppfolgingstilfelle and no Navn for OPPFOLGINGSPLANLPS_BISTAND_MOTTATT`() {
        testApplication {
            val client = setupApiAndClient()
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
            val response = client.get(url) {
                bearerAuth(validToken)
            }
            assertEquals(HttpStatusCode.OK, response.status)
            val personOversiktStatus = response.body<List<PersonOversiktStatusDTO>>().first()
            assertNull(personOversiktStatus.veilederIdent)
            assertEquals(oversiktHendelseOPLPSBistandMottatt.personident, personOversiktStatus.fnr)
            assertEquals("", personOversiktStatus.navn)
            assertEquals(behandlendeEnhetDTO.geografiskEnhet.enhetId, personOversiktStatus.enhet)
            assertNull(personOversiktStatus.motebehovUbehandlet)
            assertTrue(personOversiktStatus.oppfolgingsplanLPSBistandUbehandlet!!)
            assertFalse(personOversiktStatus.dialogmotesvarUbehandlet)
            assertNull(personOversiktStatus.dialogmotekandidat)
            assertNull(personOversiktStatus.motestatus)
            assertNull(personOversiktStatus.latestOppfolgingstilfelle)
        }
    }

    @Test
    fun `Should not return Person if OversikthendelseType for Behandling is received without existing Person`() {
        testApplication {
            val client = setupApiAndClient()
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
            val response = client.get(url) {
                bearerAuth(validToken)
            }
            assertEquals(HttpStatusCode.NoContent, response.status)
        }
    }

    @Test
    fun `Return person with dialogmotesvar_ubehandlet true`() {
        testApplication {
            val client = setupApiAndClient()
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
            val response = client.get(url) {
                bearerAuth(validToken)
            }
            assertEquals(HttpStatusCode.OK, response.status)
            val personOversiktStatus = response.body<List<PersonOversiktStatusDTO>>().first()
            assertEquals(oversikthendelseDialogmotesvarMottatt.personident, personOversiktStatus.fnr)
            assertEquals(behandlendeEnhetDTO.geografiskEnhet.enhetId, personOversiktStatus.enhet)
            assertTrue(personOversiktStatus.dialogmotesvarUbehandlet)
        }
    }

    @Test
    fun `Return person with friskmelding til arbeidsformidling starting tomorrow`() {
        testApplication {
            val client = setupApiAndClient()
            val personident = PersonIdent(ARBEIDSTAKER_FNR)
            val tomorrow = LocalDate.now().plusDays(1)
            with(database) {
                createPersonOversiktStatus(PersonOversiktStatus(fnr = personident.value))
                setTildeltEnhet(personident, NAV_ENHET)
                setFriskmeldingTilArbeidsformidlingFom(personident, tomorrow)
            }
            val response = client.get(url) {
                bearerAuth(validToken)
            }
            assertEquals(HttpStatusCode.OK, response.status)
            val personOversiktStatus = response.body<List<PersonOversiktStatusDTO>>().first()
            assertEquals(personident.value, personOversiktStatus.fnr)
            assertEquals(behandlendeEnhetDTO.geografiskEnhet.enhetId, personOversiktStatus.enhet)
            assertEquals(tomorrow, personOversiktStatus.friskmeldingTilArbeidsformidlingFom!!)
        }
    }

    @Test
    fun `Return person with friskmelding til arbeidsformidling starting today`() {
        testApplication {
            val client = setupApiAndClient()
            val personident = PersonIdent(ARBEIDSTAKER_FNR)
            val today = LocalDate.now()
            with(database) {
                createPersonOversiktStatus(PersonOversiktStatus(fnr = personident.value))
                setTildeltEnhet(personident, NAV_ENHET)
                setFriskmeldingTilArbeidsformidlingFom(personident, today)
            }
            val response = client.get(url) {
                bearerAuth(validToken)
            }
            assertEquals(HttpStatusCode.OK, response.status)
            val personOversiktStatus = response.body<List<PersonOversiktStatusDTO>>().first()
            assertEquals(personident.value, personOversiktStatus.fnr)
            assertEquals(behandlendeEnhetDTO.geografiskEnhet.enhetId, personOversiktStatus.enhet)
            assertEquals(today, personOversiktStatus.friskmeldingTilArbeidsformidlingFom!!)
        }
    }

    @Test
    fun `Return person with friskmelding til arbeidsformidling starting yesterday`() {
        testApplication {
            val client = setupApiAndClient()
            val personident = PersonIdent(ARBEIDSTAKER_FNR)
            val yesterday = LocalDate.now().minusDays(1)
            with(database) {
                createPersonOversiktStatus(PersonOversiktStatus(fnr = personident.value))
                setTildeltEnhet(personident, NAV_ENHET)
                setFriskmeldingTilArbeidsformidlingFom(personident, yesterday)
            }
            val response = client.get(url) {
                bearerAuth(validToken)
            }
            assertEquals(HttpStatusCode.OK, response.status)
            val personOversiktStatus = response.body<List<PersonOversiktStatusDTO>>().first()
            assertEquals(personident.value, personOversiktStatus.fnr)
            assertEquals(behandlendeEnhetDTO.geografiskEnhet.enhetId, personOversiktStatus.enhet)
            assertEquals(yesterday, personOversiktStatus.friskmeldingTilArbeidsformidlingFom!!)
        }
    }

    @Test
    fun `Return person when trenger_oppfolging true`() {
        testApplication {
            val client = setupApiAndClient()
            val personident = ARBEIDSTAKER_FNR
            val personoversiktStatus = PersonOversiktStatus(
                fnr = personident,
            ).copy(isAktivOppfolgingsoppgave = true)

            database.createPersonOversiktStatus(personoversiktStatus)

            database.setTildeltEnhet(
                ident = PersonIdent(ARBEIDSTAKER_FNR),
                enhet = NAV_ENHET,
            )
            val response = client.get(url) {
                bearerAuth(validToken)
            }
            assertEquals(HttpStatusCode.OK, response.status)
            val personOversiktStatus = response.body<List<PersonOversiktStatusDTO>>().first()
            assertEquals(personident, personOversiktStatus.fnr)
            assertEquals(behandlendeEnhetDTO.geografiskEnhet.enhetId, personOversiktStatus.enhet)
            assertNotNull(personOversiktStatus.oppfolgingsoppgave)
            assertEquals("FOLG_OPP_ETTER_NESTE_SYKMELDING", personOversiktStatus.oppfolgingsoppgave?.oppfolgingsgrunn)
            assertEquals("En tekst", personOversiktStatus.oppfolgingsoppgave?.tekst)
            assertNotNull(personOversiktStatus.oppfolgingsoppgave?.frist)
        }
    }

    @Test
    fun `Return no person when trenger_oppfolging false`() {
        testApplication {
            val client = setupApiAndClient()
            val personident = ARBEIDSTAKER_FNR
            val personoversiktStatus = PersonOversiktStatus(
                fnr = personident,
            )

            database.createPersonOversiktStatus(personoversiktStatus)

            database.setTildeltEnhet(
                ident = PersonIdent(ARBEIDSTAKER_FNR),
                enhet = NAV_ENHET,
            )
            val response = client.get(url) {
                bearerAuth(validToken)
            }
            assertEquals(HttpStatusCode.NoContent, response.status)
        }
    }

    @Test
    fun `Return person with behandler_ber_om_bistand_ubehandlet true when oppgave mottatt`() {
        testApplication {
            val client = setupApiAndClient()
            val personoversiktStatus = PersonOversiktStatus(
                fnr = ARBEIDSTAKER_FNR
            ).applyOversikthendelse(OversikthendelseType.BEHANDLER_BER_OM_BISTAND_MOTTATT)

            database.createPersonOversiktStatus(personoversiktStatus)

            database.setTildeltEnhet(
                ident = PersonIdent(ARBEIDSTAKER_FNR),
                enhet = NAV_ENHET,
            )
            val response = client.get(url) {
                bearerAuth(validToken)
            }
            assertEquals(HttpStatusCode.OK, response.status)
            val personOversiktStatus = response.body<List<PersonOversiktStatusDTO>>().first()
            assertEquals(ARBEIDSTAKER_FNR, personOversiktStatus.fnr)
            assertEquals(behandlendeEnhetDTO.geografiskEnhet.enhetId, personOversiktStatus.enhet)
            assertTrue(personOversiktStatus.behandlerBerOmBistandUbehandlet)
        }
    }

    @Test
    fun `Return no person when behandler_ber_om_bistand-oppgave behandlet`() {
        testApplication {
            val client = setupApiAndClient()
            val personoversiktStatus = PersonOversiktStatus(
                fnr = ARBEIDSTAKER_FNR
            ).applyOversikthendelse(OversikthendelseType.BEHANDLER_BER_OM_BISTAND_BEHANDLET)

            database.createPersonOversiktStatus(personoversiktStatus)

            database.setTildeltEnhet(
                ident = PersonIdent(ARBEIDSTAKER_FNR),
                enhet = NAV_ENHET,
            )
            val response = client.get(url) {
                bearerAuth(validToken)
            }
            assertEquals(HttpStatusCode.NoContent, response.status)
        }
    }

    @Test
    fun `Return person with correct varighetUker based on antallSykedager`() {
        testApplication {
            val client = setupApiAndClient()
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
            val response = client.get(url) {
                bearerAuth(validToken)
            }
            assertEquals(HttpStatusCode.OK, response.status)
            val personOversiktStatus = response.body<List<PersonOversiktStatusDTO>>().first()
            assertEquals(ARBEIDSTAKER_FNR, personOversiktStatus.fnr)
            assertEquals(2, personOversiktStatus.latestOppfolgingstilfelle?.varighetUker)
        }
    }

    @Test
    fun `Return person with correct virksomhetslist`() {
        testApplication {
            val client = setupApiAndClient()
            val virksomhetList = listOf(
                generateOppfolgingstilfelleVirksomhet(
                    virksomhetsnummer = Virksomhetsnummer("123456789"),
                    virksomhetsnavn = "Virksomhet AS",
                ),
                generateOppfolgingstilfelleVirksomhet(
                    virksomhetsnummer = Virksomhetsnummer("123456000"),
                    virksomhetsnavn = null,
                ),
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
            val response = client.get(url) {
                bearerAuth(validToken)
            }
            assertEquals(HttpStatusCode.OK, response.status)
            val personOversiktStatus = response.body<List<PersonOversiktStatusDTO>>().first()
            assertEquals(ARBEIDSTAKER_FNR, personOversiktStatus.fnr)
            assertEquals(
                "Virksomhet AS",
                personOversiktStatus.latestOppfolgingstilfelle?.virksomhetList?.get(0)?.virksomhetsnavn
            )
            assertNull(personOversiktStatus.latestOppfolgingstilfelle?.virksomhetList?.get(1)?.virksomhetsnavn)
        }
    }

    @Test
    fun `Return person when isAktivSenOppfolgingKandidat true`() {
        testApplication {
            val client = setupApiAndClient()
            val personident = ARBEIDSTAKER_FNR
            val personoversiktStatus = PersonOversiktStatus(
                fnr = personident,
            ).copy(isAktivSenOppfolgingKandidat = true)

            database.createPersonOversiktStatus(personoversiktStatus)

            database.setTildeltEnhet(
                ident = PersonIdent(ARBEIDSTAKER_FNR),
                enhet = NAV_ENHET,
            )
            val response = client.get(url) {
                bearerAuth(validToken)
            }
            assertEquals(HttpStatusCode.OK, response.status)
            val personOversiktStatus = response.body<List<PersonOversiktStatusDTO>>().first()
            assertEquals(personident, personOversiktStatus.fnr)
            assertEquals(NAV_ENHET, personOversiktStatus.enhet)
            assertNotNull(personOversiktStatus.senOppfolgingKandidat)
        }
    }

    @Test
    fun `Return no person when isAktivSenOppfolgingKandidat false`() {
        testApplication {
            val client = setupApiAndClient()
            val personident = ARBEIDSTAKER_FNR
            val personoversiktStatus = PersonOversiktStatus(fnr = personident)

            database.createPersonOversiktStatus(personoversiktStatus)
            database.setTildeltEnhet(
                ident = PersonIdent(ARBEIDSTAKER_FNR),
                enhet = NAV_ENHET,
            )
            val response = client.get(url) {
                bearerAuth(validToken)
            }
            assertEquals(HttpStatusCode.NoContent, response.status)
        }
    }

    @Nested
    @DisplayName("Arbeidsuforhetvurdering")
    inner class Arbeidsuforhetvurdering {

        @Test
        fun `Returns person with active arbeidsuforhetvurdering`() {
            testApplication {
                val client = setupApiAndClient()
                val newPersonOversiktStatus = PersonOversiktStatus(fnr = ARBEIDSTAKER_FNR)
                    .copy(isAktivArbeidsuforhetvurdering = true)
                database.connection.use { connection ->
                    connection.createPersonOversiktStatus(commit = true, personOversiktStatus = newPersonOversiktStatus)
                }
                database.setTildeltEnhet(
                    ident = PersonIdent(ARBEIDSTAKER_FNR),
                    enhet = NAV_ENHET,
                )
                val response = client.get(url) {
                    bearerAuth(validToken)
                }
                assertEquals(HttpStatusCode.OK, response.status)
                val personOversiktStatus = response.body<List<PersonOversiktStatusDTO>>().first()
                assertEquals(ARBEIDSTAKER_FNR, personOversiktStatus.fnr)
                assertNotNull(personOversiktStatus.arbeidsuforhetvurdering)
                assertNotNull(personOversiktStatus.arbeidsuforhetvurdering?.varsel)
                assertEquals(latestVurdering, personOversiktStatus.arbeidsuforhetvurdering?.createdAt)
            }
        }
    }

    @Nested
    @DisplayName("Manglendemedvirkning")
    inner class Manglendemedvirkning {

        @Test
        fun `Returns person with active manglendemedvirkning`() {
            testApplication {
                val client = setupApiAndClient()
                val newPersonOversiktStatus = PersonOversiktStatus(fnr = ARBEIDSTAKER_FNR)
                    .copy(isAktivManglendeMedvirkningVurdering = true)
                database.connection.use { connection ->
                    connection.createPersonOversiktStatus(commit = true, personOversiktStatus = newPersonOversiktStatus)
                }
                database.setTildeltEnhet(
                    ident = PersonIdent(ARBEIDSTAKER_FNR),
                    enhet = NAV_ENHET,
                )
                val response = client.get(url) {
                    bearerAuth(validToken)
                }
                assertEquals(HttpStatusCode.OK, response.status)
                val personOversiktStatus = response.body<List<PersonOversiktStatusDTO>>().first()
                assertEquals(ARBEIDSTAKER_FNR, personOversiktStatus.fnr)
                assertNotNull(personOversiktStatus.manglendeMedvirkning)
                assertNotNull(personOversiktStatus.manglendeMedvirkning?.varsel)
                assertEquals(
                    ManglendeMedvirkningVurderingType.FORHANDSVARSEL,
                    personOversiktStatus.manglendeMedvirkning?.vurderingType
                )
                assertEquals("begrunnelse", personOversiktStatus.manglendeMedvirkning?.begrunnelse)
                assertEquals(VEILEDER_ID, personOversiktStatus.manglendeMedvirkning?.veilederident)
            }
        }
    }

    @Nested
    @DisplayName("Aktivitetskrav")
    inner class Aktivitetskrav {

        @Test
        fun `Return person when isAktivAktivitetskravvurdering true`() {
            testApplication {
                val client = setupApiAndClient()
                val personident = ARBEIDSTAKER_FNR
                val personoversiktStatus = PersonOversiktStatus(
                    fnr = personident,
                ).copy(isAktivAktivitetskravvurdering = true)

                database.createPersonOversiktStatus(personoversiktStatus)

                database.setTildeltEnhet(
                    ident = PersonIdent(ARBEIDSTAKER_FNR),
                    enhet = NAV_ENHET,
                )
                val response = client.get(url) {
                    bearerAuth(validToken)
                }
                assertEquals(HttpStatusCode.OK, response.status)
                val personOversiktStatus = response.body<List<PersonOversiktStatusDTO>>().first()
                assertEquals(personident, personOversiktStatus.fnr)
                assertEquals(NAV_ENHET, personOversiktStatus.enhet)
                assertNotNull(personOversiktStatus.aktivitetskravvurdering)
            }
        }

        @Test
        fun `Return no person when isAktivAktivitetskravvurdering false`() {
            testApplication {
                val client = setupApiAndClient()
                val personident = ARBEIDSTAKER_FNR
                val personoversiktStatus = PersonOversiktStatus(fnr = personident)

                database.createPersonOversiktStatus(personoversiktStatus)
                database.setTildeltEnhet(
                    ident = PersonIdent(ARBEIDSTAKER_FNR),
                    enhet = NAV_ENHET,
                )
                val response = client.get(url) {
                    bearerAuth(validToken)
                }
                assertEquals(HttpStatusCode.NoContent, response.status)
            }
        }
    }

    @Nested
    @DisplayName("Kartleggingssporsmal")
    inner class Kartleggingssporsmal {

        @Test
        fun `Return person when isAktivKartleggingssporsmalVurdering true`() {
            testApplication {
                val client = setupApiAndClient()
                val personident = ARBEIDSTAKER_FNR
                val personoversiktStatus = PersonOversiktStatus(
                    fnr = personident,
                ).copy(isAktivKartleggingssporsmalVurdering = true)

                database.createPersonOversiktStatus(personoversiktStatus)

                database.setTildeltEnhet(
                    ident = PersonIdent(ARBEIDSTAKER_FNR),
                    enhet = NAV_ENHET,
                )
                val response = client.get(url) { bearerAuth(validToken) }
                assertEquals(HttpStatusCode.OK, response.status)
                val personOversiktStatus = response.body<List<PersonOversiktStatusDTO>>().first()
                assertEquals(personident, personOversiktStatus.fnr)
                assertEquals(NAV_ENHET, personOversiktStatus.enhet)
                assertTrue(personOversiktStatus.isAktivKartleggingssporsmalVurdering)
            }
        }

        @Test
        fun `Return no person when isAktivKartleggingssporsmalVurdering false`() {
            testApplication {
                val client = setupApiAndClient()
                val personident = ARBEIDSTAKER_FNR
                val personoversiktStatus = PersonOversiktStatus(fnr = personident)

                database.createPersonOversiktStatus(personoversiktStatus)
                database.setTildeltEnhet(
                    ident = PersonIdent(ARBEIDSTAKER_FNR),
                    enhet = NAV_ENHET,
                )
                val response = client.get(url) { bearerAuth(validToken) }
                assertEquals(HttpStatusCode.NoContent, response.status)
            }
        }
    }
}
