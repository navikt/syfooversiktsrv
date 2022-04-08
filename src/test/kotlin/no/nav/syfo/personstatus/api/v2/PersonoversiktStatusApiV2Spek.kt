package no.nav.syfo.personstatus.api.v2

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.http.*
import io.ktor.server.testing.*
import io.ktor.util.*
import io.mockk.*
import kotlinx.coroutines.runBlocking
import no.nav.syfo.application.cache.RedisStore
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.client.behandlendeenhet.BehandlendeEnhetClient
import no.nav.syfo.cronjob.behandlendeenhet.PersonBehandlendeEnhetCronjob
import no.nav.syfo.cronjob.behandlendeenhet.PersonBehandlendeEnhetService
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.domain.Virksomhetsnummer
import no.nav.syfo.oppfolgingstilfelle.kafka.KafkaOppfolgingstilfellePerson
import no.nav.syfo.oppfolgingstilfelle.kafka.OPPFOLGINGSTILFELLE_PERSON_TOPIC
import no.nav.syfo.oversikthendelsetilfelle.OversikthendelstilfelleService
import no.nav.syfo.oversikthendelsetilfelle.generateOversikthendelsetilfelle
import no.nav.syfo.personstatus.OversiktHendelseService
import no.nav.syfo.personstatus.domain.*
import no.nav.syfo.personstatus.lagreBrukerKnytningPaEnhet
import no.nav.syfo.testutil.*
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_NO_NAME_FNR
import no.nav.syfo.testutil.UserConstants.NAV_ENHET
import no.nav.syfo.testutil.UserConstants.VEILEDER_ID
import no.nav.syfo.testutil.UserConstants.VIRKSOMHETSNAVN_2
import no.nav.syfo.testutil.UserConstants.VIRKSOMHETSNUMMER
import no.nav.syfo.testutil.UserConstants.VIRKSOMHETSNUMMER_2
import no.nav.syfo.testutil.assertion.checkPersonOppfolgingstilfelle
import no.nav.syfo.testutil.generator.generateKOversikthendelse
import no.nav.syfo.testutil.generator.generateKafkaOppfolgingstilfellePerson
import no.nav.syfo.util.bearerHeader
import no.nav.syfo.util.configuredJacksonMapper
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.apache.kafka.clients.consumer.*
import org.apache.kafka.common.TopicPartition
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import redis.clients.jedis.*
import java.time.*

@InternalAPI
object PersonoversiktStatusApiV2Spek : Spek({
    val objectMapper: ObjectMapper = configuredJacksonMapper()

    describe("PersonoversiktApi") {

        with(TestApplicationEngine()) {
            start()

            val externalMockEnvironment = ExternalMockEnvironment()
            val database = externalMockEnvironment.database
            val environment = externalMockEnvironment.environment

            application.testApiModule(
                externalMockEnvironment = externalMockEnvironment
            )

            val redisStore = RedisStore(
                jedisPool = JedisPool(
                    JedisPoolConfig(),
                    externalMockEnvironment.environment.redisHost,
                    externalMockEnvironment.environment.redisPort,
                    Protocol.DEFAULT_TIMEOUT,
                    externalMockEnvironment.environment.redisSecret,
                ),
            )

            val azureAdClient = AzureAdClient(
                aadAppClient = environment.azureAppClientId,
                aadAppSecret = environment.azureAppClientSecret,
                aadTokenEndpoint = environment.azureTokenEndpoint,
                redisStore = redisStore,
            )

            val behandlendeEnhetClient = BehandlendeEnhetClient(
                azureAdClient = azureAdClient,
                baseUrl = environment.syfobehandlendeenhetUrl,
                syfobehandlendeenhetClientId = environment.syfobehandlendeenhetClientId
            )

            val personBehandlendeEnhetService = PersonBehandlendeEnhetService(
                database = database,
                behandlendeEnhetClient = behandlendeEnhetClient,
            )

            val personBehandlendeEnhetCronjob = PersonBehandlendeEnhetCronjob(
                personBehandlendeEnhetService = personBehandlendeEnhetService,
            )

            val oversiktHendelseService = OversiktHendelseService(database)
            val oversikthendelstilfelleService = OversikthendelstilfelleService(database)

            val personIdentDefault = PersonIdent(ARBEIDSTAKER_FNR)

            val mockKafkaConsumerOppfolgingstilfellePerson =
                mockk<KafkaConsumer<String, KafkaOppfolgingstilfellePerson>>()
            val partition = 0
            val oppfolgingstilfellePersonTopicPartition = TopicPartition(
                OPPFOLGINGSTILFELLE_PERSON_TOPIC,
                partition,
            )
            val kafkaOppfolgingstilfellePersonServiceRelevant = generateKafkaOppfolgingstilfellePerson(
                personIdent = personIdentDefault,
                virksomhetsnummerList = listOf(
                    UserConstants.VIRKSOMHETSNUMMER_DEFAULT,
                    Virksomhetsnummer(VIRKSOMHETSNUMMER_2),
                )
            )
            val kafkaOppfolgingstilfellePersonServiceRecordRelevant = ConsumerRecord(
                OPPFOLGINGSTILFELLE_PERSON_TOPIC,
                partition,
                1,
                "key1",
                kafkaOppfolgingstilfellePersonServiceRelevant,
            )

            beforeEachTest {
                database.connection.dropData()

                clearMocks(mockKafkaConsumerOppfolgingstilfellePerson)
                every { mockKafkaConsumerOppfolgingstilfellePerson.commitSync() } returns Unit
                every { mockKafkaConsumerOppfolgingstilfellePerson.poll(any<Duration>()) } returns ConsumerRecords(
                    mapOf(
                        oppfolgingstilfellePersonTopicPartition to listOf(
                            kafkaOppfolgingstilfellePersonServiceRecordRelevant,
                        )
                    )
                )
            }

            beforeGroup {
                externalMockEnvironment.startExternalMocks()
            }

            afterGroup {
                externalMockEnvironment.stopExternalMocks()
            }

            val validToken = generateJWT(
                audience = externalMockEnvironment.environment.azureAppClientId,
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

                it("should return NoContent, if there is a person with a relevant active Oppfolgingstilfelle, but neither MOTEBEHOV_SVAR_MOTTATT nor MOTEPLANLEGGER_ALLE_SVAR_MOTTATT") {
                    val oversikthendelstilfelle = generateOversikthendelsetilfelle.copy(
                        enhetId = NAV_ENHET,
                        gradert = false,
                        fom = LocalDate.now().minusDays(56),
                        tom = LocalDate.now()
                    )
                    oversikthendelstilfelleService.oppdaterPersonMedHendelse(oversikthendelstilfelle)

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

                it("should return list of PersonOversiktStatus, if MOTEBEHOV_SVAR_MOTTATT and MOTEPLANLEGGER_ALLE_SVAR_MOTTATT and OPPFOLGINGSPLANLPS_BISTAND_MOTTATT, and there is a person with a relevant active Oppfolgingstilfelle") {
                    val oversikthendelstilfelle = generateOversikthendelsetilfelle.copy(
                        enhetId = NAV_ENHET,
                        gradert = false,
                        fom = LocalDate.now().minusDays(56),
                        tom = LocalDate.now()
                    )
                    oversikthendelstilfelleService.oppdaterPersonMedHendelse(oversikthendelstilfelle)

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
                        personOversiktStatus.fnr shouldBeEqualTo oversikthendelstilfelle.fnr
                        personOversiktStatus.enhet shouldBeEqualTo oversikthendelstilfelle.enhetId
                        personOversiktStatus.motebehovUbehandlet shouldBeEqualTo true
                        personOversiktStatus.moteplanleggerUbehandlet shouldBeEqualTo true
                        personOversiktStatus.oppfolgingsplanLPSBistandUbehandlet shouldBeEqualTo true

                        personOversiktStatus.latestOppfolgingstilfelle.shouldBeNull()

                        personOversiktStatus.oppfolgingstilfeller.size shouldBeEqualTo 1
                        checkPersonOppfolgingstilfelle(
                            personOversiktStatus.oppfolgingstilfeller.first(),
                            oversikthendelstilfelle
                        )
                    }
                }

                it("should return list of PersonOversiktStatus, if MOTEBEHOV_SVAR_MOTTATT and if there is a person with 2 relevant active Oppfolgingstilfelle with different virksomhetsnummer") {
                    val oversikthendelstilfelle = generateOversikthendelsetilfelle.copy(
                        virksomhetsnummer = VIRKSOMHETSNUMMER,
                        enhetId = NAV_ENHET,
                        gradert = false,
                        fom = LocalDate.now().minusDays(56),
                        tom = LocalDate.now()
                    )
                    val oversikthendelstilfelle2 = oversikthendelstilfelle.copy(
                        virksomhetsnummer = VIRKSOMHETSNUMMER_2,
                        virksomhetsnavn = VIRKSOMHETSNAVN_2
                    )
                    oversikthendelstilfelleService.oppdaterPersonMedHendelse(oversikthendelstilfelle)
                    oversikthendelstilfelleService.oppdaterPersonMedHendelse(oversikthendelstilfelle2)

                    val oversiktHendelseMotebehovMottatt = KOversikthendelse(
                        ARBEIDSTAKER_FNR,
                        OversikthendelseType.MOTEBEHOV_SVAR_MOTTATT.name,
                        NAV_ENHET,
                        LocalDateTime.now()
                    )
                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelseMotebehovMottatt)

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
                        personOversiktStatus.fnr shouldBeEqualTo oversikthendelstilfelle.fnr
                        personOversiktStatus.navn shouldBeEqualTo getIdentName(ident = oversikthendelstilfelle.fnr)
                        personOversiktStatus.enhet shouldBeEqualTo oversikthendelstilfelle.enhetId
                        personOversiktStatus.motebehovUbehandlet shouldBeEqualTo true
                        personOversiktStatus.moteplanleggerUbehandlet shouldBeEqualTo null
                        personOversiktStatus.oppfolgingsplanLPSBistandUbehandlet shouldBeEqualTo null

                        personOversiktStatus.latestOppfolgingstilfelle.shouldBeNull()

                        personOversiktStatus.oppfolgingstilfeller.size shouldBeEqualTo 2
                        checkPersonOppfolgingstilfelle(
                            personOversiktStatus.oppfolgingstilfeller.first(),
                            oversikthendelstilfelle
                        )
                        checkPersonOppfolgingstilfelle(
                            personOversiktStatus.oppfolgingstilfeller.last(),
                            oversikthendelstilfelle2
                        )
                    }
                }

                it("should return list of PersonOversiktStatus, if MOTEPLANLEGGER_ALLE_SVAR_MOTTATT and  if there is a person with a relevant Oppfolgingstilfelle valid in Arbeidsgiverperiode") {
                    val oversikthendelstilfelle = generateOversikthendelsetilfelle.copy(
                        enhetId = NAV_ENHET,
                        gradert = false,
                        fom = LocalDate.now().minusDays(56),
                        tom = LocalDate.now().minusDays(16)
                    )
                    oversikthendelstilfelleService.oppdaterPersonMedHendelse(oversikthendelstilfelle)

                    val oversiktHendelseMoteplanleggerMottatt = KOversikthendelse(
                        ARBEIDSTAKER_FNR,
                        OversikthendelseType.MOTEPLANLEGGER_ALLE_SVAR_MOTTATT.name,
                        NAV_ENHET,
                        LocalDateTime.now()
                    )
                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelseMoteplanleggerMottatt)

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
                        personOversiktStatus.fnr shouldBeEqualTo oversikthendelstilfelle.fnr
                        personOversiktStatus.navn shouldBeEqualTo getIdentName(ident = oversikthendelstilfelle.fnr)
                        personOversiktStatus.enhet shouldBeEqualTo oversikthendelstilfelle.enhetId
                        personOversiktStatus.motebehovUbehandlet shouldBeEqualTo null
                        personOversiktStatus.moteplanleggerUbehandlet shouldBeEqualTo true
                        personOversiktStatus.oppfolgingsplanLPSBistandUbehandlet shouldBeEqualTo null

                        personOversiktStatus.latestOppfolgingstilfelle.shouldBeNull()

                        personOversiktStatus.oppfolgingstilfeller.size shouldBeEqualTo 1
                        checkPersonOppfolgingstilfelle(
                            personOversiktStatus.oppfolgingstilfeller.first(),
                            oversikthendelstilfelle
                        )
                    }
                }

                it("should not return person with a Oppfolgingstilfelle that is not valid after today + Arbeidsgiverperiode") {
                    val oversikthendelstilfelle = generateOversikthendelsetilfelle.copy(
                        enhetId = NAV_ENHET,
                        gradert = false,
                        fom = LocalDate.now().minusDays(56),
                        tom = LocalDate.now().minusDays(17)
                    )
                    oversikthendelstilfelleService.oppdaterPersonMedHendelse(oversikthendelstilfelle)

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

                it("should not return a person with a Oppfolgingstilfelle that is Gradert") {
                    val oversikthendelstilfelle = generateOversikthendelsetilfelle.copy(
                        enhetId = NAV_ENHET,
                        gradert = true,
                        fom = LocalDate.now().minusDays(56),
                        tom = LocalDate.now().plusDays(16)
                    )
                    oversikthendelstilfelleService.oppdaterPersonMedHendelse(oversikthendelstilfelle)

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

                it("should not return a person with a Oppfolgingstilfelle that is not 8 weeks old") {
                    val oversikthendelstilfelle = generateOversikthendelsetilfelle.copy(
                        enhetId = NAV_ENHET,
                        gradert = false,
                        fom = LocalDate.now().minusDays(55),
                        tom = LocalDate.now().plusDays(16)
                    )
                    oversikthendelstilfelleService.oppdaterPersonMedHendelse(oversikthendelstilfelle)

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

                    val oversikthendelstilfelle = generateOversikthendelsetilfelle.copy(
                        enhetId = NAV_ENHET,
                        gradert = false,
                        fom = LocalDate.now().minusDays(56),
                        tom = LocalDate.now()
                    )
                    oversikthendelstilfelleService.oppdaterPersonMedHendelse(oversikthendelstilfelle)

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
                        personOversiktStatus.fnr shouldBeEqualTo oversikthendelstilfelle.fnr
                        personOversiktStatus.navn shouldBeEqualTo getIdentName(ident = oversikthendelstilfelle.fnr)
                        personOversiktStatus.enhet shouldBeEqualTo oversikthendelstilfelle.enhetId
                        personOversiktStatus.motebehovUbehandlet shouldBeEqualTo true
                        personOversiktStatus.moteplanleggerUbehandlet shouldBeEqualTo true
                        personOversiktStatus.oppfolgingsplanLPSBistandUbehandlet shouldBeEqualTo true

                        personOversiktStatus.latestOppfolgingstilfelle.shouldBeNull()

                        personOversiktStatus.oppfolgingstilfeller.size shouldBeEqualTo 1
                        checkPersonOppfolgingstilfelle(
                            personOversiktStatus.oppfolgingstilfeller.first(),
                            oversikthendelstilfelle
                        )
                    }
                }

                it("should return Person, receives Oppfolgingstilfelle, and then MOTEBEHOV_SVAR_MOTTATT and MOTEPLANLEGGER_ALLE_SVAR_MOTTATT and OPPFOLGINGSPLANLPS_BISTAND_MOTTATT") {
                    val oversikthendelstilfelle = generateOversikthendelsetilfelle.copy(
                        enhetId = NAV_ENHET,
                        gradert = false,
                        fom = LocalDate.now().minusDays(56),
                        tom = LocalDate.now()
                    )
                    oversikthendelstilfelleService.oppdaterPersonMedHendelse(oversikthendelstilfelle)

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
                        personOversiktStatus.fnr shouldBeEqualTo oversikthendelstilfelle.fnr
                        personOversiktStatus.navn shouldBeEqualTo getIdentName(ident = oversikthendelstilfelle.fnr)
                        personOversiktStatus.enhet shouldBeEqualTo oversikthendelstilfelle.enhetId
                        personOversiktStatus.motebehovUbehandlet shouldBeEqualTo true
                        personOversiktStatus.moteplanleggerUbehandlet shouldBeEqualTo true
                        personOversiktStatus.oppfolgingsplanLPSBistandUbehandlet shouldBeEqualTo true

                        personOversiktStatus.latestOppfolgingstilfelle.shouldBeNull()

                        personOversiktStatus.oppfolgingstilfeller.size shouldBeEqualTo 1
                        checkPersonOppfolgingstilfelle(
                            personOversiktStatus.oppfolgingstilfeller.first(),
                            oversikthendelstilfelle
                        )
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
                        personOversiktStatus.navn shouldBeEqualTo getIdentName(ident = oversiktHendelseOPLPSBistandMottatt.fnr)
                        personOversiktStatus.enhet shouldBeEqualTo oversiktHendelseOPLPSBistandMottatt.enhetId
                        personOversiktStatus.motebehovUbehandlet shouldBeEqualTo null
                        personOversiktStatus.moteplanleggerUbehandlet shouldBeEqualTo null
                        personOversiktStatus.oppfolgingsplanLPSBistandUbehandlet shouldBeEqualTo true

                        personOversiktStatus.latestOppfolgingstilfelle.shouldBeNull()

                        personOversiktStatus.oppfolgingstilfeller.size shouldBeEqualTo 0
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
                        personOversiktStatus.enhet shouldBeEqualTo oversiktHendelseOPLPSBistandMottatt.enhetId
                        personOversiktStatus.motebehovUbehandlet shouldBeEqualTo null
                        personOversiktStatus.moteplanleggerUbehandlet shouldBeEqualTo null
                        personOversiktStatus.oppfolgingsplanLPSBistandUbehandlet shouldBeEqualTo true

                        personOversiktStatus.latestOppfolgingstilfelle.shouldBeNull()

                        personOversiktStatus.oppfolgingstilfeller.size shouldBeEqualTo 0
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
