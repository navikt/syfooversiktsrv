package no.nav.syfo.personstatus

import io.ktor.server.testing.*
import io.ktor.util.*
import no.nav.syfo.application.api.authentication.installContentNegotiation
import no.nav.syfo.personstatus.domain.*
import no.nav.syfo.testutil.TestDatabase
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testutil.UserConstants.NAV_ENHET
import no.nav.syfo.testutil.UserConstants.NAV_ENHET_2
import no.nav.syfo.testutil.UserConstants.VEILEDER_ID
import no.nav.syfo.testutil.dropData
import no.nav.syfo.testutil.generator.generateKOversikthendelse
import org.amshove.kluent.*
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.LocalDateTime

@InternalAPI
object OversiktHendelseServiceSpek : Spek({

    describe("OversiktHendelseService") {

        val database by lazy { TestDatabase() }
        val oversiktHendelseService = OversiktHendelseService(database)

        afterGroup {
            database.stop()
        }

        with(TestApplicationEngine()) {
            start()

            application.installContentNegotiation()

            beforeEachTest {
                database.connection.dropData()
            }

            describe("Oppdater person basert paa hendelse MOTEBEHOV_SVAR_MOTTATT") {

                it("skal opprette person, om person ikke eksisterer i oversikt") {
                    val oversiktHendelse = KOversikthendelse(
                        ARBEIDSTAKER_FNR,
                        OversikthendelseType.MOTEBEHOV_SVAR_MOTTATT.name,
                        NAV_ENHET,
                        LocalDateTime.now()
                    )

                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelse)

                    val personListe = database.connection.getPersonOversiktStatusList(
                        fnr = oversiktHendelse.fnr,
                    )

                    personListe.size shouldBe 1
                    personListe[0].fnr shouldBeEqualTo oversiktHendelse.fnr
                    personListe[0].veilederIdent shouldBeEqualTo null
                    personListe[0].enhet.shouldBeNull()
                    personListe[0].motebehovUbehandlet shouldBeEqualTo true
                }

                it("skal oppdatere person, om person eksisterer i oversikt") {
                    val tilknytning = VeilederBrukerKnytning(VEILEDER_ID, ARBEIDSTAKER_FNR, NAV_ENHET)
                    val oversiktHendelse = KOversikthendelse(
                        ARBEIDSTAKER_FNR,
                        OversikthendelseType.MOTEBEHOV_SVAR_MOTTATT.name,
                        NAV_ENHET,
                        LocalDateTime.now()
                    )

                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelse)

                    database.lagreBrukerKnytningPaEnhet(tilknytning)

                    val personListe = database.connection.getPersonOversiktStatusList(
                        fnr = oversiktHendelse.fnr,
                    )

                    personListe.size shouldBe 1
                    personListe[0].fnr shouldBeEqualTo oversiktHendelse.fnr
                    personListe[0].veilederIdent shouldBeEqualTo tilknytning.veilederIdent
                    personListe[0].enhet.shouldBeNull()
                    personListe[0].motebehovUbehandlet shouldBeEqualTo true
                }

                it("skal oppdatere person, om person eksisterer i oversikt med motebehov-hendelse") {
                    val oversiktHendelse = KOversikthendelse(
                        ARBEIDSTAKER_FNR,
                        OversikthendelseType.MOTEBEHOV_SVAR_MOTTATT.name,
                        NAV_ENHET,
                        LocalDateTime.now()
                    )

                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelse = oversiktHendelse)

                    val oversiktHendelseNy = KOversikthendelse(
                        ARBEIDSTAKER_FNR,
                        OversikthendelseType.MOTEBEHOV_SVAR_MOTTATT.name,
                        NAV_ENHET_2,
                        LocalDateTime.now()
                    )

                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelseNy)

                    val personListe = database.connection.getPersonOversiktStatusList(
                        fnr = oversiktHendelse.fnr,
                    )

                    personListe.size shouldBe 1
                    personListe[0].fnr shouldBeEqualTo oversiktHendelseNy.fnr
                    personListe[0].veilederIdent shouldBeEqualTo null
                    personListe[0].enhet.shouldBeNull()
                    personListe[0].motebehovUbehandlet shouldBeEqualTo true
                }
            }

            describe("Oppdater person basert paa hendelse ${OversikthendelseType.MOTEBEHOV_SVAR_BEHANDLET.name}") {

                it("skal ikke oppdatere person, om person ikke eksisterer i oversikt") {
                    val oversiktHendelse = KOversikthendelse(
                        ARBEIDSTAKER_FNR,
                        OversikthendelseType.MOTEBEHOV_SVAR_BEHANDLET.name,
                        NAV_ENHET,
                        LocalDateTime.now()
                    )

                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelse)

                    val personListe = database.connection.getPersonOversiktStatusList(
                        fnr = oversiktHendelse.fnr,
                    )

                    personListe.size shouldBe 0
                }

                it("skal oppdatere person, om person eksisterer i oversikt") {
                    val oversiktHendelseMoteplanleggerSvarUbehandlet = KOversikthendelse(
                        ARBEIDSTAKER_FNR,
                        OversikthendelseType.MOTEPLANLEGGER_ALLE_SVAR_MOTTATT.name,
                        NAV_ENHET,
                        LocalDateTime.now()
                    )
                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelseMoteplanleggerSvarUbehandlet)

                    val oversiktHendelseMotebehovUbehandlet = KOversikthendelse(
                        ARBEIDSTAKER_FNR,
                        OversikthendelseType.MOTEBEHOV_SVAR_BEHANDLET.name,
                        NAV_ENHET,
                        LocalDateTime.now()
                    )
                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelseMotebehovUbehandlet)

                    val tilknytning = VeilederBrukerKnytning(VEILEDER_ID, ARBEIDSTAKER_FNR, NAV_ENHET)

                    database.lagreBrukerKnytningPaEnhet(tilknytning)

                    val personListe = database.connection.getPersonOversiktStatusList(
                        fnr = oversiktHendelseMoteplanleggerSvarUbehandlet.fnr,
                    )

                    personListe.size shouldBe 1
                    personListe[0].fnr shouldBeEqualTo oversiktHendelseMotebehovUbehandlet.fnr
                    personListe[0].veilederIdent shouldBeEqualTo tilknytning.veilederIdent
                    personListe[0].enhet.shouldBeNull()
                    personListe[0].motebehovUbehandlet shouldBeEqualTo false
                }

                it("skal oppdatere person, om person eksisterer i oversikt") {
                    val oversiktHendelseMotebehovMottatt = KOversikthendelse(
                        ARBEIDSTAKER_FNR,
                        OversikthendelseType.MOTEBEHOV_SVAR_MOTTATT.name,
                        NAV_ENHET,
                        LocalDateTime.now()
                    )

                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelse = oversiktHendelseMotebehovMottatt)

                    val oversiktHendelseMotebehovUbehandlet = KOversikthendelse(
                        ARBEIDSTAKER_FNR,
                        OversikthendelseType.MOTEBEHOV_SVAR_BEHANDLET.name,
                        NAV_ENHET_2,
                        LocalDateTime.now()
                    )

                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelseMotebehovUbehandlet)

                    val personListe = database.connection.getPersonOversiktStatusList(
                        fnr = oversiktHendelseMotebehovMottatt.fnr,
                    )

                    personListe.size shouldBe 1
                    personListe[0].fnr shouldBeEqualTo oversiktHendelseMotebehovUbehandlet.fnr
                    personListe[0].veilederIdent shouldBeEqualTo null
                    personListe[0].enhet.shouldBeNull()
                    personListe[0].motebehovUbehandlet shouldBeEqualTo false
                }
            }

            describe("Oppdater person basert paa hendelse ${OversikthendelseType.MOTEPLANLEGGER_ALLE_SVAR_MOTTATT.name}") {

                it("skal opprette person, om person ikke eksisterer i oversikt") {
                    val oversiktHendelse = KOversikthendelse(
                        ARBEIDSTAKER_FNR,
                        OversikthendelseType.MOTEPLANLEGGER_ALLE_SVAR_MOTTATT.name,
                        NAV_ENHET,
                        LocalDateTime.now()
                    )

                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelse)

                    val personListe = database.connection.getPersonOversiktStatusList(
                        fnr = oversiktHendelse.fnr,
                    )

                    personListe.size shouldBe 1
                    personListe[0].fnr shouldBeEqualTo oversiktHendelse.fnr
                    personListe[0].veilederIdent shouldBeEqualTo null
                    personListe[0].enhet.shouldBeNull()
                    personListe[0].motebehovUbehandlet shouldBeEqualTo null
                    personListe[0].moteplanleggerUbehandlet shouldBeEqualTo true
                }

                it("skal oppdatere person, om person eksisterer i oversikt") {
                    val tilknytning = VeilederBrukerKnytning(VEILEDER_ID, ARBEIDSTAKER_FNR, NAV_ENHET)
                    val oversiktHendelse = KOversikthendelse(
                        ARBEIDSTAKER_FNR,
                        OversikthendelseType.MOTEPLANLEGGER_ALLE_SVAR_MOTTATT.name,
                        NAV_ENHET,
                        LocalDateTime.now()
                    )

                    database.lagreBrukerKnytningPaEnhet(tilknytning)

                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelse)

                    val personListe = database.connection.getPersonOversiktStatusList(
                        fnr = oversiktHendelse.fnr,
                    )

                    personListe.size shouldBe 1
                    personListe[0].fnr shouldBeEqualTo oversiktHendelse.fnr
                    personListe[0].veilederIdent shouldBeEqualTo tilknytning.veilederIdent
                    personListe[0].enhet.shouldBeNull()
                    personListe[0].motebehovUbehandlet shouldBeEqualTo null
                    personListe[0].moteplanleggerUbehandlet shouldBeEqualTo true
                }

                it("skal oppdatere person, om person eksisterer i oversikt med moteplanlegger_svar_mottatt-hendelse") {
                    val oversiktHendelse = KOversikthendelse(
                        ARBEIDSTAKER_FNR,
                        OversikthendelseType.MOTEPLANLEGGER_ALLE_SVAR_MOTTATT.name,
                        NAV_ENHET,
                        LocalDateTime.now()
                    )

                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelse)

                    val oversiktHendelseNy = KOversikthendelse(
                        ARBEIDSTAKER_FNR,
                        OversikthendelseType.MOTEPLANLEGGER_ALLE_SVAR_MOTTATT.name,
                        NAV_ENHET_2,
                        LocalDateTime.now()
                    )

                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelseNy)

                    val personListe = database.connection.getPersonOversiktStatusList(
                        fnr = oversiktHendelse.fnr,
                    )

                    personListe.size shouldBe 1
                    personListe[0].fnr shouldBeEqualTo oversiktHendelseNy.fnr
                    personListe[0].veilederIdent shouldBeEqualTo null
                    personListe[0].enhet.shouldBeNull()
                    personListe[0].motebehovUbehandlet shouldBeEqualTo null
                    personListe[0].moteplanleggerUbehandlet shouldBeEqualTo true
                }
            }

            describe("Oppdater person basert paa hendelse ${OversikthendelseType.MOTEPLANLEGGER_ALLE_SVAR_BEHANDLET.name}") {

                it("skal ikke oppdatere person, om person ikke eksisterer i oversikt") {
                    val oversiktHendelse = KOversikthendelse(
                        ARBEIDSTAKER_FNR,
                        OversikthendelseType.MOTEPLANLEGGER_ALLE_SVAR_BEHANDLET.name,
                        NAV_ENHET,
                        LocalDateTime.now()
                    )

                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelse)

                    val personListe = database.connection.getPersonOversiktStatusList(
                        fnr = oversiktHendelse.fnr,
                    )

                    personListe.size shouldBe 0
                }

                it("skal oppdatere person, om person eksisterer i oversikt") {
                    val oversiktHendelseMotebehovMottatt = KOversikthendelse(
                        ARBEIDSTAKER_FNR,
                        OversikthendelseType.MOTEBEHOV_SVAR_MOTTATT.name,
                        NAV_ENHET,
                        LocalDateTime.now()
                    )
                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelseMotebehovMottatt)

                    val tilknytning = VeilederBrukerKnytning(VEILEDER_ID, ARBEIDSTAKER_FNR, NAV_ENHET)
                    database.lagreBrukerKnytningPaEnhet(tilknytning)

                    val oversiktHendelseMoteplanleggerSvarUbehandlet = KOversikthendelse(
                        ARBEIDSTAKER_FNR,
                        OversikthendelseType.MOTEPLANLEGGER_ALLE_SVAR_BEHANDLET.name,
                        NAV_ENHET,
                        LocalDateTime.now()
                    )
                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelseMoteplanleggerSvarUbehandlet)

                    val personListe = database.connection.getPersonOversiktStatusList(
                        fnr = oversiktHendelseMotebehovMottatt.fnr,
                    )

                    personListe.size shouldBe 1
                    personListe[0].fnr shouldBeEqualTo oversiktHendelseMoteplanleggerSvarUbehandlet.fnr
                    personListe[0].veilederIdent shouldBeEqualTo tilknytning.veilederIdent
                    personListe[0].enhet.shouldBeNull()
                    personListe[0].motebehovUbehandlet shouldBeEqualTo true
                    personListe[0].moteplanleggerUbehandlet shouldBeEqualTo false
                }

                it("skal oppdatere person, om person eksisterer i oversikt") {
                    val oversiktHendelseMoteplanleggerSvarMottatt = KOversikthendelse(
                        ARBEIDSTAKER_FNR,
                        OversikthendelseType.MOTEPLANLEGGER_ALLE_SVAR_MOTTATT.name,
                        NAV_ENHET,
                        LocalDateTime.now()
                    )
                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelseMoteplanleggerSvarMottatt)

                    val oversiktHendelseMoteplanleggerSvarUbehandlet = KOversikthendelse(
                        ARBEIDSTAKER_FNR,
                        OversikthendelseType.MOTEPLANLEGGER_ALLE_SVAR_BEHANDLET.name,
                        NAV_ENHET_2,
                        LocalDateTime.now()
                    )
                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelseMoteplanleggerSvarUbehandlet)

                    val personListe = database.connection.getPersonOversiktStatusList(
                        fnr = oversiktHendelseMoteplanleggerSvarMottatt.fnr,
                    )

                    personListe.size shouldBe 1
                    personListe[0].fnr shouldBeEqualTo oversiktHendelseMoteplanleggerSvarUbehandlet.fnr
                    personListe[0].veilederIdent shouldBeEqualTo null
                    personListe[0].enhet.shouldBeNull()
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

                    val personListe = database.connection.getPersonOversiktStatusList(
                        fnr = oversiktHendelse.fnr,
                    )

                    personListe.size shouldBe 1
                    personListe[0].let {
                        it.fnr shouldBeEqualTo oversiktHendelse.fnr
                        it.veilederIdent shouldBeEqualTo null
                        it.enhet.shouldBeNull()
                        it.motebehovUbehandlet shouldBeEqualTo null
                        it.moteplanleggerUbehandlet shouldBeEqualTo null
                        it.oppfolgingsplanLPSBistandUbehandlet shouldBeEqualTo true
                    }
                }

                it("skal oppdatere person, om person eksisterer i oversikt") {
                    val oversiktHendelse = generateKOversikthendelse(
                        OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_MOTTATT
                    )
                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelse)

                    val tilknytning = VeilederBrukerKnytning(VEILEDER_ID, ARBEIDSTAKER_FNR, NAV_ENHET)
                    database.lagreBrukerKnytningPaEnhet(tilknytning)

                    val personListe = database.connection.getPersonOversiktStatusList(
                        fnr = oversiktHendelse.fnr,
                    )

                    personListe.size shouldBe 1
                    personListe[0].let {
                        it.fnr shouldBeEqualTo oversiktHendelse.fnr
                        it.veilederIdent shouldBeEqualTo tilknytning.veilederIdent
                        it.enhet.shouldBeNull()
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

                    val personListe = database.connection.getPersonOversiktStatusList(
                        fnr = oversiktHendelse.fnr,
                    )

                    personListe.size shouldBe 1
                    personListe[0].let {
                        it.fnr shouldBeEqualTo oversiktHendelseNy.fnr
                        it.veilederIdent shouldBeEqualTo null
                        it.enhet.shouldBeNull()
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

                    val personListe = database.connection.getPersonOversiktStatusList(
                        fnr = oversiktHendelse.fnr,
                    )

                    personListe.size shouldBe 0
                }

                it("skal oppdatere person, om person eksisterer i oversikt") {
                    val oversiktHendelseMotebehovMottatt = generateKOversikthendelse(
                        OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_MOTTATT
                    )
                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelseMotebehovMottatt)

                    val tilknytning = VeilederBrukerKnytning(VEILEDER_ID, ARBEIDSTAKER_FNR, NAV_ENHET)
                    database.lagreBrukerKnytningPaEnhet(tilknytning)

                    val oversiktHendelseOPLPSBistandUbehandlet = generateKOversikthendelse(
                        OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_BEHANDLET
                    )
                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelseOPLPSBistandUbehandlet)

                    val personListe = database.connection.getPersonOversiktStatusList(
                        fnr = oversiktHendelseMotebehovMottatt.fnr,
                    )

                    personListe.size shouldBe 1
                    personListe[0].let {
                        it.fnr shouldBeEqualTo oversiktHendelseOPLPSBistandUbehandlet.fnr
                        it.veilederIdent shouldBeEqualTo tilknytning.veilederIdent
                        it.enhet.shouldBeNull()
                        it.motebehovUbehandlet shouldBeEqualTo null
                        it.moteplanleggerUbehandlet shouldBeEqualTo null
                        it.oppfolgingsplanLPSBistandUbehandlet shouldBeEqualTo false
                    }
                }
            }
        }
    }
})
