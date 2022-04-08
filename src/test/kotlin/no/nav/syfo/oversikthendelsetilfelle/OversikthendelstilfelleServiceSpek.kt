package no.nav.syfo.oversikthendelsetilfelle

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.jackson.*
import io.ktor.server.testing.*
import io.ktor.util.*
import no.nav.syfo.personstatus.getPersonOversiktStatusList
import no.nav.syfo.testutil.*
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_2_FNR
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testutil.UserConstants.NAV_ENHET_2
import no.nav.syfo.testutil.UserConstants.VIRKSOMHETSNAVN_2
import no.nav.syfo.testutil.UserConstants.VIRKSOMHETSNUMMER
import no.nav.syfo.testutil.UserConstants.VIRKSOMHETSNUMMER_2
import no.nav.syfo.testutil.assertion.checkPPersonOppfolgingstilfelle
import no.nav.syfo.testutil.assertion.checkPPersonOversiktStatus
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.LocalDate
import java.time.LocalDateTime

@InternalAPI
object OversikthendelstilfelleServiceSpek : Spek({

    describe("OversikthendelstilfelleService") {

        val database by lazy { TestDatabase() }
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

            describe("Person eksisterer ikke") {
                it("skal opprette person, med oppfolgingstilfelle ikke gradert") {
                    val hendelse = oversikthendelstilfelle.copy(gradert = false)
                    oversikthendelstilfelleService.oppdaterPersonMedHendelse(
                        oversikthendelsetilfelle = hendelse,
                    )

                    val personListe = database.getPersonOversiktStatusList(fnr = hendelse.fnr)
                    val person = personListe.first()

                    personListe.size shouldBe 1
                    checkPPersonOversiktStatus(
                        pPersonOversiktStatus = person,
                        oversikthendelsetilfelle = hendelse,
                    )

                    val oppfolgingstilfeller = database.hentOppfolgingstilfellerForPerson(personId = person.id)

                    oppfolgingstilfeller.size shouldBe 1
                    checkPPersonOppfolgingstilfelle(
                        pPersonOppfolgingstilfelle = oppfolgingstilfeller.first(),
                        oversikthendelsetilfelle = hendelse,
                        personId = person.id,
                    )
                }

                it("skal opprette person, med oppfolgingstilfelle gradert") {
                    val hendelse = oversikthendelstilfelle.copy(gradert = true)
                    oversikthendelstilfelleService.oppdaterPersonMedHendelse(
                        oversikthendelsetilfelle = hendelse,
                    )

                    val personListe = database.getPersonOversiktStatusList(fnr = hendelse.fnr)
                    val person = personListe.first()

                    personListe.size shouldBe 1
                    checkPPersonOversiktStatus(
                        pPersonOversiktStatus = person,
                        oversikthendelsetilfelle = hendelse,
                    )

                    val oppfolgingstilfeller = database.hentOppfolgingstilfellerForPerson(personId = person.id)

                    oppfolgingstilfeller.size shouldBe 1
                    checkPPersonOppfolgingstilfelle(
                        pPersonOppfolgingstilfelle = oppfolgingstilfeller.first(),
                        oversikthendelsetilfelle = hendelse,
                        personId = person.id,
                    )
                }
            }

            describe("Person eksisterer, uendret enhet") {
                it("skal oppdatere person, om person eksisterer i oversikt, med oppfolgingstilfelle ikke gradert") {
                    val hendelse = oversikthendelstilfelle.copy(gradert = false)
                    oversikthendelstilfelleService.oppdaterPersonMedHendelse(
                        oversikthendelsetilfelle = hendelse,
                    )

                    val personListe = database.getPersonOversiktStatusList(fnr = hendelse.fnr)
                    val person = personListe.first()

                    personListe.size shouldBe 1
                    checkPPersonOversiktStatus(
                        pPersonOversiktStatus = person,
                        oversikthendelsetilfelle = hendelse,
                    )

                    val oppfolgingstilfeller = database.hentOppfolgingstilfellerForPerson(personId = person.id)

                    oppfolgingstilfeller.size shouldBe 1
                    checkPPersonOppfolgingstilfelle(
                        pPersonOppfolgingstilfelle = oppfolgingstilfeller.first(),
                        oversikthendelsetilfelle = hendelse,
                        personId = person.id,
                    )
                }

                it("skal oppdatere person, om person eksisterer i oversikt, med oppfolgingstilfelle gradert") {
                    val hendelse = oversikthendelstilfelle.copy(gradert = true)
                    oversikthendelstilfelleService.oppdaterPersonMedHendelse(
                        oversikthendelsetilfelle = hendelse,
                    )

                    val personListe = database.getPersonOversiktStatusList(fnr = hendelse.fnr)
                    val person = personListe.first()

                    personListe.size shouldBe 1
                    checkPPersonOversiktStatus(
                        pPersonOversiktStatus = person,
                        oversikthendelsetilfelle = hendelse,
                    )

                    val oppfolgingstilfeller = database.hentOppfolgingstilfellerForPerson(person.id)

                    oppfolgingstilfeller.size shouldBe 1
                    checkPPersonOppfolgingstilfelle(
                        pPersonOppfolgingstilfelle = oppfolgingstilfeller.first(),
                        oversikthendelsetilfelle = hendelse,
                        personId = person.id,
                    )
                }
            }

            describe("Person eksistere, endret enhet") {
                it("skal oppdatere person og nullstille tildelt veileder, om person eksisterer i oversikt og enhet er endret, med oppfolgingstilfelle ikke gradert mottatt") {
                    val hendelse = oversikthendelstilfelle.copy(
                        navn = getIdentName(ident = ARBEIDSTAKER_2_FNR),
                        enhetId = NAV_ENHET_2,
                        gradert = false,
                    )
                    oversikthendelstilfelleService.oppdaterPersonMedHendelse(
                        oversikthendelsetilfelle = hendelse,
                    )

                    val personListe = database.getPersonOversiktStatusList(fnr = hendelse.fnr)
                    val person = personListe.first()

                    personListe.size shouldBe 1
                    checkPPersonOversiktStatus(
                        pPersonOversiktStatus = person,
                        oversikthendelsetilfelle = hendelse,
                    )

                    val oppfolgingstilfeller = database.hentOppfolgingstilfellerForPerson(personId = person.id)

                    oppfolgingstilfeller.size shouldBe 1
                    checkPPersonOppfolgingstilfelle(
                        pPersonOppfolgingstilfelle = oppfolgingstilfeller.first(),
                        oversikthendelsetilfelle = hendelse,
                        personId = person.id,
                    )
                }

                it("skal oppdatere person og nullstille tildelt veileder, om person eksisterer i oversikt og enhet er endret, med oppfolgingstilfelle gradert mottatt") {
                    val hendelse = oversikthendelstilfelle.copy(
                        navn = getIdentName(ident = ARBEIDSTAKER_2_FNR),
                        enhetId = NAV_ENHET_2,
                        gradert = true,
                    )
                    oversikthendelstilfelleService.oppdaterPersonMedHendelse(
                        oversikthendelsetilfelle = hendelse,
                    )

                    val personListe = database.getPersonOversiktStatusList(fnr = hendelse.fnr)
                    val person = personListe.first()

                    personListe.size shouldBe 1
                    checkPPersonOversiktStatus(
                        pPersonOversiktStatus = person,
                        oversikthendelsetilfelle = hendelse,
                    )

                    val oppfolgingstilfeller = database.hentOppfolgingstilfellerForPerson(personId = person.id)

                    oppfolgingstilfeller.size shouldBe 1
                    checkPPersonOppfolgingstilfelle(
                        pPersonOppfolgingstilfelle = oppfolgingstilfeller.first(),
                        oversikthendelsetilfelle = hendelse,
                        personId = person.id,
                    )
                }
            }

            describe("Flere oppfolgingstilfeller mottas") {
                it("skal oppdatere person, med flere oppfolgingstilfeller med samme virksomhetsnummer") {
                    val oversikthendelsetilfelleMottattForst = oversikthendelstilfelle.copy(
                        fom = LocalDate.now().plusDays(120),
                        tom = LocalDate.now().plusDays(120),
                        gradert = true,
                    )
                    oversikthendelstilfelleService.oppdaterPersonMedHendelse(
                        oversikthendelsetilfelle = oversikthendelsetilfelleMottattForst,
                    )

                    val oversikthendelsetilfelleMottattSist = oversikthendelstilfelle.copy(
                        fom = LocalDate.now().plusDays(60),
                        tom = LocalDate.now().plusDays(60),
                        gradert = false,
                        tidspunkt = LocalDateTime.now(),
                    )

                    oversikthendelstilfelleService.oppdaterPersonMedHendelse(
                        oversikthendelsetilfelle = oversikthendelsetilfelleMottattSist,
                    )

                    val personListe =
                        database.getPersonOversiktStatusList(fnr = oversikthendelsetilfelleMottattSist.fnr)
                    val person = personListe.first()

                    personListe.size shouldBe 1
                    checkPPersonOversiktStatus(
                        pPersonOversiktStatus = person,
                        oversikthendelsetilfelle = oversikthendelsetilfelleMottattForst,
                    )

                    val oppfolgingstilfeller = database.hentOppfolgingstilfellerForPerson(personId = person.id)

                    oppfolgingstilfeller.size shouldBe 1
                    checkPPersonOppfolgingstilfelle(
                        pPersonOppfolgingstilfelle = oppfolgingstilfeller.first(),
                        oversikthendelsetilfelle = oversikthendelsetilfelleMottattSist,
                        personId = person.id,
                    )
                }
            }

            it("skal oppdatere person, med flere oppfolgingstilfeller med ulike virksomhetsnummer") {
                val oversikthendelsetilfelleMottattForst = oversikthendelstilfelle.copy(
                    virksomhetsnummer = VIRKSOMHETSNUMMER,
                    fom = LocalDate.now().plusDays(120),
                    tom = LocalDate.now().plusDays(120),
                    gradert = true,
                )
                val oversikthendelsetilfelleMottattSist = oversikthendelstilfelle.copy(
                    virksomhetsnummer = VIRKSOMHETSNUMMER_2,
                    fom = LocalDate.now().plusDays(60),
                    tom = LocalDate.now().plusDays(60),
                    gradert = false,
                )
                oversikthendelstilfelleService.oppdaterPersonMedHendelse(
                    oversikthendelsetilfelle = oversikthendelsetilfelleMottattForst,
                )

                oversikthendelstilfelleService.oppdaterPersonMedHendelse(
                    oversikthendelsetilfelle = oversikthendelsetilfelleMottattSist,
                )

                val personListe = database.getPersonOversiktStatusList(oversikthendelsetilfelleMottattSist.fnr)
                val person = personListe.first()

                personListe.size shouldBe 1
                checkPPersonOversiktStatus(
                    pPersonOversiktStatus = person,
                    oversikthendelsetilfelle = oversikthendelsetilfelleMottattForst,
                )

                val oppfolgingstilfeller = database.hentOppfolgingstilfellerForPerson(person.id)

                oppfolgingstilfeller.size shouldBe 2
                checkPPersonOppfolgingstilfelle(
                    pPersonOppfolgingstilfelle = oppfolgingstilfeller.first(),
                    oversikthendelsetilfelle = oversikthendelsetilfelleMottattForst,
                    personId = person.id,
                )
                checkPPersonOppfolgingstilfelle(
                    pPersonOppfolgingstilfelle = oppfolgingstilfeller.last(),
                    oversikthendelsetilfelle = oversikthendelsetilfelleMottattSist,
                    personId = person.id,
                )
            }

            it("skal oppdatere person, med flere oppfolgingstilfeller, ulik person, samme virksomhet") {
                val oversikthendelsetilfelleMottattForst = oversikthendelstilfelle.copy(
                    fnr = ARBEIDSTAKER_FNR,
                    navn = getIdentName(ident = ARBEIDSTAKER_FNR),
                    virksomhetsnummer = VIRKSOMHETSNUMMER,
                    fom = LocalDate.now().plusDays(120),
                    tom = LocalDate.now().plusDays(120),
                    gradert = true,
                )
                val oversikthendelsetilfelleMottattSist = oversikthendelstilfelle.copy(
                    fnr = ARBEIDSTAKER_2_FNR,
                    navn = getIdentName(ident = ARBEIDSTAKER_2_FNR),
                    virksomhetsnummer = VIRKSOMHETSNUMMER,
                    fom = LocalDate.now().plusDays(60),
                    tom = LocalDate.now().plusDays(60),
                    gradert = false,
                )
                oversikthendelstilfelleService.oppdaterPersonMedHendelse(
                    oversikthendelsetilfelle = oversikthendelsetilfelleMottattForst,
                )

                oversikthendelstilfelleService.oppdaterPersonMedHendelse(
                    oversikthendelsetilfelle = oversikthendelsetilfelleMottattSist,
                )

                val personListeForst =
                    database.getPersonOversiktStatusList(fnr = oversikthendelsetilfelleMottattForst.fnr)
                val personListeSist =
                    database.getPersonOversiktStatusList(fnr = oversikthendelsetilfelleMottattSist.fnr)
                val personForst = personListeForst.first()
                val personSist = personListeSist.last()

                personListeForst.size shouldBe 1
                personListeSist.size shouldBe 1
                checkPPersonOversiktStatus(
                    pPersonOversiktStatus = personForst,
                    oversikthendelsetilfelle = oversikthendelsetilfelleMottattForst,
                )
                checkPPersonOversiktStatus(
                    pPersonOversiktStatus = personSist,
                    oversikthendelsetilfelle = oversikthendelsetilfelleMottattSist,
                )

                val oppfolgingstilfellerForst = database.hentOppfolgingstilfellerForPerson(personId = personForst.id)
                val oppfolgingstilfellerSist = database.hentOppfolgingstilfellerForPerson(personId = personSist.id)

                oppfolgingstilfellerForst.size shouldBe 1
                oppfolgingstilfellerSist.size shouldBe 1
                checkPPersonOppfolgingstilfelle(
                    pPersonOppfolgingstilfelle = oppfolgingstilfellerForst.first(),
                    oversikthendelsetilfelle = oversikthendelsetilfelleMottattForst,
                    personId = personForst.id,
                )
                checkPPersonOppfolgingstilfelle(
                    pPersonOppfolgingstilfelle = oppfolgingstilfellerSist.first(),
                    oversikthendelsetilfelle = oversikthendelsetilfelleMottattSist,
                    personId = personSist.id,
                )
            }

            it("Skal ikke oppdatere person, med oppfolgingstilfelle, med nytt virksomhetsnavn hvis den har fra før") {
                val utenVirksomhetsnavn = oversikthendelstilfelle.copy(
                    virksomhetsnavn = null,
                    tidspunkt = LocalDateTime.now(),
                )

                oversikthendelstilfelleService.oppdaterPersonMedHendelse(utenVirksomhetsnavn)
                val person = database.getPersonOversiktStatusList(fnr = utenVirksomhetsnavn.fnr)
                person.size shouldBe 1
                val oppfolgingstilfelle = database.hentOppfolgingstilfellerForPerson(personId = person.first().id)
                oppfolgingstilfelle.first().virksomhetsnavn shouldBeEqualTo null

                oversikthendelstilfelleService.oppdaterPersonMedHendelse(
                    utenVirksomhetsnavn.copy(
                        virksomhetsnavn = VIRKSOMHETSNAVN_2,
                    ),
                )
                val oppdatertOppfolingstilfelle =
                    database.hentOppfolgingstilfellerForPerson(personId = person.first().id)
                oppdatertOppfolingstilfelle.first().virksomhetsnavn shouldBeEqualTo null
            }

            it("Skal oppdatere person, med oppfolgingstilfelle, med nytt virksomhetsnavn hvis den ikke har fra før") {
                val utenVirksomhetsnavn = oversikthendelstilfelle.copy(
                    virksomhetsnavn = null
                )

                oversikthendelstilfelleService.oppdaterPersonMedHendelse(utenVirksomhetsnavn)
                val person = database.getPersonOversiktStatusList(fnr = utenVirksomhetsnavn.fnr)
                person.size shouldBe 1
                val oppfolgingstilfelle = database.hentOppfolgingstilfellerForPerson(personId = person.first().id)
                oppfolgingstilfelle.first().virksomhetsnavn shouldBeEqualTo null

                oversikthendelstilfelleService.oppdaterPersonMedHendelse(
                    utenVirksomhetsnavn.copy(
                        virksomhetsnavn = VIRKSOMHETSNAVN_2,
                        tidspunkt = LocalDateTime.now(),
                    ),
                )
                val oppdatertOppfolingstilfelle =
                    database.hentOppfolgingstilfellerForPerson(personId = person.first().id)
                oppdatertOppfolingstilfelle.first().virksomhetsnavn shouldBeEqualTo VIRKSOMHETSNAVN_2
            }
        }
    }
})
