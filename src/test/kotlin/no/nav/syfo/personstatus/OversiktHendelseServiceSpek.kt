package no.nav.syfo.personstatus

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.jackson.jackson
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.util.InternalAPI
import no.nav.syfo.personstatus.domain.*
import no.nav.syfo.testutil.*
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testutil.UserConstants.NAV_ENHET
import no.nav.syfo.testutil.UserConstants.NAV_ENHET_2
import no.nav.syfo.testutil.UserConstants.VEILEDER_ID
import no.nav.syfo.testutil.generator.generateKOversikthendelse
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.LocalDateTime

@InternalAPI
object OversiktHendelseServiceSpek : Spek({

    describe("OversiktHendelseService") {

        val database by lazy { TestDB() }
        val oversiktHendelseService = OversiktHendelseService(database)

        afterGroup {
            database.stop()
        }

        with(TestApplicationEngine()) {
            start()

            application.install(ContentNegotiation) {
                jackson {
                    registerKotlinModule()
                    registerModule(JavaTimeModule())
                    configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                }
            }

            beforeEachTest {
            }

            afterEachTest {
                database.connection.dropData()
            }

            describe("Oppdater person basert paa hendelse MOTEBEHOV_SVAR_MOTTATT") {

                it("skal opprette person, om person ikke eksisterer i oversikt") {
                    val oversiktHendelse = KOversikthendelse(ARBEIDSTAKER_FNR, OversikthendelseType.MOTEBEHOV_SVAR_MOTTATT.name, NAV_ENHET, LocalDateTime.now())

                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelse)

                    val personListe = database.connection.hentPersonerTilknyttetEnhet(NAV_ENHET)

                    personListe.size shouldBe 1
                    personListe[0].fnr shouldBeEqualTo oversiktHendelse.fnr
                    personListe[0].veilederIdent shouldBeEqualTo null
                    personListe[0].enhet shouldBeEqualTo oversiktHendelse.enhetId
                    personListe[0].motebehovUbehandlet shouldBeEqualTo true
                }

                it("skal oppdatere person, om person eksisterer i oversikt") {
                    val tilknytning = VeilederBrukerKnytning(VEILEDER_ID, ARBEIDSTAKER_FNR, NAV_ENHET)
                    val oversiktHendelse = KOversikthendelse(ARBEIDSTAKER_FNR, OversikthendelseType.MOTEBEHOV_SVAR_MOTTATT.name, NAV_ENHET, LocalDateTime.now())

                    database.connection.opprettVeilederBrukerKnytning(tilknytning)

                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelse)

                    val personListe = database.connection.hentPersonerTilknyttetEnhet(NAV_ENHET)

                    personListe.size shouldBe 1
                    personListe[0].fnr shouldBeEqualTo oversiktHendelse.fnr
                    personListe[0].veilederIdent shouldBeEqualTo tilknytning.veilederIdent
                    personListe[0].enhet shouldBeEqualTo oversiktHendelse.enhetId
                    personListe[0].motebehovUbehandlet shouldBeEqualTo true
                }

                it("skal oppdatere person, om person eksisterer i oversikt med motebehov-hendelse") {
                    val oversiktHendelse = KOversikthendelse(ARBEIDSTAKER_FNR, OversikthendelseType.MOTEBEHOV_SVAR_MOTTATT.name, NAV_ENHET, LocalDateTime.now())

                    database.connection.opprettPerson(oversiktHendelse)

                    val oversiktHendelseNy = KOversikthendelse(ARBEIDSTAKER_FNR, OversikthendelseType.MOTEBEHOV_SVAR_MOTTATT.name, NAV_ENHET_2, LocalDateTime.now())

                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelseNy)

                    val personListeEnhetTidligere = database.connection.hentPersonerTilknyttetEnhet(NAV_ENHET)

                    personListeEnhetTidligere.size shouldBe 0

                    val personListe = database.connection.hentPersonerTilknyttetEnhet(NAV_ENHET_2)

                    personListe.size shouldBe 1
                    personListe[0].fnr shouldBeEqualTo oversiktHendelseNy.fnr
                    personListe[0].veilederIdent shouldBeEqualTo null
                    personListe[0].enhet shouldBeEqualTo oversiktHendelseNy.enhetId
                    personListe[0].motebehovUbehandlet shouldBeEqualTo true
                }

                it("skal oppdatere person og nullstille tildelt veileder, om person eksisterer i oversikt og enhet er endret") {
                    val tilknytning = VeilederBrukerKnytning(VEILEDER_ID, ARBEIDSTAKER_FNR, NAV_ENHET)

                    database.connection.opprettVeilederBrukerKnytning(tilknytning)

                    val oversiktHendelse = KOversikthendelse(ARBEIDSTAKER_FNR, OversikthendelseType.MOTEBEHOV_SVAR_MOTTATT.name, NAV_ENHET_2, LocalDateTime.now())

                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelse)

                    val personListe = database.connection.hentPersonerTilknyttetEnhet(NAV_ENHET_2)

                    personListe.size shouldBe 1
                    personListe[0].fnr shouldBeEqualTo oversiktHendelse.fnr
                    personListe[0].veilederIdent shouldBeEqualTo null
                    personListe[0].enhet shouldBeEqualTo oversiktHendelse.enhetId
                    personListe[0].motebehovUbehandlet shouldBeEqualTo true
                }
            }

            describe("Oppdater person basert paa hendelse ${OversikthendelseType.MOTEBEHOV_SVAR_BEHANDLET.name}") {

                it("skal ikke oppdatere person, om person ikke eksisterer i oversikt") {
                    val oversiktHendelse = KOversikthendelse(ARBEIDSTAKER_FNR, OversikthendelseType.MOTEBEHOV_SVAR_BEHANDLET.name, NAV_ENHET, LocalDateTime.now())

                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelse)

                    val personListe = database.connection.hentPersonerTilknyttetEnhet(NAV_ENHET)

                    personListe.size shouldBe 0
                }

                it("skal oppdatere person, om person eksisterer i oversikt") {
                    val tilknytning = VeilederBrukerKnytning(VEILEDER_ID, ARBEIDSTAKER_FNR, NAV_ENHET)

                    database.connection.opprettVeilederBrukerKnytning(tilknytning)

                    val oversiktHendelseMotebehovUbehandlet = KOversikthendelse(ARBEIDSTAKER_FNR, OversikthendelseType.MOTEBEHOV_SVAR_BEHANDLET.name, NAV_ENHET, LocalDateTime.now())

                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelseMotebehovUbehandlet)

                    val personListe = database.connection.hentPersonerTilknyttetEnhet(NAV_ENHET)

                    personListe.size shouldBe 1
                    personListe[0].fnr shouldBeEqualTo oversiktHendelseMotebehovUbehandlet.fnr
                    personListe[0].veilederIdent shouldBeEqualTo tilknytning.veilederIdent
                    personListe[0].enhet shouldBeEqualTo oversiktHendelseMotebehovUbehandlet.enhetId
                    personListe[0].motebehovUbehandlet shouldBeEqualTo false
                }

                it("skal oppdatere person, om person eksisterer i oversikt") {
                    val oversiktHendelseMotebehovMottatt = KOversikthendelse(ARBEIDSTAKER_FNR, OversikthendelseType.MOTEBEHOV_SVAR_MOTTATT.name, NAV_ENHET, LocalDateTime.now())

                    database.connection.opprettPerson(oversiktHendelseMotebehovMottatt)

                    val oversiktHendelseMotebehovUbehandlet = KOversikthendelse(ARBEIDSTAKER_FNR, OversikthendelseType.MOTEBEHOV_SVAR_BEHANDLET.name, NAV_ENHET_2, LocalDateTime.now())

                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelseMotebehovUbehandlet)

                    val personListeEnhetTidligere = database.connection.hentPersonerTilknyttetEnhet(NAV_ENHET)

                    personListeEnhetTidligere.size shouldBe 0

                    val personListe = database.connection.hentPersonerTilknyttetEnhet(NAV_ENHET_2)

                    personListe.size shouldBe 1
                    personListe[0].fnr shouldBeEqualTo oversiktHendelseMotebehovUbehandlet.fnr
                    personListe[0].veilederIdent shouldBeEqualTo null
                    personListe[0].enhet shouldBeEqualTo oversiktHendelseMotebehovUbehandlet.enhetId
                    personListe[0].motebehovUbehandlet shouldBeEqualTo false
                }

                it("skal oppdatere person og nullstille tildelt veileder, om person eksisterer i oversikt og enhet er endret") {
                    val tilknytning = VeilederBrukerKnytning(VEILEDER_ID, ARBEIDSTAKER_FNR, NAV_ENHET)

                    database.connection.opprettVeilederBrukerKnytning(tilknytning)

                    val oversiktHendelseMotebehovMottatt = KOversikthendelse(ARBEIDSTAKER_FNR, OversikthendelseType.MOTEBEHOV_SVAR_MOTTATT.name, NAV_ENHET, LocalDateTime.now())

                    database.connection.oppdaterPersonMedMotebehovMottatt(oversiktHendelseMotebehovMottatt)

                    val oversiktHendelseMotebehovUbehandlet = KOversikthendelse(ARBEIDSTAKER_FNR, OversikthendelseType.MOTEBEHOV_SVAR_BEHANDLET.name, NAV_ENHET_2, LocalDateTime.now())

                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelseMotebehovUbehandlet)

                    val personListe = database.connection.hentPersonerTilknyttetEnhet(NAV_ENHET_2)

                    personListe.size shouldBe 1
                    personListe[0].fnr shouldBeEqualTo oversiktHendelseMotebehovUbehandlet.fnr
                    personListe[0].veilederIdent shouldBeEqualTo null
                    personListe[0].enhet shouldBeEqualTo oversiktHendelseMotebehovUbehandlet.enhetId
                    personListe[0].motebehovUbehandlet shouldBeEqualTo false
                }
            }

            describe("Oppdater person basert paa hendelse ${OversikthendelseType.MOTEPLANLEGGER_ALLE_SVAR_MOTTATT.name}") {

                it("skal opprette person, om person ikke eksisterer i oversikt") {
                    val oversiktHendelse = KOversikthendelse(ARBEIDSTAKER_FNR, OversikthendelseType.MOTEPLANLEGGER_ALLE_SVAR_MOTTATT.name, NAV_ENHET, LocalDateTime.now())

                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelse)

                    val personListe = database.connection.hentPersonerTilknyttetEnhet(NAV_ENHET)

                    personListe.size shouldBe 1
                    personListe[0].fnr shouldBeEqualTo oversiktHendelse.fnr
                    personListe[0].veilederIdent shouldBeEqualTo null
                    personListe[0].enhet shouldBeEqualTo oversiktHendelse.enhetId
                    personListe[0].motebehovUbehandlet shouldBeEqualTo null
                    personListe[0].moteplanleggerUbehandlet shouldBeEqualTo true
                }

                it("skal oppdatere person, om person eksisterer i oversikt") {
                    val tilknytning = VeilederBrukerKnytning(VEILEDER_ID, ARBEIDSTAKER_FNR, NAV_ENHET)
                    val oversiktHendelse = KOversikthendelse(ARBEIDSTAKER_FNR, OversikthendelseType.MOTEPLANLEGGER_ALLE_SVAR_MOTTATT.name, NAV_ENHET, LocalDateTime.now())

                    database.connection.opprettVeilederBrukerKnytning(tilknytning)

                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelse)

                    val personListe = database.connection.hentPersonerTilknyttetEnhet(NAV_ENHET)

                    personListe.size shouldBe 1
                    personListe[0].fnr shouldBeEqualTo oversiktHendelse.fnr
                    personListe[0].veilederIdent shouldBeEqualTo tilknytning.veilederIdent
                    personListe[0].enhet shouldBeEqualTo oversiktHendelse.enhetId
                    personListe[0].motebehovUbehandlet shouldBeEqualTo null
                    personListe[0].moteplanleggerUbehandlet shouldBeEqualTo true
                }

                it("skal oppdatere person, om person eksisterer i oversikt med moteplanlegger_svar_mottatt-hendelse") {
                    val oversiktHendelse = KOversikthendelse(ARBEIDSTAKER_FNR, OversikthendelseType.MOTEPLANLEGGER_ALLE_SVAR_MOTTATT.name, NAV_ENHET, LocalDateTime.now())

                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelse)

                    val oversiktHendelseNy = KOversikthendelse(ARBEIDSTAKER_FNR, OversikthendelseType.MOTEPLANLEGGER_ALLE_SVAR_MOTTATT.name, NAV_ENHET_2, LocalDateTime.now())

                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelseNy)

                    val personListeEnhetTidligere = database.connection.hentPersonerTilknyttetEnhet(NAV_ENHET)

                    personListeEnhetTidligere.size shouldBe 0

                    val personListe = database.connection.hentPersonerTilknyttetEnhet(NAV_ENHET_2)

                    personListe.size shouldBe 1
                    personListe[0].fnr shouldBeEqualTo oversiktHendelseNy.fnr
                    personListe[0].veilederIdent shouldBeEqualTo null
                    personListe[0].enhet shouldBeEqualTo oversiktHendelseNy.enhetId
                    personListe[0].motebehovUbehandlet shouldBeEqualTo null
                    personListe[0].moteplanleggerUbehandlet shouldBeEqualTo true
                }

                it("skal oppdatere person og nullstille tildelt veileder, om person eksisterer i oversikt og enhet er endret") {
                    val tilknytning = VeilederBrukerKnytning(VEILEDER_ID, ARBEIDSTAKER_FNR, NAV_ENHET)

                    database.connection.opprettVeilederBrukerKnytning(tilknytning)

                    val oversiktHendelse = KOversikthendelse(ARBEIDSTAKER_FNR, OversikthendelseType.MOTEPLANLEGGER_ALLE_SVAR_MOTTATT.name, NAV_ENHET_2, LocalDateTime.now())

                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelse)

                    val personListe = database.connection.hentPersonerTilknyttetEnhet(NAV_ENHET_2)

                    personListe.size shouldBe 1
                    personListe[0].fnr shouldBeEqualTo oversiktHendelse.fnr
                    personListe[0].veilederIdent shouldBeEqualTo null
                    personListe[0].enhet shouldBeEqualTo oversiktHendelse.enhetId
                    personListe[0].motebehovUbehandlet shouldBeEqualTo null
                    personListe[0].moteplanleggerUbehandlet shouldBeEqualTo true
                }
            }

            describe("Oppdater person basert paa hendelse ${OversikthendelseType.MOTEPLANLEGGER_ALLE_SVAR_BEHANDLET.name}") {

                it("skal ikke oppdatere person, om person ikke eksisterer i oversikt") {
                    val oversiktHendelse = KOversikthendelse(ARBEIDSTAKER_FNR, OversikthendelseType.MOTEPLANLEGGER_ALLE_SVAR_BEHANDLET.name, NAV_ENHET, LocalDateTime.now())

                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelse)

                    val personListe = database.connection.hentPersonerTilknyttetEnhet(NAV_ENHET)

                    personListe.size shouldBe 0
                }

                it("skal oppdatere person, om person eksisterer i oversikt") {
                    val oversiktHendelseMotebehovMottatt = KOversikthendelse(ARBEIDSTAKER_FNR, OversikthendelseType.MOTEBEHOV_SVAR_MOTTATT.name, NAV_ENHET, LocalDateTime.now())
                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelseMotebehovMottatt)

                    val tilknytning = VeilederBrukerKnytning(VEILEDER_ID, ARBEIDSTAKER_FNR, NAV_ENHET)
                    database.connection.tildelVeilederTilPerson(tilknytning)

                    val oversiktHendelseMoteplanleggerSvarUbehandlet = KOversikthendelse(ARBEIDSTAKER_FNR, OversikthendelseType.MOTEPLANLEGGER_ALLE_SVAR_BEHANDLET.name, NAV_ENHET, LocalDateTime.now())
                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelseMoteplanleggerSvarUbehandlet)

                    val personListe = database.connection.hentPersonerTilknyttetEnhet(NAV_ENHET)

                    personListe.size shouldBe 1
                    personListe[0].fnr shouldBeEqualTo oversiktHendelseMoteplanleggerSvarUbehandlet.fnr
                    personListe[0].veilederIdent shouldBeEqualTo tilknytning.veilederIdent
                    personListe[0].enhet shouldBeEqualTo oversiktHendelseMoteplanleggerSvarUbehandlet.enhetId
                    personListe[0].motebehovUbehandlet shouldBeEqualTo true
                    personListe[0].moteplanleggerUbehandlet shouldBeEqualTo false
                }

                it("skal oppdatere person, om person eksisterer i oversikt") {
                    val oversiktHendelseMoteplanleggerSvarMottatt = KOversikthendelse(ARBEIDSTAKER_FNR, OversikthendelseType.MOTEPLANLEGGER_ALLE_SVAR_MOTTATT.name, NAV_ENHET, LocalDateTime.now())
                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelseMoteplanleggerSvarMottatt)

                    val oversiktHendelseMoteplanleggerSvarUbehandlet = KOversikthendelse(ARBEIDSTAKER_FNR, OversikthendelseType.MOTEPLANLEGGER_ALLE_SVAR_BEHANDLET.name, NAV_ENHET_2, LocalDateTime.now())
                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelseMoteplanleggerSvarUbehandlet)

                    val personListeEnhetTidligere = database.connection.hentPersonerTilknyttetEnhet(NAV_ENHET)

                    personListeEnhetTidligere.size shouldBe 0

                    val personListe = database.connection.hentPersonerTilknyttetEnhet(NAV_ENHET_2)

                    personListe.size shouldBe 1
                    personListe[0].fnr shouldBeEqualTo oversiktHendelseMoteplanleggerSvarUbehandlet.fnr
                    personListe[0].veilederIdent shouldBeEqualTo null
                    personListe[0].enhet shouldBeEqualTo oversiktHendelseMoteplanleggerSvarUbehandlet.enhetId
                    personListe[0].motebehovUbehandlet shouldBeEqualTo null
                    personListe[0].moteplanleggerUbehandlet shouldBeEqualTo false
                }

                it("skal oppdatere person og nullstille tildelt veileder, om person eksisterer i oversikt og enhet er endret") {
                    val oversiktHendelseMoteplanleggerSvarMottatt = KOversikthendelse(ARBEIDSTAKER_FNR, OversikthendelseType.MOTEPLANLEGGER_ALLE_SVAR_MOTTATT.name, NAV_ENHET, LocalDateTime.now())
                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelseMoteplanleggerSvarMottatt)

                    val tilknytning = VeilederBrukerKnytning(VEILEDER_ID, ARBEIDSTAKER_FNR, NAV_ENHET)
                    database.connection.tildelVeilederTilPerson(tilknytning)

                    val oversiktHendelseMoteplanleggerSvarUbehandlet = KOversikthendelse(ARBEIDSTAKER_FNR, OversikthendelseType.MOTEPLANLEGGER_ALLE_SVAR_BEHANDLET.name, NAV_ENHET_2, LocalDateTime.now())
                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelseMoteplanleggerSvarUbehandlet)

                    val personListe = database.connection.hentPersonerTilknyttetEnhet(NAV_ENHET_2)

                    personListe.size shouldBe 1
                    personListe[0].fnr shouldBeEqualTo oversiktHendelseMoteplanleggerSvarUbehandlet.fnr
                    personListe[0].veilederIdent shouldBeEqualTo null
                    personListe[0].enhet shouldBeEqualTo oversiktHendelseMoteplanleggerSvarUbehandlet.enhetId
                    personListe[0].motebehovUbehandlet shouldBeEqualTo null
                    personListe[0].moteplanleggerUbehandlet shouldBeEqualTo false
                }
            }

            describe("Oppdater person basert paa hendelse ${OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_MOTTATT.name}") {

                it("skal opprette person, om person ikke eksisterer i oversikt") {
                    val oversiktHendelse = generateKOversikthendelse(
                        OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_MOTTATT
                    )

                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelse)

                    val personListe = database.connection.hentPersonerTilknyttetEnhet(NAV_ENHET)

                    personListe.size shouldBe 1
                    personListe[0].let {
                        it.fnr shouldBeEqualTo oversiktHendelse.fnr
                        it.veilederIdent shouldBeEqualTo null
                        it.enhet shouldBeEqualTo oversiktHendelse.enhetId
                        it.motebehovUbehandlet shouldBeEqualTo null
                        it.moteplanleggerUbehandlet shouldBeEqualTo null
                        it.oppfolgingsplanLPSBistandUbehandlet shouldBeEqualTo true
                    }
                }

                it("skal oppdatere person, om person eksisterer i oversikt") {
                    val tilknytning = VeilederBrukerKnytning(VEILEDER_ID, ARBEIDSTAKER_FNR, NAV_ENHET)
                    val oversiktHendelse = generateKOversikthendelse(
                        OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_MOTTATT
                    )

                    database.connection.opprettVeilederBrukerKnytning(tilknytning)

                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelse)

                    val personListe = database.connection.hentPersonerTilknyttetEnhet(NAV_ENHET)

                    personListe.size shouldBe 1
                    personListe[0].let {
                        it.fnr shouldBeEqualTo oversiktHendelse.fnr
                        it.veilederIdent shouldBeEqualTo tilknytning.veilederIdent
                        it.enhet shouldBeEqualTo oversiktHendelse.enhetId
                        it.motebehovUbehandlet shouldBeEqualTo null
                        it.moteplanleggerUbehandlet shouldBeEqualTo null
                        it.oppfolgingsplanLPSBistandUbehandlet shouldBeEqualTo true
                    }
                }

                it("skal oppdatere person med ny enhet, om person eksisterer i oversikt") {
                    val oversiktHendelse = generateKOversikthendelse(
                        OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_MOTTATT
                    )

                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelse)

                    val oversiktHendelseNy = generateKOversikthendelse(
                        OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_MOTTATT
                    ).copy(enhetId = NAV_ENHET_2)

                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelseNy)

                    val personListeEnhetTidligere = database.connection.hentPersonerTilknyttetEnhet(NAV_ENHET)

                    personListeEnhetTidligere.size shouldBe 0

                    val personListe = database.connection.hentPersonerTilknyttetEnhet(NAV_ENHET_2)

                    personListe.size shouldBe 1
                    personListe[0].let {
                        it.fnr shouldBeEqualTo oversiktHendelseNy.fnr
                        it.veilederIdent shouldBeEqualTo null
                        it.enhet shouldBeEqualTo oversiktHendelseNy.enhetId
                        it.motebehovUbehandlet shouldBeEqualTo null
                        it.moteplanleggerUbehandlet shouldBeEqualTo null
                        it.oppfolgingsplanLPSBistandUbehandlet shouldBeEqualTo true
                    }
                }

                it("skal oppdatere person og nullstille tildelt veileder, om person eksisterer i oversikt og enhet er endret") {
                    val tilknytning = VeilederBrukerKnytning(VEILEDER_ID, ARBEIDSTAKER_FNR, NAV_ENHET)

                    database.connection.opprettVeilederBrukerKnytning(tilknytning)

                    val oversiktHendelse = generateKOversikthendelse(
                        OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_MOTTATT
                    ).copy(enhetId = NAV_ENHET_2)
                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelse)

                    val personListe = database.connection.hentPersonerTilknyttetEnhet(NAV_ENHET_2)

                    personListe.size shouldBe 1
                    personListe[0].let {
                        it.fnr shouldBeEqualTo oversiktHendelse.fnr
                        it.veilederIdent shouldBeEqualTo null
                        it.enhet shouldBeEqualTo oversiktHendelse.enhetId
                        it.motebehovUbehandlet shouldBeEqualTo null
                        it.moteplanleggerUbehandlet shouldBeEqualTo null
                        it.oppfolgingsplanLPSBistandUbehandlet shouldBeEqualTo true
                    }
                }
            }

            describe("Oppdater person basert paa hendelse ${OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_BEHANDLET.name}") {

                it("skal ikke oppdatere person, om person ikke eksisterer i oversikt") {
                    val oversiktHendelse = generateKOversikthendelse(
                        OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_BEHANDLET
                    )
                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelse)

                    val personListe = database.connection.hentPersonerTilknyttetEnhet(NAV_ENHET)

                    personListe.size shouldBe 0
                }

                it("skal oppdatere person, om person eksisterer i oversikt") {
                    val oversiktHendelseMotebehovMottatt = generateKOversikthendelse(
                        OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_MOTTATT
                    )
                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelseMotebehovMottatt)

                    val tilknytning = VeilederBrukerKnytning(VEILEDER_ID, ARBEIDSTAKER_FNR, NAV_ENHET)
                    database.connection.tildelVeilederTilPerson(tilknytning)

                    val oversiktHendelseOPLPSBistandUbehandlet = generateKOversikthendelse(
                        OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_BEHANDLET
                    )
                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelseOPLPSBistandUbehandlet)

                    val personListe = database.connection.hentPersonerTilknyttetEnhet(NAV_ENHET)

                    personListe.size shouldBe 1
                    personListe[0].let {
                        it.fnr shouldBeEqualTo oversiktHendelseOPLPSBistandUbehandlet.fnr
                        it.veilederIdent shouldBeEqualTo tilknytning.veilederIdent
                        it.enhet shouldBeEqualTo oversiktHendelseOPLPSBistandUbehandlet.enhetId
                        it.motebehovUbehandlet shouldBeEqualTo null
                        it.moteplanleggerUbehandlet shouldBeEqualTo null
                        it.oppfolgingsplanLPSBistandUbehandlet shouldBeEqualTo false
                    }
                }

                it("skal oppdatere person med ny enhet, om person eksisterer i oversikt") {
                    val oversiktHendelseOPLPSBistandMottatt = generateKOversikthendelse(
                        OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_MOTTATT
                    )
                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelseOPLPSBistandMottatt)

                    val oversiktHendelseOPLPSBistandUbehandlet = generateKOversikthendelse(
                        OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_BEHANDLET
                    ).copy(enhetId = NAV_ENHET_2)
                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelseOPLPSBistandUbehandlet)

                    val personListeEnhetTidligere = database.connection.hentPersonerTilknyttetEnhet(NAV_ENHET)

                    personListeEnhetTidligere.size shouldBe 0

                    val personListe = database.connection.hentPersonerTilknyttetEnhet(NAV_ENHET_2)

                    personListe.size shouldBe 1
                    personListe[0].let {
                        it.fnr shouldBeEqualTo oversiktHendelseOPLPSBistandUbehandlet.fnr
                        it.veilederIdent shouldBeEqualTo null
                        it.enhet shouldBeEqualTo oversiktHendelseOPLPSBistandUbehandlet.enhetId
                        it.motebehovUbehandlet shouldBeEqualTo null
                        it.moteplanleggerUbehandlet shouldBeEqualTo null
                        it.oppfolgingsplanLPSBistandUbehandlet shouldBeEqualTo false
                    }
                }

                it("skal oppdatere person og nullstille tildelt veileder, om person eksisterer i oversikt og enhet er endret") {
                    val oversiktHendelseOPLPSBistandMottatt = generateKOversikthendelse(
                        OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_MOTTATT
                    )
                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelseOPLPSBistandMottatt)

                    val tilknytning = VeilederBrukerKnytning(VEILEDER_ID, ARBEIDSTAKER_FNR, NAV_ENHET)
                    database.connection.tildelVeilederTilPerson(tilknytning)

                    val oversiktHendelseOPLPSBistandUbehandlet = generateKOversikthendelse(
                        OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_BEHANDLET
                    ).copy(enhetId = NAV_ENHET_2)

                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelseOPLPSBistandUbehandlet)

                    val personListe = database.connection.hentPersonerTilknyttetEnhet(NAV_ENHET_2)

                    personListe.size shouldBe 1
                    personListe[0].let {
                        it.fnr shouldBeEqualTo oversiktHendelseOPLPSBistandUbehandlet.fnr
                        it.veilederIdent shouldBeEqualTo null
                        it.enhet shouldBeEqualTo oversiktHendelseOPLPSBistandUbehandlet.enhetId
                        it.motebehovUbehandlet shouldBeEqualTo null
                        it.moteplanleggerUbehandlet shouldBeEqualTo null
                        it.oppfolgingsplanLPSBistandUbehandlet shouldBeEqualTo false
                    }
                }
            }
        }
    }
})
