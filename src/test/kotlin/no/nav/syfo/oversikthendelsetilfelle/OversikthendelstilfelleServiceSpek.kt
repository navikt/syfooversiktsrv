package no.nav.syfo.oversikthendelsetilfelle

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.jackson.*
import io.ktor.server.testing.*
import io.ktor.util.*
import no.nav.syfo.oversikthendelsetilfelle.domain.KOversikthendelsetilfelle
import no.nav.syfo.oversikthendelsetilfelle.domain.PPersonOppfolgingstilfelle
import no.nav.syfo.personstatus.domain.PPersonOversiktStatus
import no.nav.syfo.personstatus.domain.VeilederBrukerKnytning
import no.nav.syfo.personstatus.hentPersonResultatInternal
import no.nav.syfo.personstatus.lagreBrukerKnytningPaEnhet
import no.nav.syfo.testutil.TestDB
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_2_FNR
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_2_NAVN
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_NAVN
import no.nav.syfo.testutil.UserConstants.NAV_ENHET_2
import no.nav.syfo.testutil.UserConstants.VEILEDER_ID
import no.nav.syfo.testutil.UserConstants.VIRKSOMHETSNAVN_2
import no.nav.syfo.testutil.UserConstants.VIRKSOMHETSNUMMER
import no.nav.syfo.testutil.UserConstants.VIRKSOMHETSNUMMER_2
import no.nav.syfo.testutil.dropData
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.LocalDate

@InternalAPI
object OversikthendelstilfelleServiceSpek : Spek({

    describe("OversikthendelstilfelleService") {

        val database by lazy { TestDB() }
        val oversikthendelstilfelleService = OversikthendelstilfelleService(database)

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

            val oversikthendelstilfelle = generateOversikthendelsetilfelle

            val tilknytning = VeilederBrukerKnytning(
                VEILEDER_ID,
                oversikthendelstilfelle.fnr,
                oversikthendelstilfelle.enhetId
            )

            describe("Person eksisterer ikke") {
                it("skal opprette person, med oppfolgingstilfelle ikke gradert") {
                    val hendelse = oversikthendelstilfelle.copy(gradert = false)
                    oversikthendelstilfelleService.oppdaterPersonMedHendelse(hendelse)

                    val personListe = database.hentPersonResultatInternal(hendelse.fnr)
                    val person = personListe.first()

                    personListe.size shouldBe 1
                    checkPersonOversiktStatus(person, hendelse, null)

                    val oppfolgingstilfeller = database.hentOppfolgingstilfellerForPerson(person.id)

                    oppfolgingstilfeller.size shouldBe 1
                    checkPersonOppfolgingstilfelle(oppfolgingstilfeller.first(), hendelse, person.id)
                }

                it("skal opprette person, med oppfolgingstilfelle gradert") {
                    val hendelse = oversikthendelstilfelle.copy(gradert = true)
                    oversikthendelstilfelleService.oppdaterPersonMedHendelse(hendelse)

                    val personListe = database.hentPersonResultatInternal(hendelse.fnr)
                    val person = personListe.first()

                    personListe.size shouldBe 1
                    checkPersonOversiktStatus(person, hendelse, null)

                    val oppfolgingstilfeller = database.hentOppfolgingstilfellerForPerson(person.id)

                    oppfolgingstilfeller.size shouldBe 1
                    checkPersonOppfolgingstilfelle(oppfolgingstilfeller.first(), hendelse, person.id)
                }
            }

            describe("Person eksisterer, uendret enhet") {
                it("skal oppdatere person, om person eksisterer i oversikt, med oppfolgingstilfelle ikke gradert") {
                    database.lagreBrukerKnytningPaEnhet(tilknytning)

                    val hendelse = oversikthendelstilfelle.copy(gradert = false)
                    oversikthendelstilfelleService.oppdaterPersonMedHendelse(hendelse)

                    val personListe = database.hentPersonResultatInternal(hendelse.fnr)
                    val person = personListe.first()

                    personListe.size shouldBe 1
                    checkPersonOversiktStatus(person, hendelse, tilknytning.veilederIdent)

                    val oppfolgingstilfeller = database.hentOppfolgingstilfellerForPerson(person.id)

                    oppfolgingstilfeller.size shouldBe 1
                    checkPersonOppfolgingstilfelle(oppfolgingstilfeller.first(), hendelse, person.id)
                }

                it("skal oppdatere person, om person eksisterer i oversikt, med oppfolgingstilfelle gradert") {
                    database.lagreBrukerKnytningPaEnhet(tilknytning)

                    val hendelse = oversikthendelstilfelle.copy(gradert = true)
                    oversikthendelstilfelleService.oppdaterPersonMedHendelse(hendelse)

                    val personListe = database.hentPersonResultatInternal(hendelse.fnr)
                    val person = personListe.first()

                    personListe.size shouldBe 1
                    checkPersonOversiktStatus(person, hendelse, tilknytning.veilederIdent)

                    val oppfolgingstilfeller = database.hentOppfolgingstilfellerForPerson(person.id)

                    oppfolgingstilfeller.size shouldBe 1
                    checkPersonOppfolgingstilfelle(oppfolgingstilfeller.first(), hendelse, person.id)
                }
            }

            describe("Person eksistere, endret enhet") {
                it("skal oppdatere person og nullstille tildelt veileder, om person eksisterer i oversikt og enhet er endret, med oppfolgingstilfelle ikke gradert mottatt") {
                    database.lagreBrukerKnytningPaEnhet(tilknytning)

                    val hendelse = oversikthendelstilfelle.copy(
                        navn = ARBEIDSTAKER_2_NAVN,
                        enhetId = NAV_ENHET_2,
                        gradert = false
                    )
                    oversikthendelstilfelleService.oppdaterPersonMedHendelse(hendelse)

                    val personListe = database.hentPersonResultatInternal(hendelse.fnr)
                    val person = personListe.first()

                    personListe.size shouldBe 1
                    checkPersonOversiktStatus(person, hendelse, null)

                    val oppfolgingstilfeller = database.hentOppfolgingstilfellerForPerson(person.id)

                    oppfolgingstilfeller.size shouldBe 1
                    checkPersonOppfolgingstilfelle(oppfolgingstilfeller.first(), hendelse, person.id)
                }

                it("skal oppdatere person og nullstille tildelt veileder, om person eksisterer i oversikt og enhet er endret, med oppfolgingstilfelle gradert mottatt") {
                    database.lagreBrukerKnytningPaEnhet(tilknytning)

                    val hendelse = oversikthendelstilfelle.copy(
                        navn = ARBEIDSTAKER_2_NAVN,
                        enhetId = NAV_ENHET_2,
                        gradert = true
                    )
                    oversikthendelstilfelleService.oppdaterPersonMedHendelse(hendelse)

                    val personListe = database.hentPersonResultatInternal(hendelse.fnr)
                    val person = personListe.first()

                    personListe.size shouldBe 1
                    checkPersonOversiktStatus(person, hendelse, null)

                    val oppfolgingstilfeller = database.hentOppfolgingstilfellerForPerson(person.id)

                    oppfolgingstilfeller.size shouldBe 1
                    checkPersonOppfolgingstilfelle(oppfolgingstilfeller.first(), hendelse, person.id)
                }
            }

            describe("Flere oppfolgingstilfeller mottas") {
                it("skal oppdatere person, med flere oppfolgingstilfeller med samme virksomhetsnummer") {
                    database.lagreBrukerKnytningPaEnhet(tilknytning)

                    val oversikthendelsetilfelleMottattForst = oversikthendelstilfelle.copy(
                        fom = LocalDate.now().plusDays(120),
                        tom = LocalDate.now().plusDays(120),
                        gradert = true
                    )
                    val oversikthendelsetilfelleMottattSist = oversikthendelstilfelle.copy(
                        fom = LocalDate.now().plusDays(60),
                        tom = LocalDate.now().plusDays(60),
                        gradert = false
                    )
                    oversikthendelstilfelleService.oppdaterPersonMedHendelse(oversikthendelsetilfelleMottattForst)

                    oversikthendelstilfelleService.oppdaterPersonMedHendelse(oversikthendelsetilfelleMottattSist)

                    val personListe = database.hentPersonResultatInternal(oversikthendelsetilfelleMottattSist.fnr)
                    val person = personListe.first()

                    personListe.size shouldBe 1
                    checkPersonOversiktStatus(person, oversikthendelsetilfelleMottattForst, tilknytning.veilederIdent)

                    val oppfolgingstilfeller = database.hentOppfolgingstilfellerForPerson(person.id)

                    oppfolgingstilfeller.size shouldBe 1
                    checkPersonOppfolgingstilfelle(oppfolgingstilfeller.first(), oversikthendelsetilfelleMottattSist, person.id)
                }
            }

            it("skal oppdatere person, med flere oppfolgingstilfeller med ulike virksomhetsnummer") {
                database.lagreBrukerKnytningPaEnhet(tilknytning)

                val oversikthendelsetilfelleMottattForst = oversikthendelstilfelle.copy(
                    virksomhetsnummer = VIRKSOMHETSNUMMER,
                    fom = LocalDate.now().plusDays(120),
                    tom = LocalDate.now().plusDays(120),
                    gradert = true
                )
                val oversikthendelsetilfelleMottattSist = oversikthendelstilfelle.copy(
                    virksomhetsnummer = VIRKSOMHETSNUMMER_2,
                    fom = LocalDate.now().plusDays(60),
                    tom = LocalDate.now().plusDays(60),
                    gradert = false
                )
                oversikthendelstilfelleService.oppdaterPersonMedHendelse(oversikthendelsetilfelleMottattForst)

                oversikthendelstilfelleService.oppdaterPersonMedHendelse(oversikthendelsetilfelleMottattSist)

                val personListe = database.hentPersonResultatInternal(oversikthendelsetilfelleMottattSist.fnr)
                val person = personListe.first()

                personListe.size shouldBe 1
                checkPersonOversiktStatus(person, oversikthendelsetilfelleMottattForst, tilknytning.veilederIdent)

                val oppfolgingstilfeller = database.hentOppfolgingstilfellerForPerson(person.id)

                oppfolgingstilfeller.size shouldBe 2
                checkPersonOppfolgingstilfelle(oppfolgingstilfeller.first(), oversikthendelsetilfelleMottattForst, person.id)
                checkPersonOppfolgingstilfelle(oppfolgingstilfeller.last(), oversikthendelsetilfelleMottattSist, person.id)
            }

            it("skal oppdatere person, med flere oppfolgingstilfeller, ulik person, samme virksomhet") {
                database.lagreBrukerKnytningPaEnhet(tilknytning)

                val oversikthendelsetilfelleMottattForst = oversikthendelstilfelle.copy(
                    fnr = ARBEIDSTAKER_FNR,
                    navn = ARBEIDSTAKER_NAVN,
                    virksomhetsnummer = VIRKSOMHETSNUMMER,
                    fom = LocalDate.now().plusDays(120),
                    tom = LocalDate.now().plusDays(120),
                    gradert = true
                )
                val oversikthendelsetilfelleMottattSist = oversikthendelstilfelle.copy(
                    fnr = ARBEIDSTAKER_2_FNR,
                    navn = ARBEIDSTAKER_2_NAVN,
                    virksomhetsnummer = VIRKSOMHETSNUMMER,
                    fom = LocalDate.now().plusDays(60),
                    tom = LocalDate.now().plusDays(60),
                    gradert = false
                )
                oversikthendelstilfelleService.oppdaterPersonMedHendelse(oversikthendelsetilfelleMottattForst)

                oversikthendelstilfelleService.oppdaterPersonMedHendelse(oversikthendelsetilfelleMottattSist)

                val personListeForst = database.hentPersonResultatInternal(oversikthendelsetilfelleMottattForst.fnr)
                val personListeSist = database.hentPersonResultatInternal(oversikthendelsetilfelleMottattSist.fnr)
                val personForst = personListeForst.first()
                val personSist = personListeSist.last()

                personListeForst.size shouldBe 1
                personListeSist.size shouldBe 1
                checkPersonOversiktStatus(personForst, oversikthendelsetilfelleMottattForst, tilknytning.veilederIdent)
                checkPersonOversiktStatus(personSist, oversikthendelsetilfelleMottattSist, null)

                val oppfolgingstilfellerForst = database.hentOppfolgingstilfellerForPerson(personForst.id)
                val oppfolgingstilfellerSist = database.hentOppfolgingstilfellerForPerson(personSist.id)

                oppfolgingstilfellerForst.size shouldBe 1
                oppfolgingstilfellerSist.size shouldBe 1
                checkPersonOppfolgingstilfelle(oppfolgingstilfellerForst.first(), oversikthendelsetilfelleMottattForst, personForst.id)
                checkPersonOppfolgingstilfelle(oppfolgingstilfellerSist.first(), oversikthendelsetilfelleMottattSist, personSist.id)
            }

            it("Skal oppdatere person, med oppfolgingstilfelle, med nytt virksomhetsnavn hvis den ikke har fra før") {
                val utenVirksomhetsnavn = oversikthendelstilfelle.copy(
                    virksomhetsnavn = null
                )

                oversikthendelstilfelleService.oppdaterPersonMedHendelse(utenVirksomhetsnavn)
                val person = database.hentPersonResultatInternal(utenVirksomhetsnavn.fnr)
                person.size shouldBe 1
                val oppfolgingstilfelle = database.hentOppfolgingstilfellerForPerson(person.first().id)
                oppfolgingstilfelle.first().virksomhetsnavn shouldBeEqualTo null

                oversikthendelstilfelleService.oppdaterPersonMedHendelse(utenVirksomhetsnavn.copy(virksomhetsnavn = VIRKSOMHETSNAVN_2))
                val oppdatertOppfolingstilfelle = database.hentOppfolgingstilfellerForPerson(person.first().id)
                oppdatertOppfolingstilfelle.first().virksomhetsnavn shouldBeEqualTo VIRKSOMHETSNAVN_2
            }
        }
    }
})

fun checkPersonOversiktStatus(pPersonOversiktStatus: PPersonOversiktStatus, oversikthendelsetilfelle: KOversikthendelsetilfelle, veilederIdent: String?) {
    pPersonOversiktStatus.fnr shouldBeEqualTo oversikthendelsetilfelle.fnr
    pPersonOversiktStatus.navn shouldBeEqualTo oversikthendelsetilfelle.navn
    pPersonOversiktStatus.veilederIdent shouldBeEqualTo veilederIdent
    pPersonOversiktStatus.enhet shouldBeEqualTo oversikthendelsetilfelle.enhetId
    pPersonOversiktStatus.motebehovUbehandlet shouldBeEqualTo null
    pPersonOversiktStatus.moteplanleggerUbehandlet shouldBeEqualTo null
}

fun checkPersonOppfolgingstilfelle(pPersonOppfolgingstilfelle: PPersonOppfolgingstilfelle, oversikthendelsetilfelle: KOversikthendelsetilfelle, personId: Int) {
    pPersonOppfolgingstilfelle.personOversiktStatusId shouldBeEqualTo personId
    pPersonOppfolgingstilfelle.virksomhetsnummer shouldBeEqualTo oversikthendelsetilfelle.virksomhetsnummer
    pPersonOppfolgingstilfelle.gradert shouldBeEqualTo oversikthendelsetilfelle.gradert
    pPersonOppfolgingstilfelle.fom shouldBeEqualTo oversikthendelsetilfelle.fom
    pPersonOppfolgingstilfelle.tom shouldBeEqualTo oversikthendelsetilfelle.tom
}
