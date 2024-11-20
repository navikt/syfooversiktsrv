package no.nav.syfo.personstatus.infrastructure.database

import io.ktor.server.testing.*
import no.nav.syfo.personstatus.domain.PersonIdent
import no.nav.syfo.personstatus.db.createPersonOversiktStatus
import no.nav.syfo.personstatus.db.getPersonOversiktStatusList
import no.nav.syfo.personstatus.domain.Initials
import no.nav.syfo.personstatus.domain.PersonOversiktStatus
import no.nav.syfo.personstatus.domain.SearchQuery
import no.nav.syfo.personstatus.infrastructure.database.repository.PersonOversiktStatusRepository
import no.nav.syfo.testutil.ExternalMockEnvironment
import no.nav.syfo.testutil.UserConstants
import no.nav.syfo.testutil.dropData
import no.nav.syfo.testutil.generator.generateOppfolgingstilfelle
import org.amshove.kluent.*
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.LocalDate
import java.time.Month

val activeOppfolgingstilfelle = generateOppfolgingstilfelle(
    start = LocalDate.now().minusWeeks(15),
    end = LocalDate.now().plusWeeks(1),
    antallSykedager = null,
)
val inactiveOppfolgingstilfelle = activeOppfolgingstilfelle.copy(
    oppfolgingstilfelleEnd = LocalDate.now().minusWeeks(3)
)

class PersonOversiktStatusRepositorySpek : Spek({

    describe(PersonOversiktStatusRepository::class.java.simpleName) {
        with(TestApplicationEngine()) {
            start()
            val externalMockEnvironment = ExternalMockEnvironment.instance
            val database = externalMockEnvironment.database
            val personOversiktStatusRepository = externalMockEnvironment.personOversiktStatusRepository

            beforeEachTest { database.dropData() }
            afterEachGroup { database.dropData() }

            describe("updateArbeidsuforhetvurderingStatus") {
                it("Successfully updates arbeidsuforhet vurdering status to active") {
                    val newPersonOversiktStatus = PersonOversiktStatus(fnr = UserConstants.ARBEIDSTAKER_FNR)
                    database.connection.use { connection ->
                        connection.createPersonOversiktStatus(
                            commit = true,
                            personOversiktStatus = newPersonOversiktStatus,
                        )
                    }

                    val result = personOversiktStatusRepository.updateArbeidsuforhetvurderingStatus(
                        personident = PersonIdent(UserConstants.ARBEIDSTAKER_FNR),
                        isAktivVurdering = true
                    )

                    result.isSuccess shouldBe true
                    val personOversiktStatus = database.getPersonOversiktStatusList(fnr = UserConstants.ARBEIDSTAKER_FNR).first()
                    newPersonOversiktStatus.isAktivArbeidsuforhetvurdering shouldNotBe personOversiktStatus.isAktivArbeidsuforhetvurdering
                    personOversiktStatus.isAktivArbeidsuforhetvurdering shouldBe true
                }

                it("Successfully updates arbeidsuforhet vurdering status from active to not active") {
                    val newPersonOversiktStatus = PersonOversiktStatus(fnr = UserConstants.ARBEIDSTAKER_FNR)
                        .copy(isAktivArbeidsuforhetvurdering = true)
                    database.connection.use { connection ->
                        connection.createPersonOversiktStatus(
                            commit = true,
                            personOversiktStatus = newPersonOversiktStatus,
                        )
                    }

                    val result = personOversiktStatusRepository.updateArbeidsuforhetvurderingStatus(
                        personident = PersonIdent(UserConstants.ARBEIDSTAKER_FNR),
                        isAktivVurdering = false
                    )

                    result.isSuccess shouldBe true
                    val personOversiktStatus = database.getPersonOversiktStatusList(fnr = UserConstants.ARBEIDSTAKER_FNR).first()
                    newPersonOversiktStatus.isAktivArbeidsuforhetvurdering shouldNotBe personOversiktStatus.isAktivArbeidsuforhetvurdering
                    personOversiktStatus.isAktivArbeidsuforhetvurdering shouldBe false
                }

                it("Creates new person when none exist when updating arbeidsuforhet vurdering status") {
                    val result = personOversiktStatusRepository.updateArbeidsuforhetvurderingStatus(
                        personident = PersonIdent(UserConstants.ARBEIDSTAKER_FNR),
                        isAktivVurdering = false
                    )

                    val personOversiktStatus = database.getPersonOversiktStatusList(fnr = UserConstants.ARBEIDSTAKER_FNR)
                    personOversiktStatus.shouldNotBeEmpty()
                    result.isSuccess shouldBe true
                }
            }

            describe("upsertSenOppfolgingKandidatStatus") {
                it("Successfully updates mer oppfolging kandidat status to active") {
                    val newPersonOversiktStatus = PersonOversiktStatus(fnr = UserConstants.ARBEIDSTAKER_FNR)
                    database.connection.use { connection ->
                        connection.createPersonOversiktStatus(
                            commit = true,
                            personOversiktStatus = newPersonOversiktStatus,
                        )
                    }

                    val result = personOversiktStatusRepository.upsertSenOppfolgingKandidat(
                        personident = PersonIdent(UserConstants.ARBEIDSTAKER_FNR),
                        isAktivKandidat = true,
                    )

                    result.isSuccess shouldBe true
                    val pPersonOversiktStatus = database.getPersonOversiktStatusList(fnr = UserConstants.ARBEIDSTAKER_FNR).first()
                    pPersonOversiktStatus.isAktivSenOppfolgingKandidat shouldNotBeEqualTo newPersonOversiktStatus.isAktivSenOppfolgingKandidat
                    pPersonOversiktStatus.isAktivSenOppfolgingKandidat shouldBe true
                }

                it("Successfully updates mer oppfolging kandidat status from active to not active") {
                    val newPersonOversiktStatus = PersonOversiktStatus(fnr = UserConstants.ARBEIDSTAKER_FNR)
                        .copy(isAktivSenOppfolgingKandidat = true)
                    database.connection.use { connection ->
                        connection.createPersonOversiktStatus(
                            commit = true,
                            personOversiktStatus = newPersonOversiktStatus,
                        )
                    }

                    val result = personOversiktStatusRepository.upsertSenOppfolgingKandidat(
                        personident = PersonIdent(UserConstants.ARBEIDSTAKER_FNR),
                        isAktivKandidat = false,
                    )

                    result.isSuccess shouldBe true
                    val pPersonOversiktStatus = database.getPersonOversiktStatusList(fnr = UserConstants.ARBEIDSTAKER_FNR).first()
                    pPersonOversiktStatus.isAktivSenOppfolgingKandidat shouldNotBeEqualTo newPersonOversiktStatus.isAktivSenOppfolgingKandidat
                    pPersonOversiktStatus.isAktivSenOppfolgingKandidat shouldBe false
                }

                it("Creates new person when none exist when upserting mer oppfolging kandidat status") {
                    val result = personOversiktStatusRepository.upsertSenOppfolgingKandidat(
                        personident = PersonIdent(UserConstants.ARBEIDSTAKER_FNR),
                        isAktivKandidat = true,
                    )

                    val pPersonOversiktStatus = database.getPersonOversiktStatusList(fnr = UserConstants.ARBEIDSTAKER_FNR)
                    pPersonOversiktStatus.shouldNotBeEmpty()
                    result.isSuccess shouldBe true
                }
            }

            describe("getPersonOversiktStatus") {
                it("Retrieves person oversikt status when one exists") {
                    val fodselsdato = LocalDate.of(1985, Month.MAY, 17)
                    val newPersonOversiktStatus = PersonOversiktStatus(fnr = UserConstants.ARBEIDSTAKER_FNR, fodselsdato = fodselsdato)
                    database.connection.use { connection ->
                        connection.createPersonOversiktStatus(
                            commit = true,
                            personOversiktStatus = newPersonOversiktStatus,
                        )
                    }

                    val personStatus =
                        personOversiktStatusRepository.getPersonOversiktStatus(PersonIdent(UserConstants.ARBEIDSTAKER_FNR))
                    personStatus shouldNotBe null
                    personStatus?.fnr shouldBeEqualTo newPersonOversiktStatus.fnr
                    personStatus?.fodselsdato shouldBeEqualTo fodselsdato
                }
                it("Handles trying to retrieve non existing person oversikt status") {
                    val personStatus =
                        personOversiktStatusRepository.getPersonOversiktStatus(PersonIdent(UserConstants.ARBEIDSTAKER_FNR))

                    personStatus shouldBe null
                }
            }

            describe("Update aktivitetskrav aktiv vurdering") {
                it("Successfully updates aktiv aktivitetskrav status to active") {
                    val newPersonOversiktStatus = PersonOversiktStatus(fnr = UserConstants.ARBEIDSTAKER_FNR)
                    database.connection.use { connection ->
                        connection.createPersonOversiktStatus(
                            commit = true,
                            personOversiktStatus = newPersonOversiktStatus,
                        )
                    }

                    val result = personOversiktStatusRepository.upsertAktivitetskravAktivStatus(
                        personident = PersonIdent(UserConstants.ARBEIDSTAKER_FNR),
                        isAktivVurdering = true,
                    )

                    result.isSuccess shouldBe true
                    val pPersonOversiktStatus = database.getPersonOversiktStatusList(fnr = UserConstants.ARBEIDSTAKER_FNR).first()
                    pPersonOversiktStatus.isAktivAktivitetskravvurdering shouldNotBeEqualTo newPersonOversiktStatus.isAktivAktivitetskravvurdering
                    pPersonOversiktStatus.isAktivAktivitetskravvurdering shouldBe true
                }

                it("Successfully updates aktivitetskrav aktiv status from active to not active") {
                    val newPersonOversiktStatus = PersonOversiktStatus(fnr = UserConstants.ARBEIDSTAKER_FNR)
                        .copy(isAktivAktivitetskravvurdering = true)
                    database.connection.use { connection ->
                        connection.createPersonOversiktStatus(
                            commit = true,
                            personOversiktStatus = newPersonOversiktStatus,
                        )
                    }

                    val result = personOversiktStatusRepository.upsertAktivitetskravAktivStatus(
                        personident = PersonIdent(UserConstants.ARBEIDSTAKER_FNR),
                        isAktivVurdering = false,
                    )

                    result.isSuccess shouldBe true
                    val pPersonOversiktStatus = database.getPersonOversiktStatusList(fnr = UserConstants.ARBEIDSTAKER_FNR).first()
                    pPersonOversiktStatus.isAktivAktivitetskravvurdering shouldNotBeEqualTo newPersonOversiktStatus.isAktivAktivitetskravvurdering
                    pPersonOversiktStatus.isAktivAktivitetskravvurdering shouldBe false
                }

                it("Creates new person when none exist when upserting mer oppfolging kandidat status") {
                    val result = personOversiktStatusRepository.upsertAktivitetskravAktivStatus(
                        personident = PersonIdent(UserConstants.ARBEIDSTAKER_FNR),
                        isAktivVurdering = true,
                    )

                    val pPersonOversiktStatus = database.getPersonOversiktStatusList(fnr = UserConstants.ARBEIDSTAKER_FNR)
                    pPersonOversiktStatus.shouldNotBeEmpty()
                    result.isSuccess shouldBe true
                }
            }

            describe("Person search on initials and birthdate") {
                val fodselsdato = LocalDate.of(1985, Month.MAY, 17)

                fun searchPerson(initials: String, birthdate: LocalDate): List<PersonOversiktStatus> {
                    val searchQuery = SearchQuery(
                        initials = Initials(initials),
                        birthdate = birthdate
                    )
                    return personOversiktStatusRepository.searchPerson(searchQuery)
                }

                it("finds relevant sykmeldt person when searching with correct initials and birthdate") {
                    val newPersonOversiktStatus = PersonOversiktStatus(
                        fnr = UserConstants.ARBEIDSTAKER_FNR,
                        fodselsdato = fodselsdato,
                        navn = "Fornavn Mellomnavn Etternavn",
                        latestOppfolgingstilfelle = activeOppfolgingstilfelle,
                    )
                    personOversiktStatusRepository.createPersonOversiktStatus(newPersonOversiktStatus)

                    searchPerson(initials = "FME", birthdate = fodselsdato).let {
                        it.size shouldBe 1
                        it.first().navn shouldBeEqualTo "Fornavn Mellomnavn Etternavn"
                    }
                    searchPerson("FE", birthdate = fodselsdato).let {
                        it.size shouldBe 1
                        it.first().navn shouldBeEqualTo "Fornavn Mellomnavn Etternavn"
                    }
                    searchPerson("FM", birthdate = fodselsdato).let {
                        it.size shouldBe 1
                        it.first().navn shouldBeEqualTo "Fornavn Mellomnavn Etternavn"
                    }
                }

                it("finds relevant sykmeldt person when searching with correct lowercase initials and birthdate") {
                    val newPersonOversiktStatus = PersonOversiktStatus(
                        fnr = UserConstants.ARBEIDSTAKER_FNR,
                        fodselsdato = fodselsdato,
                        navn = "Fornavn Mellomnavn Etternavn",
                        latestOppfolgingstilfelle = activeOppfolgingstilfelle,
                    )
                    personOversiktStatusRepository.createPersonOversiktStatus(newPersonOversiktStatus)

                    searchPerson("fme", birthdate = fodselsdato).let {
                        it.size shouldBe 1
                        it.first().navn shouldBeEqualTo "Fornavn Mellomnavn Etternavn"
                    }
                    searchPerson("fe", birthdate = fodselsdato).let {
                        it.size shouldBe 1
                        it.first().navn shouldBeEqualTo "Fornavn Mellomnavn Etternavn"
                    }
                    searchPerson("fm", birthdate = fodselsdato).let {
                        it.size shouldBe 1
                        it.first().navn shouldBeEqualTo "Fornavn Mellomnavn Etternavn"
                    }
                }

                it("returns empty list when not matching birthdate or initials") {
                    val newPersonOversiktStatus = PersonOversiktStatus(
                        fnr = UserConstants.ARBEIDSTAKER_FNR,
                        fodselsdato = fodselsdato,
                        navn = "Fornavn Mellomnavn Etternavn",
                        latestOppfolgingstilfelle = activeOppfolgingstilfelle,
                    )
                    personOversiktStatusRepository.createPersonOversiktStatus(newPersonOversiktStatus)

                    searchPerson("AB", birthdate = LocalDate.now()).size shouldBe 0
                }

                it("returns empty list when matching birthdate but not initials") {
                    val newPersonOversiktStatus = PersonOversiktStatus(
                        fnr = UserConstants.ARBEIDSTAKER_FNR,
                        fodselsdato = fodselsdato,
                        navn = "Fornavn Mellomnavn Etternavn",
                        latestOppfolgingstilfelle = activeOppfolgingstilfelle,
                    )
                    personOversiktStatusRepository.createPersonOversiktStatus(newPersonOversiktStatus)

                    searchPerson("AB", birthdate = fodselsdato).size shouldBe 0
                }

                it("returns empty list when matching initials but not birthdate") {
                    val newPersonOversiktStatus = PersonOversiktStatus(
                        fnr = UserConstants.ARBEIDSTAKER_FNR,
                        fodselsdato = fodselsdato,
                        navn = "Fornavn Mellomnavn Etternavn",
                        latestOppfolgingstilfelle = activeOppfolgingstilfelle,
                    )
                    personOversiktStatusRepository.createPersonOversiktStatus(newPersonOversiktStatus)

                    searchPerson("FME", birthdate = LocalDate.now()).size shouldBe 0
                }

                it("returns empty list when matching initials but person missing birthdate") {
                    val newPersonOversiktStatus = PersonOversiktStatus(
                        fnr = UserConstants.ARBEIDSTAKER_FNR,
                        navn = "Fornavn Mellomnavn Etternavn",
                        latestOppfolgingstilfelle = activeOppfolgingstilfelle,
                    )
                    personOversiktStatusRepository.createPersonOversiktStatus(newPersonOversiktStatus)

                    searchPerson("FME", birthdate = fodselsdato).size shouldBe 0
                }

                it("returns empty list when matching initials and birthdate but no active oppfolgingstilfelle") {
                    val newPersonOversiktStatus = PersonOversiktStatus(
                        fnr = UserConstants.ARBEIDSTAKER_FNR,
                        fodselsdato = fodselsdato,
                        navn = "Fornavn Mellomnavn Etternavn",
                        latestOppfolgingstilfelle = inactiveOppfolgingstilfelle,
                    )
                    personOversiktStatusRepository.createPersonOversiktStatus(newPersonOversiktStatus)

                    searchPerson("FME", birthdate = fodselsdato).size shouldBe 0
                }

                it("returns several persons when relevant") {
                    personOversiktStatusRepository.createPersonOversiktStatus(
                        PersonOversiktStatus(
                            fnr = UserConstants.ARBEIDSTAKER_FNR,
                            fodselsdato = fodselsdato,
                            navn = "Fornavn Mellomnavn Etternavn",
                            latestOppfolgingstilfelle = activeOppfolgingstilfelle,
                        )
                    )
                    personOversiktStatusRepository.createPersonOversiktStatus(
                        PersonOversiktStatus(
                            fnr = UserConstants.ARBEIDSTAKER_2_FNR,
                            fodselsdato = fodselsdato,
                            navn = "Frank Mellomnavnsen Etternavnsen",
                            latestOppfolgingstilfelle = activeOppfolgingstilfelle,
                        )
                    )

                    searchPerson("FME", birthdate = fodselsdato).let {
                        it.size shouldBe 2
                        it.first().navn shouldBeEqualTo "Fornavn Mellomnavn Etternavn"
                        it[1].navn shouldBeEqualTo "Frank Mellomnavnsen Etternavnsen"
                    }
                }

                it("returns several persons when relevant") {
                    personOversiktStatusRepository.createPersonOversiktStatus(
                        PersonOversiktStatus(
                            fnr = UserConstants.ARBEIDSTAKER_FNR,
                            fodselsdato = fodselsdato,
                            navn = "Fornavn Evensen Melonsen",
                            latestOppfolgingstilfelle = activeOppfolgingstilfelle,
                        )
                    )
                    personOversiktStatusRepository.createPersonOversiktStatus(
                        PersonOversiktStatus(
                            fnr = UserConstants.ARBEIDSTAKER_2_FNR,
                            fodselsdato = fodselsdato,
                            navn = "Frank Melonsen Evensen",
                            latestOppfolgingstilfelle = activeOppfolgingstilfelle,
                        )
                    )

                    searchPerson("FE", birthdate = fodselsdato).let {
                        it.size shouldBe 2
                        it.first().navn shouldBeEqualTo "Fornavn Evensen Melonsen"
                        it[1].navn shouldBeEqualTo "Frank Melonsen Evensen"
                    }
                    searchPerson("FM", birthdate = fodselsdato).let {
                        it.size shouldBe 2
                        it.first().navn shouldBeEqualTo "Fornavn Evensen Melonsen"
                        it[1].navn shouldBeEqualTo "Frank Melonsen Evensen"
                    }
                }
            }
        }
    }
})
