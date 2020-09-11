package no.nav.syfo.personstatus

import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.jackson
import io.ktor.response.respond
import io.ktor.routing.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.handleRequest
import io.ktor.util.InternalAPI
import io.mockk.every
import io.mockk.mockkStatic
import no.nav.syfo.auth.getTokenFromCookie
import no.nav.syfo.auth.isInvalidToken
import no.nav.syfo.oversikthendelsetilfelle.OversikthendelstilfelleService
import no.nav.syfo.oversikthendelsetilfelle.domain.KOversikthendelsetilfelle
import no.nav.syfo.oversikthendelsetilfelle.generateOversikthendelsetilfelle
import no.nav.syfo.personstatus.domain.*
import no.nav.syfo.testutil.*
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testutil.UserConstants.NAV_ENHET
import no.nav.syfo.testutil.UserConstants.VEILEDER_ID
import no.nav.syfo.testutil.UserConstants.VIRKSOMHETSNAVN_2
import no.nav.syfo.testutil.UserConstants.VIRKSOMHETSNUMMER
import no.nav.syfo.testutil.UserConstants.VIRKSOMHETSNUMMER_2
import no.nav.syfo.testutil.generator.generateKOversikthendelse
import no.nav.syfo.tilgangskontroll.Tilgang
import no.nav.syfo.tilgangskontroll.TilgangskontrollConsumer
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.net.ServerSocket
import java.time.LocalDate
import java.time.LocalDateTime

private val objectMapper: ObjectMapper = ObjectMapper().apply {
    registerKotlinModule()
    registerModule(JavaTimeModule())
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
}

@InternalAPI
object PersonoversiktStatusApiSpek : Spek({

    describe("PersonoversiktApi") {

        with(TestApplicationEngine()) {
            start()

            val responseAccessEnhet = Tilgang(true, "")
            val responseAccessPersons = listOf(ARBEIDSTAKER_FNR)

            val mockHttpServerPort = ServerSocket(0).use { it.localPort }
            val mockHttpServerUrl = "http://localhost:$mockHttpServerPort"
            val mockServer = embeddedServer(Netty, mockHttpServerPort) {
                install(ContentNegotiation) {
                    jackson {}
                }
                routing {
                    get("/syfo-tilgangskontroll/api/tilgang/enhet") {
                        if (call.parameters["enhet"] == NAV_ENHET) {
                            call.respond(responseAccessEnhet)
                        }
                    }
                    post("/syfo-tilgangskontroll/api/tilgang/brukere") {
                        call.respond(responseAccessPersons)
                    }
                }
            }.start()

            val database = TestDB()
            val cookies = ""
            val baseUrl = "/api/v1/personoversikt"
            val tilgangskontrollConsumer = TilgangskontrollConsumer(
                    mockHttpServerUrl
            )
            val oversiktHendelseService = OversiktHendelseService(database)
            val oversikthendelstilfelleService = OversikthendelstilfelleService(database)

            application.install(ContentNegotiation) {
                jackson {
                    registerKotlinModule()
                    registerModule(JavaTimeModule())
                    configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                }
            }

            application.routing {
                registerPersonoversiktApi(tilgangskontrollConsumer, PersonoversiktStatusService(database))
            }

            beforeEachTest {
                mockkStatic("no.nav.syfo.auth.TokenAuthKt")
            }

            afterEachTest {
                database.connection.dropData()
            }

            afterGroup {
                mockServer.stop(1L, 10L)
                database.stop()
            }

            describe("Hent personoversikt for enhet") {
                val url = "$baseUrl/enhet/$NAV_ENHET"

                it("skal returnere status NoContent om det ikke er noen personer som er tilknyttet enhet") {
                    every {
                        isInvalidToken(any())
                    } returns false

                    with(handleRequest(HttpMethod.Get, url) {
                        call.request.cookies[cookies]
                    }) {
                        response.status() shouldEqual HttpStatusCode.NoContent
                    }
                }

                it("skal returnere NoContent med ubehandlet motebehovsvar og ikke har oppfolgingstilfelle") {
                    every {
                        isInvalidToken(any())
                    } returns false
                    every {
                        getTokenFromCookie(any())
                    } returns "token"

                    val oversiktHendelse = KOversikthendelse(ARBEIDSTAKER_FNR, OversikthendelseType.MOTEBEHOV_SVAR_MOTTATT.name, NAV_ENHET, LocalDateTime.now())
                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelse)

                    val tilknytning = VeilederBrukerKnytning(VEILEDER_ID, ARBEIDSTAKER_FNR, NAV_ENHET)
                    database.connection.tildelVeilederTilPerson(tilknytning)

                    with(handleRequest(HttpMethod.Get, url) {
                        call.request.cookies[cookies]
                    }) {
                        response.status() shouldEqual HttpStatusCode.NoContent
                    }
                }

                it("skal returnere NoContent med ubehandlet moteplanleggersvar og ikke har oppfolgingstilfelle") {
                    every {
                        isInvalidToken(any())
                    } returns false
                    every {
                        getTokenFromCookie(any())
                    } returns "token"

                    val oversiktHendelse = KOversikthendelse(ARBEIDSTAKER_FNR, OversikthendelseType.MOTEPLANLEGGER_ALLE_SVAR_MOTTATT.name, NAV_ENHET, LocalDateTime.now())
                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelse)

                    val tilknytning = VeilederBrukerKnytning(VEILEDER_ID, ARBEIDSTAKER_FNR, NAV_ENHET)
                    database.connection.tildelVeilederTilPerson(tilknytning)

                    with(handleRequest(HttpMethod.Get, url) {
                        call.request.cookies[cookies]
                    }) {
                        response.status() shouldEqual HttpStatusCode.NoContent
                    }
                }

                it("skal returnere NoContent, om alle personer i personoversikt er behandlet og ikke har oppfolgingstilfelle") {
                    every {
                        isInvalidToken(any())
                    } returns false
                    every {
                        getTokenFromCookie(any())
                    } returns "token"

                    val oversiktHendelse = KOversikthendelse(ARBEIDSTAKER_FNR, OversikthendelseType.MOTEBEHOV_SVAR_MOTTATT.name, NAV_ENHET, LocalDateTime.now())
                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelse)

                    val tilknytning = VeilederBrukerKnytning(VEILEDER_ID, ARBEIDSTAKER_FNR, NAV_ENHET)
                    database.connection.tildelVeilederTilPerson(tilknytning)

                    val oversiktHendelseNy = KOversikthendelse(ARBEIDSTAKER_FNR, OversikthendelseType.MOTEBEHOV_SVAR_BEHANDLET.name, NAV_ENHET, LocalDateTime.now())
                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelseNy)

                    val oversiktHendelseMoteplanleggerMottatt = KOversikthendelse(ARBEIDSTAKER_FNR, OversikthendelseType.MOTEPLANLEGGER_ALLE_SVAR_MOTTATT.name, NAV_ENHET, LocalDateTime.now())
                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelseMoteplanleggerMottatt)

                    val oversiktHendelseMoteplanleggerBehandlet = KOversikthendelse(ARBEIDSTAKER_FNR, OversikthendelseType.MOTEPLANLEGGER_ALLE_SVAR_BEHANDLET.name, NAV_ENHET, LocalDateTime.now())
                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelseMoteplanleggerBehandlet)

                    with(handleRequest(HttpMethod.Get, url) {
                        call.request.cookies[cookies]
                    }) {
                        response.status() shouldEqual HttpStatusCode.NoContent
                    }
                }

                it("should return NoContent, if there is a person with a relevant active Oppfolgingstilfelle, but neither MOTEBEHOV_SVAR_MOTTATT nor MOTEPLANLEGGER_ALLE_SVAR_MOTTATT") {
                    every {
                        isInvalidToken(any())
                    } returns false
                    every {
                        getTokenFromCookie(any())
                    } returns "token"

                    val oversikthendelstilfelle = generateOversikthendelsetilfelle.copy(
                            enhetId = NAV_ENHET,
                            gradert = false,
                            fom = LocalDate.now().minusDays(56),
                            tom = LocalDate.now()
                    )
                    oversikthendelstilfelleService.oppdaterPersonMedHendelse(oversikthendelstilfelle)

                    with(handleRequest(HttpMethod.Get, url) {
                        call.request.cookies[cookies]
                    }) {
                        response.status() shouldEqual HttpStatusCode.NoContent
                    }
                }

                it("should return list of PersonOversiktStatus, if MOTEBEHOV_SVAR_MOTTATT and MOTEPLANLEGGER_ALLE_SVAR_MOTTATT and OPPFOLGINGSPLANLPS_BISTAND_MOTTATT, and there is a person with a relevant active Oppfolgingstilfelle") {
                    every {
                        isInvalidToken(any())
                    } returns false
                    every {
                        getTokenFromCookie(any())
                    } returns "token"

                    val oversikthendelstilfelle = generateOversikthendelsetilfelle.copy(
                            enhetId = NAV_ENHET,
                            gradert = false,
                            fom = LocalDate.now().minusDays(56),
                            tom = LocalDate.now()
                    )
                    oversikthendelstilfelleService.oppdaterPersonMedHendelse(oversikthendelstilfelle)

                    val oversiktHendelseMotebehovMottatt = KOversikthendelse(ARBEIDSTAKER_FNR, OversikthendelseType.MOTEBEHOV_SVAR_MOTTATT.name, NAV_ENHET, LocalDateTime.now())
                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelseMotebehovMottatt)

                    val oversiktHendelseMoteplanleggerMottatt = KOversikthendelse(ARBEIDSTAKER_FNR, OversikthendelseType.MOTEPLANLEGGER_ALLE_SVAR_MOTTATT.name, NAV_ENHET, LocalDateTime.now())
                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelseMoteplanleggerMottatt)

                    val oversiktHendelseOPLPSBistandMottatt = generateKOversikthendelse(OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_MOTTATT)
                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelseOPLPSBistandMottatt)

                    with(handleRequest(HttpMethod.Get, url) {
                        call.request.cookies[cookies]
                    }) {
                        response.status() shouldEqual HttpStatusCode.OK
                        val personOversiktStatus = objectMapper.readValue<List<PersonOversiktStatus>>(response.content!!).first()
                        personOversiktStatus.veilederIdent shouldEqual null
                        personOversiktStatus.fnr shouldEqual oversikthendelstilfelle.fnr
                        personOversiktStatus.enhet shouldEqual oversikthendelstilfelle.enhetId
                        personOversiktStatus.motebehovUbehandlet shouldEqual true
                        personOversiktStatus.moteplanleggerUbehandlet shouldEqual true
                        personOversiktStatus.oppfolgingsplanLPSBistandUbehandlet shouldEqual true

                        personOversiktStatus.oppfolgingstilfeller.size shouldEqual 1
                        checkPersonOppfolgingstilfelle(personOversiktStatus.oppfolgingstilfeller.first(), oversikthendelstilfelle)
                    }
                }

                it("should return list of PersonOversiktStatus, if MOTEBEHOV_SVAR_MOTTATT and if there is a person with 2 relevant active Oppfolgingstilfelle with different virksomhetsnummer") {
                    every {
                        isInvalidToken(any())
                    } returns false
                    every {
                        getTokenFromCookie(any())
                    } returns "token"

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

                    val oversiktHendelseMotebehovMottatt = KOversikthendelse(ARBEIDSTAKER_FNR, OversikthendelseType.MOTEBEHOV_SVAR_MOTTATT.name, NAV_ENHET, LocalDateTime.now())
                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelseMotebehovMottatt)

                    with(handleRequest(HttpMethod.Get, url) {
                        call.request.cookies[cookies]
                    }) {
                        response.status() shouldEqual HttpStatusCode.OK
                        val personOversiktStatus = objectMapper.readValue<List<PersonOversiktStatus>>(response.content!!).first()
                        personOversiktStatus.veilederIdent shouldEqual null
                        personOversiktStatus.fnr shouldEqual oversikthendelstilfelle.fnr
                        personOversiktStatus.navn shouldEqual oversikthendelstilfelle.navn
                        personOversiktStatus.enhet shouldEqual oversikthendelstilfelle.enhetId
                        personOversiktStatus.motebehovUbehandlet shouldEqual true
                        personOversiktStatus.moteplanleggerUbehandlet shouldEqual null
                        personOversiktStatus.oppfolgingsplanLPSBistandUbehandlet shouldEqual null

                        personOversiktStatus.oppfolgingstilfeller.size shouldEqual 2
                        checkPersonOppfolgingstilfelle(personOversiktStatus.oppfolgingstilfeller.first(), oversikthendelstilfelle)
                        checkPersonOppfolgingstilfelle(personOversiktStatus.oppfolgingstilfeller.last(), oversikthendelstilfelle2)
                    }
                }

                it("should return list of PersonOversiktStatus, if MOTEPLANLEGGER_ALLE_SVAR_MOTTATT and  if there is a person with a relevant Oppfolgingstilfelle valid in Arbeidsgiverperiode") {
                    every {
                        isInvalidToken(any())
                    } returns false
                    every {
                        getTokenFromCookie(any())
                    } returns "token"

                    val oversikthendelstilfelle = generateOversikthendelsetilfelle.copy(
                            enhetId = NAV_ENHET,
                            gradert = false,
                            fom = LocalDate.now().minusDays(56),
                            tom = LocalDate.now().minusDays(16)
                    )
                    oversikthendelstilfelleService.oppdaterPersonMedHendelse(oversikthendelstilfelle)

                    val oversiktHendelseMoteplanleggerMottatt = KOversikthendelse(ARBEIDSTAKER_FNR, OversikthendelseType.MOTEPLANLEGGER_ALLE_SVAR_MOTTATT.name, NAV_ENHET, LocalDateTime.now())
                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelseMoteplanleggerMottatt)

                    with(handleRequest(HttpMethod.Get, url) {
                        call.request.cookies[cookies]
                    }) {
                        response.status() shouldEqual HttpStatusCode.OK
                        val personOversiktStatus = objectMapper.readValue<List<PersonOversiktStatus>>(response.content!!).first()
                        personOversiktStatus.veilederIdent shouldEqual null
                        personOversiktStatus.fnr shouldEqual oversikthendelstilfelle.fnr
                        personOversiktStatus.navn shouldEqual oversikthendelstilfelle.navn
                        personOversiktStatus.enhet shouldEqual oversikthendelstilfelle.enhetId
                        personOversiktStatus.motebehovUbehandlet shouldEqual null
                        personOversiktStatus.moteplanleggerUbehandlet shouldEqual true
                        personOversiktStatus.oppfolgingsplanLPSBistandUbehandlet shouldEqual null

                        personOversiktStatus.oppfolgingstilfeller.size shouldEqual 1
                        checkPersonOppfolgingstilfelle(personOversiktStatus.oppfolgingstilfeller.first(), oversikthendelstilfelle)
                    }
                }

                it("should not return person with a Oppfolgingstilfelle that is not valid after today + Arbeidsgiverperiode") {
                    every {
                        isInvalidToken(any())
                    } returns false
                    every {
                        getTokenFromCookie(any())
                    } returns "token"

                    val oversikthendelstilfelle = generateOversikthendelsetilfelle.copy(
                            enhetId = NAV_ENHET,
                            gradert = false,
                            fom = LocalDate.now().minusDays(56),
                            tom = LocalDate.now().minusDays(17)
                    )
                    oversikthendelstilfelleService.oppdaterPersonMedHendelse(oversikthendelstilfelle)

                    with(handleRequest(HttpMethod.Get, url) {
                        call.request.cookies[cookies]
                    }) {
                        response.status() shouldEqual HttpStatusCode.NoContent
                    }
                }

                it("should not return a person with a Oppfolgingstilfelle that is Gradert") {
                    every {
                        isInvalidToken(any())
                    } returns false
                    every {
                        getTokenFromCookie(any())
                    } returns "token"

                    val oversikthendelstilfelle = generateOversikthendelsetilfelle.copy(
                            enhetId = NAV_ENHET,
                            gradert = true,
                            fom = LocalDate.now().minusDays(56),
                            tom = LocalDate.now().plusDays(16)
                    )
                    oversikthendelstilfelleService.oppdaterPersonMedHendelse(oversikthendelstilfelle)

                    with(handleRequest(HttpMethod.Get, url) {
                        call.request.cookies[cookies]
                    }) {
                        response.status() shouldEqual HttpStatusCode.NoContent
                    }
                }

                it("should not return a person with a Oppfolgingstilfelle that is not 8 weeks old") {
                    every {
                        isInvalidToken(any())
                    } returns false
                    every {
                        getTokenFromCookie(any())
                    } returns "token"

                    val oversikthendelstilfelle = generateOversikthendelsetilfelle.copy(
                            enhetId = NAV_ENHET,
                            gradert = false,
                            fom = LocalDate.now().minusDays(55),
                            tom = LocalDate.now().plusDays(16)
                    )
                    oversikthendelstilfelleService.oppdaterPersonMedHendelse(oversikthendelstilfelle)

                    with(handleRequest(HttpMethod.Get, url) {
                        call.request.cookies[cookies]
                    }) {
                        response.status() shouldEqual HttpStatusCode.NoContent
                    }
                }

                it("should return Person, with MOTEBEHOV_SVAR_MOTTATT && MOTEPLANLEGGER_ALLE_SVAR_MOTTATT, and then receives Oppfolgingstilfelle and the OPPFOLGINGSPLANLPS_BISTAND_MOTTATT") {
                    every {
                        isInvalidToken(any())
                    } returns false
                    every {
                        getTokenFromCookie(any())
                    } returns "token"

                    val oversiktHendelse = KOversikthendelse(ARBEIDSTAKER_FNR, OversikthendelseType.MOTEBEHOV_SVAR_MOTTATT.name, NAV_ENHET, LocalDateTime.now())
                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelse)

                    val tilknytning = VeilederBrukerKnytning(VEILEDER_ID, ARBEIDSTAKER_FNR, NAV_ENHET)
                    database.connection.tildelVeilederTilPerson(tilknytning)

                    val oversiktHendelseMoteplanleggerMottatt = KOversikthendelse(ARBEIDSTAKER_FNR, OversikthendelseType.MOTEPLANLEGGER_ALLE_SVAR_MOTTATT.name, NAV_ENHET, LocalDateTime.now())
                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelseMoteplanleggerMottatt)

                    val oversiktHendelseOPLPSBistandMottatt = generateKOversikthendelse(OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_MOTTATT)
                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelseOPLPSBistandMottatt)

                    val oversikthendelstilfelle = generateOversikthendelsetilfelle.copy(
                            enhetId = NAV_ENHET,
                            gradert = false,
                            fom = LocalDate.now().minusDays(56),
                            tom = LocalDate.now()
                    )
                    oversikthendelstilfelleService.oppdaterPersonMedHendelse(oversikthendelstilfelle)

                    with(handleRequest(HttpMethod.Get, url) {
                        call.request.cookies[cookies]
                    }) {
                        response.status() shouldEqual HttpStatusCode.OK
                        val personOversiktStatus = objectMapper.readValue<List<PersonOversiktStatus>>(response.content!!).first()
                        personOversiktStatus.veilederIdent shouldEqual tilknytning.veilederIdent
                        personOversiktStatus.fnr shouldEqual oversikthendelstilfelle.fnr
                        personOversiktStatus.navn shouldEqual oversikthendelstilfelle.navn
                        personOversiktStatus.enhet shouldEqual oversikthendelstilfelle.enhetId
                        personOversiktStatus.motebehovUbehandlet shouldEqual true
                        personOversiktStatus.moteplanleggerUbehandlet shouldEqual true
                        personOversiktStatus.oppfolgingsplanLPSBistandUbehandlet shouldEqual true

                        personOversiktStatus.oppfolgingstilfeller.size shouldEqual 1
                        checkPersonOppfolgingstilfelle(personOversiktStatus.oppfolgingstilfeller.first(), oversikthendelstilfelle)
                    }
                }

                it("should return Person, receives Oppfolgingstilfelle, and then MOTEBEHOV_SVAR_MOTTATT and MOTEPLANLEGGER_ALLE_SVAR_MOTTATT and OPPFOLGINGSPLANLPS_BISTAND_MOTTATT") {
                    every {
                        isInvalidToken(any())
                    } returns false
                    every {
                        getTokenFromCookie(any())
                    } returns "token"

                    val oversikthendelstilfelle = generateOversikthendelsetilfelle.copy(
                            enhetId = NAV_ENHET,
                            gradert = false,
                            fom = LocalDate.now().minusDays(56),
                            tom = LocalDate.now()
                    )
                    oversikthendelstilfelleService.oppdaterPersonMedHendelse(oversikthendelstilfelle)

                    val oversiktHendelse = KOversikthendelse(ARBEIDSTAKER_FNR, OversikthendelseType.MOTEBEHOV_SVAR_MOTTATT.name, NAV_ENHET, LocalDateTime.now())
                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelse)

                    val tilknytning = VeilederBrukerKnytning(VEILEDER_ID, ARBEIDSTAKER_FNR, NAV_ENHET)
                    database.connection.tildelVeilederTilPerson(tilknytning)

                    val oversiktHendelseMoteplanleggerMottatt = KOversikthendelse(ARBEIDSTAKER_FNR, OversikthendelseType.MOTEPLANLEGGER_ALLE_SVAR_MOTTATT.name, NAV_ENHET, LocalDateTime.now())
                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelseMoteplanleggerMottatt)

                    val oversiktHendelseOPLPSBistandMottatt = generateKOversikthendelse(OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_MOTTATT)
                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelseOPLPSBistandMottatt)

                    with(handleRequest(HttpMethod.Get, url) {
                        call.request.cookies[cookies]
                    }) {
                        response.status() shouldEqual HttpStatusCode.OK
                        val personOversiktStatus = objectMapper.readValue<List<PersonOversiktStatus>>(response.content!!).first()
                        personOversiktStatus.veilederIdent shouldEqual tilknytning.veilederIdent
                        personOversiktStatus.fnr shouldEqual oversikthendelstilfelle.fnr
                        personOversiktStatus.navn shouldEqual oversikthendelstilfelle.navn
                        personOversiktStatus.enhet shouldEqual oversikthendelstilfelle.enhetId
                        personOversiktStatus.motebehovUbehandlet shouldEqual true
                        personOversiktStatus.moteplanleggerUbehandlet shouldEqual true
                        personOversiktStatus.oppfolgingsplanLPSBistandUbehandlet shouldEqual true

                        personOversiktStatus.oppfolgingstilfeller.size shouldEqual 1
                        checkPersonOppfolgingstilfelle(personOversiktStatus.oppfolgingstilfeller.first(), oversikthendelstilfelle)
                    }
                }
            }
        }
    }
})

fun checkPersonOppfolgingstilfelle(oppfolgingstilfelle: Oppfolgingstilfelle, oversikthendelsetilfelle: KOversikthendelsetilfelle) {
    oppfolgingstilfelle.virksomhetsnummer shouldEqual oversikthendelsetilfelle.virksomhetsnummer
    oppfolgingstilfelle.fom shouldEqual oversikthendelsetilfelle.fom
    oppfolgingstilfelle.tom shouldEqual oversikthendelsetilfelle.tom
}
