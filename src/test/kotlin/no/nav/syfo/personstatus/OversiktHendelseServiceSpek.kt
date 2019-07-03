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
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.LocalDateTime

@InternalAPI
object OversiktHendelseServiceSpek : Spek({

    val database = TestDB()
    val oversiktHendelseService = OversiktHendelseService(database)

    afterGroup {
        database.stop()
    }

    describe("OversiktHendelseService") {

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
                    personListe[0].fnr shouldEqual oversiktHendelse.fnr
                    personListe[0].veilederIdent shouldEqual null
                    personListe[0].enhet shouldEqual oversiktHendelse.enhetId
                    personListe[0].motebehovUbehandlet shouldEqual true
                }

                it("skal oppdatere person, om person eksisterer i oversikt") {
                    val tilknytning = VeilederBrukerKnytning(VEILEDER_ID, ARBEIDSTAKER_FNR, NAV_ENHET)
                    val oversiktHendelse = KOversikthendelse(ARBEIDSTAKER_FNR, OversikthendelseType.MOTEBEHOV_SVAR_MOTTATT.name, NAV_ENHET, LocalDateTime.now())

                    database.connection.opprettVeilederBrukerKnytning(tilknytning)

                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelse)

                    val personListe = database.connection.hentPersonerTilknyttetEnhet(NAV_ENHET)

                    personListe.size shouldBe 1
                    personListe[0].fnr shouldEqual oversiktHendelse.fnr
                    personListe[0].veilederIdent shouldEqual tilknytning.veilederIdent
                    personListe[0].enhet shouldEqual oversiktHendelse.enhetId
                    personListe[0].motebehovUbehandlet shouldEqual true
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
                    personListe[0].fnr shouldEqual oversiktHendelseNy.fnr
                    personListe[0].veilederIdent shouldEqual null
                    personListe[0].enhet shouldEqual oversiktHendelseNy.enhetId
                    personListe[0].motebehovUbehandlet shouldEqual true
                }

                it("skal oppdatere person og nullstille tildelt veileder, om person eksisterer i oversikt og enhet er endret") {
                    val tilknytning = VeilederBrukerKnytning(VEILEDER_ID, ARBEIDSTAKER_FNR, NAV_ENHET)

                    database.connection.opprettVeilederBrukerKnytning(tilknytning)

                    val oversiktHendelse = KOversikthendelse(ARBEIDSTAKER_FNR, OversikthendelseType.MOTEBEHOV_SVAR_MOTTATT.name, NAV_ENHET_2, LocalDateTime.now())

                    oversiktHendelseService.oppdaterPersonMedHendelse(oversiktHendelse)

                    val personListe = database.connection.hentPersonerTilknyttetEnhet(NAV_ENHET_2)

                    personListe.size shouldBe 1
                    personListe[0].fnr shouldEqual oversiktHendelse.fnr
                    personListe[0].veilederIdent shouldEqual null
                    personListe[0].enhet shouldEqual oversiktHendelse.enhetId
                    personListe[0].motebehovUbehandlet shouldEqual true
                }
            }
        }
    }
})
