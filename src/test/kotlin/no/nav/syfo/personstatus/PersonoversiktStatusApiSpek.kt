package no.nav.syfo.personstatus

import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.http.*
import io.ktor.server.testing.*
import io.ktor.util.*
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
import org.amshove.kluent.shouldBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.LocalDate
import java.time.LocalDateTime

@InternalAPI
object PersonoversiktStatusApiSpek : Spek({
    val objectMapper: ObjectMapper = apiConsumerObjectMapper()

    describe("PersonoversiktApi") {

        with(TestApplicationEngine()) {
            start()

            val externalMockEnvironment = ExternalMockEnvironment()

            val database = externalMockEnvironment.database
            val cookies = ""
            val baseUrl = "/api/v1/personoversikt"
            val oversiktHendelseService = OversiktHendelseService(database)
            val oversikthendelstilfelleService = OversikthendelstilfelleService(database)

            application.testApiModule(
                externalMockEnvironment = externalMockEnvironment
            )

            beforeEachTest {
                mockkStatic("no.nav.syfo.auth.TokenAuthKt")
            }

            afterEachTest {
                database.connection.dropData()
            }

            beforeGroup {
                externalMockEnvironment.startExternalMocks()
            }

            afterGroup {
                externalMockEnvironment.stopExternalMocks()
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
                        response.status() shouldBeEqualTo HttpStatusCode.NoContent
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
                    database.lagreBrukerKnytningPaEnhet(tilknytning)

                    with(handleRequest(HttpMethod.Get, url) {
                        call.request.cookies[cookies]
                    }) {
                        response.status() shouldBeEqualTo HttpStatusCode.NoContent
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
                    database.lagreBrukerKnytningPaEnhet(tilknytning)

                    with(handleRequest(HttpMethod.Get, url) {
                        call.request.cookies[cookies]
                    }) {
                        response.status() shouldBeEqualTo HttpStatusCode.NoContent
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
                    database.lagreBrukerKnytningPaEnhet(tilknytning)

                    val oversiktHendelseNy = KOversikthendelse(ARBEIDSTAKER_FNR, OversikthendelseType.MOTEBEHOV_SVAR_BEHANDLET.name, NAV_ENHET, LocalDateTime.now())
                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelseNy)

                    val oversiktHendelseMoteplanleggerMottatt = KOversikthendelse(ARBEIDSTAKER_FNR, OversikthendelseType.MOTEPLANLEGGER_ALLE_SVAR_MOTTATT.name, NAV_ENHET, LocalDateTime.now())
                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelseMoteplanleggerMottatt)

                    val oversiktHendelseMoteplanleggerBehandlet = KOversikthendelse(ARBEIDSTAKER_FNR, OversikthendelseType.MOTEPLANLEGGER_ALLE_SVAR_BEHANDLET.name, NAV_ENHET, LocalDateTime.now())
                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelseMoteplanleggerBehandlet)

                    with(handleRequest(HttpMethod.Get, url) {
                        call.request.cookies[cookies]
                    }) {
                        response.status() shouldBeEqualTo HttpStatusCode.NoContent
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
                        response.status() shouldBeEqualTo HttpStatusCode.NoContent
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
                        response.status() shouldBeEqualTo HttpStatusCode.OK
                        val personOversiktStatus = objectMapper.readValue<List<PersonOversiktStatus>>(response.content!!).first()
                        personOversiktStatus.veilederIdent shouldBeEqualTo null
                        personOversiktStatus.fnr shouldBeEqualTo oversikthendelstilfelle.fnr
                        personOversiktStatus.enhet shouldBeEqualTo oversikthendelstilfelle.enhetId
                        personOversiktStatus.motebehovUbehandlet shouldBeEqualTo true
                        personOversiktStatus.moteplanleggerUbehandlet shouldBeEqualTo true
                        personOversiktStatus.oppfolgingsplanLPSBistandUbehandlet shouldBeEqualTo true

                        personOversiktStatus.oppfolgingstilfeller.size shouldBeEqualTo 1
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
                        response.status() shouldBeEqualTo HttpStatusCode.OK
                        val personOversiktStatus = objectMapper.readValue<List<PersonOversiktStatus>>(response.content!!).first()
                        personOversiktStatus.veilederIdent shouldBeEqualTo null
                        personOversiktStatus.fnr shouldBeEqualTo oversikthendelstilfelle.fnr
                        personOversiktStatus.navn shouldBeEqualTo oversikthendelstilfelle.navn
                        personOversiktStatus.enhet shouldBeEqualTo oversikthendelstilfelle.enhetId
                        personOversiktStatus.motebehovUbehandlet shouldBeEqualTo true
                        personOversiktStatus.moteplanleggerUbehandlet shouldBeEqualTo null
                        personOversiktStatus.oppfolgingsplanLPSBistandUbehandlet shouldBeEqualTo null

                        personOversiktStatus.oppfolgingstilfeller.size shouldBeEqualTo 2
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
                        response.status() shouldBeEqualTo HttpStatusCode.OK
                        val personOversiktStatus = objectMapper.readValue<List<PersonOversiktStatus>>(response.content!!).first()
                        personOversiktStatus.veilederIdent shouldBeEqualTo null
                        personOversiktStatus.fnr shouldBeEqualTo oversikthendelstilfelle.fnr
                        personOversiktStatus.navn shouldBeEqualTo oversikthendelstilfelle.navn
                        personOversiktStatus.enhet shouldBeEqualTo oversikthendelstilfelle.enhetId
                        personOversiktStatus.motebehovUbehandlet shouldBeEqualTo null
                        personOversiktStatus.moteplanleggerUbehandlet shouldBeEqualTo true
                        personOversiktStatus.oppfolgingsplanLPSBistandUbehandlet shouldBeEqualTo null

                        personOversiktStatus.oppfolgingstilfeller.size shouldBeEqualTo 1
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
                        response.status() shouldBeEqualTo HttpStatusCode.NoContent
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
                        response.status() shouldBeEqualTo HttpStatusCode.NoContent
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
                        response.status() shouldBeEqualTo HttpStatusCode.NoContent
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
                    database.lagreBrukerKnytningPaEnhet(tilknytning)

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
                        response.status() shouldBeEqualTo HttpStatusCode.OK
                        val personOversiktStatus = objectMapper.readValue<List<PersonOversiktStatus>>(response.content!!).first()
                        personOversiktStatus.veilederIdent shouldBeEqualTo tilknytning.veilederIdent
                        personOversiktStatus.fnr shouldBeEqualTo oversikthendelstilfelle.fnr
                        personOversiktStatus.navn shouldBeEqualTo oversikthendelstilfelle.navn
                        personOversiktStatus.enhet shouldBeEqualTo oversikthendelstilfelle.enhetId
                        personOversiktStatus.motebehovUbehandlet shouldBeEqualTo true
                        personOversiktStatus.moteplanleggerUbehandlet shouldBeEqualTo true
                        personOversiktStatus.oppfolgingsplanLPSBistandUbehandlet shouldBeEqualTo true

                        personOversiktStatus.oppfolgingstilfeller.size shouldBeEqualTo 1
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
                    database.lagreBrukerKnytningPaEnhet(tilknytning)

                    val oversiktHendelseMoteplanleggerMottatt = KOversikthendelse(ARBEIDSTAKER_FNR, OversikthendelseType.MOTEPLANLEGGER_ALLE_SVAR_MOTTATT.name, NAV_ENHET, LocalDateTime.now())
                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelseMoteplanleggerMottatt)

                    val oversiktHendelseOPLPSBistandMottatt = generateKOversikthendelse(OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_MOTTATT)
                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelseOPLPSBistandMottatt)

                    with(handleRequest(HttpMethod.Get, url) {
                        call.request.cookies[cookies]
                    }) {
                        response.status() shouldBeEqualTo HttpStatusCode.OK
                        val personOversiktStatus = objectMapper.readValue<List<PersonOversiktStatus>>(response.content!!).first()
                        personOversiktStatus.veilederIdent shouldBeEqualTo tilknytning.veilederIdent
                        personOversiktStatus.fnr shouldBeEqualTo oversikthendelstilfelle.fnr
                        personOversiktStatus.navn shouldBeEqualTo oversikthendelstilfelle.navn
                        personOversiktStatus.enhet shouldBeEqualTo oversikthendelstilfelle.enhetId
                        personOversiktStatus.motebehovUbehandlet shouldBeEqualTo true
                        personOversiktStatus.moteplanleggerUbehandlet shouldBeEqualTo true
                        personOversiktStatus.oppfolgingsplanLPSBistandUbehandlet shouldBeEqualTo true

                        personOversiktStatus.oppfolgingstilfeller.size shouldBeEqualTo 1
                        checkPersonOppfolgingstilfelle(personOversiktStatus.oppfolgingstilfeller.first(), oversikthendelstilfelle)
                    }
                }

                it("should return Person, no Oppfolgingstilfelle, and then OPPFOLGINGSPLANLPS_BISTAND_MOTTATT") {
                    every {
                        isInvalidToken(any())
                    } returns false
                    every {
                        getTokenFromCookie(any())
                    } returns "token"

                    val oversiktHendelseOPLPSBistandMottatt = generateKOversikthendelse(OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_MOTTATT)
                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelseOPLPSBistandMottatt)

                    with(handleRequest(HttpMethod.Get, url) {
                        call.request.cookies[cookies]
                    }) {
                        response.status() shouldBeEqualTo HttpStatusCode.OK
                        val personOversiktStatus = objectMapper.readValue<List<PersonOversiktStatus>>(response.content!!).first()
                        personOversiktStatus.veilederIdent shouldBeEqualTo null
                        personOversiktStatus.fnr shouldBeEqualTo oversiktHendelseOPLPSBistandMottatt.fnr
                        personOversiktStatus.navn shouldBeEqualTo ""
                        personOversiktStatus.enhet shouldBeEqualTo oversiktHendelseOPLPSBistandMottatt.enhetId
                        personOversiktStatus.motebehovUbehandlet shouldBeEqualTo null
                        personOversiktStatus.moteplanleggerUbehandlet shouldBeEqualTo null
                        personOversiktStatus.oppfolgingsplanLPSBistandUbehandlet shouldBeEqualTo true

                        personOversiktStatus.oppfolgingstilfeller.size shouldBeEqualTo 0
                    }
                }
            }
        }
    }
})

fun checkPersonOppfolgingstilfelle(oppfolgingstilfelle: Oppfolgingstilfelle, oversikthendelsetilfelle: KOversikthendelsetilfelle) {
    oppfolgingstilfelle.virksomhetsnummer shouldBeEqualTo oversikthendelsetilfelle.virksomhetsnummer
    oppfolgingstilfelle.fom shouldBeEqualTo oversikthendelsetilfelle.fom
    oppfolgingstilfelle.tom shouldBeEqualTo oversikthendelsetilfelle.tom
}
