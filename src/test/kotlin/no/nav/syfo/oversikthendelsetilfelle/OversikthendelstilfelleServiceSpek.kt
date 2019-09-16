package no.nav.syfo.oversikthendelsetilfelle

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.jackson.jackson
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.util.InternalAPI
import no.nav.syfo.oversikthendelsetilfelle.domain.KOversikthendelsetilfelle
import no.nav.syfo.oversikthendelsetilfelle.domain.PersonOppfolgingstilfelleInternal
import no.nav.syfo.personstatus.domain.PersonOversiktStatusInternal
import no.nav.syfo.personstatus.domain.VeilederBrukerKnytning
import no.nav.syfo.testutil.*
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_2_FNR
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testutil.UserConstants.NAV_ENHET
import no.nav.syfo.testutil.UserConstants.NAV_ENHET_2
import no.nav.syfo.testutil.UserConstants.VEILEDER_ID
import no.nav.syfo.testutil.UserConstants.VIRKSOMHETSNUMMER
import no.nav.syfo.testutil.UserConstants.VIRKSOMHETSNUMMER_2
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.LocalDate
import java.time.LocalDateTime

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

            val oversikthendelstilfelle = KOversikthendelsetilfelle(
                    fnr = ARBEIDSTAKER_FNR,
                    enhetId = NAV_ENHET,
                    virksomhetsnummer = VIRKSOMHETSNUMMER,
                    gradert = false,
                    fom = LocalDate.now().minusDays(30),
                    tom = LocalDate.now().plusDays(30),
                    tidspunkt = LocalDateTime.now()
            )

            val tilknytning = VeilederBrukerKnytning(
                    VEILEDER_ID,
                    oversikthendelstilfelle.fnr,
                    oversikthendelstilfelle.enhetId
            )

            describe("Person eksisterer ikke") {
                it("skal opprette person, med oppfolgingstilfelle ikke gradert") {
                    val hendelse = oversikthendelstilfelle.copy(gradert = false)
                    oversikthendelstilfelleService.oppdaterPersonMedHendelse(hendelse)

                    val personListe = database.connection.hentPersonResultatInternal(hendelse.fnr)
                    val person = personListe.first()

                    personListe.size shouldBe 1
                    checkPersonOversiktStatus(person, hendelse, null)

                    val oppfolgingstilfeller = database.connection.hentOppfolgingstilfelleResultat(person.id)

                    oppfolgingstilfeller.size shouldBe 1
                    checkPersonOppfolgingstilfelle(oppfolgingstilfeller.first(), hendelse, person.id)
                }

                it("skal opprette person, med oppfolgingstilfelle gradert") {
                    val hendelse = oversikthendelstilfelle.copy(gradert = true)
                    oversikthendelstilfelleService.oppdaterPersonMedHendelse(hendelse)

                    val personListe = database.connection.hentPersonResultatInternal(hendelse.fnr)
                    val person = personListe.first()

                    personListe.size shouldBe 1
                    checkPersonOversiktStatus(person, hendelse, null)

                    val oppfolgingstilfeller = database.connection.hentOppfolgingstilfelleResultat(person.id)

                    oppfolgingstilfeller.size shouldBe 1
                    checkPersonOppfolgingstilfelle(oppfolgingstilfeller.first(), hendelse, person.id)
                }
            }

            describe("Person eksisterer, uendret enhet") {
                it("skal oppdatere person, om person eksisterer i oversikt, med oppfolgingstilfelle ikke gradert") {
                    database.connection.opprettVeilederBrukerKnytning(tilknytning)

                    val hendelse = oversikthendelstilfelle.copy(gradert = false)
                    oversikthendelstilfelleService.oppdaterPersonMedHendelse(hendelse)

                    val personListe = database.connection.hentPersonResultatInternal(hendelse.fnr)
                    val person = personListe.first()

                    personListe.size shouldBe 1
                    checkPersonOversiktStatus(person, hendelse, tilknytning.veilederIdent)

                    val oppfolgingstilfeller = database.connection.hentOppfolgingstilfelleResultat(person.id)

                    oppfolgingstilfeller.size shouldBe 1
                    checkPersonOppfolgingstilfelle(oppfolgingstilfeller.first(), hendelse, person.id)
                }

                it("skal oppdatere person, om person eksisterer i oversikt, med oppfolgingstilfelle gradert") {
                    database.connection.opprettVeilederBrukerKnytning(tilknytning)

                    val hendelse = oversikthendelstilfelle.copy(gradert = true)
                    oversikthendelstilfelleService.oppdaterPersonMedHendelse(hendelse)

                    val personListe = database.connection.hentPersonResultatInternal(hendelse.fnr)
                    val person = personListe.first()

                    personListe.size shouldBe 1
                    checkPersonOversiktStatus(person, hendelse, tilknytning.veilederIdent)

                    val oppfolgingstilfeller = database.connection.hentOppfolgingstilfelleResultat(person.id)

                    oppfolgingstilfeller.size shouldBe 1
                    checkPersonOppfolgingstilfelle(oppfolgingstilfeller.first(), hendelse, person.id)
                }
            }

            describe("Person eksistere, endret enhet") {
                it("skal oppdatere person og nullstille tildelt veileder, om person eksisterer i oversikt og enhet er endret, med oppfolgingstilfelle ikke gradert mottatt") {
                    database.connection.opprettVeilederBrukerKnytning(tilknytning)

                    val hendelse = oversikthendelstilfelle.copy(
                            enhetId = NAV_ENHET_2,
                            gradert = false
                    )
                    oversikthendelstilfelleService.oppdaterPersonMedHendelse(hendelse)

                    val personListe = database.connection.hentPersonResultatInternal(hendelse.fnr)
                    val person = personListe.first()

                    personListe.size shouldBe 1
                    checkPersonOversiktStatus(person, hendelse, null)

                    val oppfolgingstilfeller = database.connection.hentOppfolgingstilfelleResultat(person.id)

                    oppfolgingstilfeller.size shouldBe 1
                    checkPersonOppfolgingstilfelle(oppfolgingstilfeller.first(), hendelse, person.id)
                }

                it("skal oppdatere person og nullstille tildelt veileder, om person eksisterer i oversikt og enhet er endret, med oppfolgingstilfelle gradert mottatt") {
                    database.connection.opprettVeilederBrukerKnytning(tilknytning)

                    val hendelse = oversikthendelstilfelle.copy(
                            enhetId = NAV_ENHET_2,
                            gradert = true
                    )
                    oversikthendelstilfelleService.oppdaterPersonMedHendelse(hendelse)

                    val personListe = database.connection.hentPersonResultatInternal(hendelse.fnr)
                    val person = personListe.first()

                    personListe.size shouldBe 1
                    checkPersonOversiktStatus(person, hendelse, null)

                    val oppfolgingstilfeller = database.connection.hentOppfolgingstilfelleResultat(person.id)

                    oppfolgingstilfeller.size shouldBe 1
                    checkPersonOppfolgingstilfelle(oppfolgingstilfeller.first(), hendelse, person.id)
                }
            }

            describe("Flere oppfolgingstilfeller mottas") {
                it("skal oppdatere person, med flere oppfolgingstilfeller med samme virksomhetsnummer") {
                    database.connection.opprettVeilederBrukerKnytning(tilknytning)

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

                    val personListe = database.connection.hentPersonResultatInternal(oversikthendelsetilfelleMottattSist.fnr)
                    val person = personListe.first()

                    personListe.size shouldBe 1
                    checkPersonOversiktStatus(person, oversikthendelsetilfelleMottattForst, tilknytning.veilederIdent)

                    val oppfolgingstilfeller = database.connection.hentOppfolgingstilfelleResultat(person.id)

                    oppfolgingstilfeller.size shouldBe 1
                    checkPersonOppfolgingstilfelle(oppfolgingstilfeller.first(), oversikthendelsetilfelleMottattSist, person.id)
                }
            }

            it("skal oppdatere person, med flere oppfolgingstilfeller med ulike virksomhetsnummer") {
                database.connection.opprettVeilederBrukerKnytning(tilknytning)

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

                val personListe = database.connection.hentPersonResultatInternal(oversikthendelsetilfelleMottattSist.fnr)
                val person = personListe.first()

                personListe.size shouldBe 1
                checkPersonOversiktStatus(person, oversikthendelsetilfelleMottattForst, tilknytning.veilederIdent)

                val oppfolgingstilfeller = database.connection.hentOppfolgingstilfelleResultat(person.id)

                oppfolgingstilfeller.size shouldBe 2
                checkPersonOppfolgingstilfelle(oppfolgingstilfeller.first(), oversikthendelsetilfelleMottattForst, person.id)
                checkPersonOppfolgingstilfelle(oppfolgingstilfeller.last(), oversikthendelsetilfelleMottattSist, person.id)
            }

            it("skal oppdatere person, med flere oppfolgingstilfeller, ulik person, samme virksomhet") {
                database.connection.opprettVeilederBrukerKnytning(tilknytning)

                val oversikthendelsetilfelleMottattForst = oversikthendelstilfelle.copy(
                        fnr = ARBEIDSTAKER_FNR,
                        virksomhetsnummer = VIRKSOMHETSNUMMER,
                        fom = LocalDate.now().plusDays(120),
                        tom = LocalDate.now().plusDays(120),
                        gradert = true
                )
                val oversikthendelsetilfelleMottattSist = oversikthendelstilfelle.copy(
                        fnr = ARBEIDSTAKER_2_FNR,
                        virksomhetsnummer = VIRKSOMHETSNUMMER,
                        fom = LocalDate.now().plusDays(60),
                        tom = LocalDate.now().plusDays(60),
                        gradert = false
                )
                oversikthendelstilfelleService.oppdaterPersonMedHendelse(oversikthendelsetilfelleMottattForst)

                oversikthendelstilfelleService.oppdaterPersonMedHendelse(oversikthendelsetilfelleMottattSist)

                val personListeForst = database.connection.hentPersonResultatInternal(oversikthendelsetilfelleMottattForst.fnr)
                val personListeSist = database.connection.hentPersonResultatInternal(oversikthendelsetilfelleMottattSist.fnr)
                val personForst = personListeForst.first()
                val personSist = personListeSist.last()

                personListeForst.size shouldBe 1
                personListeSist.size shouldBe 1
                checkPersonOversiktStatus(personForst, oversikthendelsetilfelleMottattForst, tilknytning.veilederIdent)
                checkPersonOversiktStatus(personSist, oversikthendelsetilfelleMottattSist, null)

                val oppfolgingstilfellerForst = database.connection.hentOppfolgingstilfelleResultat(personForst.id)
                val oppfolgingstilfellerSist = database.connection.hentOppfolgingstilfelleResultat(personSist.id)

                oppfolgingstilfellerForst.size shouldBe 1
                oppfolgingstilfellerSist.size shouldBe 1
                checkPersonOppfolgingstilfelle(oppfolgingstilfellerForst.first(), oversikthendelsetilfelleMottattForst, personForst.id)
                checkPersonOppfolgingstilfelle(oppfolgingstilfellerSist.first(), oversikthendelsetilfelleMottattSist, personSist.id)
            }
        }
    }
})

fun checkPersonOversiktStatus(personOversiktStatusInternal: PersonOversiktStatusInternal, oversikthendelsetilfelle: KOversikthendelsetilfelle, veilederIdent: String?) {
    personOversiktStatusInternal.fnr shouldEqual oversikthendelsetilfelle.fnr
    personOversiktStatusInternal.veilederIdent shouldEqual veilederIdent
    personOversiktStatusInternal.enhet shouldEqual oversikthendelsetilfelle.enhetId
    personOversiktStatusInternal.motebehovUbehandlet shouldEqual null
    personOversiktStatusInternal.moteplanleggerUbehandlet shouldEqual null
}

fun checkPersonOppfolgingstilfelle(personOppfolgingstilfelleInternal: PersonOppfolgingstilfelleInternal, oversikthendelsetilfelle: KOversikthendelsetilfelle, personId: Int) {
    personOppfolgingstilfelleInternal.personOversiktStatusId shouldEqual personId
    personOppfolgingstilfelleInternal.virksomhetsnummer shouldEqual oversikthendelsetilfelle.virksomhetsnummer
    personOppfolgingstilfelleInternal.gradert shouldEqual oversikthendelsetilfelle.gradert
    personOppfolgingstilfelleInternal.fom shouldEqual oversikthendelsetilfelle.fom
    personOppfolgingstilfelleInternal.tom shouldEqual oversikthendelsetilfelle.tom
}
