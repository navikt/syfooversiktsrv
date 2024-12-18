package no.nav.syfo.personstatus.infrastructure.database

import io.ktor.server.testing.*
import no.nav.syfo.personstatus.db.createPersonOversiktStatus
import no.nav.syfo.personstatus.db.getPersonOversiktStatusList
import no.nav.syfo.personstatus.domain.Initials
import no.nav.syfo.personstatus.domain.PersonIdent
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

                it("finds person with oppgave but not sykmeldt when searching with correct initials and birthdate") {
                    val newPersonOversiktStatus = PersonOversiktStatus(
                        fnr = UserConstants.ARBEIDSTAKER_FNR,
                        fodselsdato = fodselsdato,
                        navn = "Fornavn Mellomnavn Etternavn",
                        isAktivOppfolgingsoppgave = true,
                        latestOppfolgingstilfelle = inactiveOppfolgingstilfelle,
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

                it("returns empty list when matching initials and birthdate but no active oppfolgingstilfelle or oppgave") {
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

            describe("updatePersonstatusesWithNavnAndFodselsdato") {
                it("updates navn") {
                    val createdPersonStatus = personOversiktStatusRepository.createPersonOversiktStatus(
                        PersonOversiktStatus(fnr = UserConstants.ARBEIDSTAKER_FNR)
                    )
                    val updatedPersonStatus = createdPersonStatus.updatePersonDetails(navn = "Stian Sykmeldt")

                    val updatesPersonStatuses =
                        personOversiktStatusRepository.updatePersonstatusesWithNavnAndFodselsdato(listOf(updatedPersonStatus))

                    updatesPersonStatuses.first().map { it.navn shouldBeEqualTo "Stian Sykmeldt" }
                }

                it("updates fodselsdato") {
                    val createdPersonStatus = personOversiktStatusRepository.createPersonOversiktStatus(
                        PersonOversiktStatus(fnr = UserConstants.ARBEIDSTAKER_FNR)
                    )
                    val updatedPersonStatus = createdPersonStatus.updatePersonDetails(fodselsdato = LocalDate.now())

                    val updatesPersonStatuses =
                        personOversiktStatusRepository.updatePersonstatusesWithNavnAndFodselsdato(listOf(updatedPersonStatus))

                    updatesPersonStatuses.first().map { it.fodselsdato shouldBeEqualTo updatedPersonStatus.fodselsdato }
                }

                it("updates navn and fodselsdato") {
                    val createdPersonStatus = personOversiktStatusRepository.createPersonOversiktStatus(
                        PersonOversiktStatus(fnr = UserConstants.ARBEIDSTAKER_FNR)
                    )
                    val updatedPersonStatus = createdPersonStatus.apply {
                        updatePersonDetails(navn = "Stian Sykmeldt")
                        updatePersonDetails(fodselsdato = LocalDate.now())
                    }

                    val updatesPersonStatuses =
                        personOversiktStatusRepository.updatePersonstatusesWithNavnAndFodselsdato(listOf(updatedPersonStatus))

                    updatesPersonStatuses.first().map { it.navn shouldBeEqualTo updatedPersonStatus.navn }
                    updatesPersonStatuses.first().map { it.fodselsdato shouldBeEqualTo updatedPersonStatus.fodselsdato }
                }
            }

            describe("getPersonstatusesWithoutNavnOrFodselsdato") {
                it("correctly finds persons with missing name or fodselsdato, and active oppgave, but not active oppfolgingstilfelle") {
                    val personNotMissingAnything = personOversiktStatusRepository.createPersonOversiktStatus(
                        PersonOversiktStatus(
                            fnr = UserConstants.ARBEIDSTAKER_FNR,
                            navn = "Stian Sykmeldt",
                            fodselsdato = LocalDate.now(),
                            isAktivAktivitetskravvurdering = true
                        )
                    )
                    val personMissingName = personOversiktStatusRepository.createPersonOversiktStatus(
                        PersonOversiktStatus(
                            fnr = UserConstants.ARBEIDSTAKER_2_FNR,
                            isAktivAktivitetskravvurdering = true
                        )
                    )

                    val personsWithMissingName = personOversiktStatusRepository.getPersonstatusesWithoutNavnOrFodselsdato(10)

                    personsWithMissingName.size shouldBeEqualTo 1
                    personsWithMissingName.first().fnr shouldBeEqualTo personMissingName.fnr
                    personNotMissingAnything shouldNotBeIn personsWithMissingName
                }

                it("correctly finds persons missing either name or fodselsdato") {
                    val personWithNameMissingFodselsdato = personOversiktStatusRepository.createPersonOversiktStatus(
                        PersonOversiktStatus(
                            fnr = UserConstants.ARBEIDSTAKER_FNR,
                            navn = "Stian Sykmeldt",
                            isAktivAktivitetskravvurdering = true
                        )
                    )
                    val personWithFodselsdatoMissingName = personOversiktStatusRepository.createPersonOversiktStatus(
                        PersonOversiktStatus(
                            fnr = UserConstants.ARBEIDSTAKER_2_FNR,
                            fodselsdato = LocalDate.now(),
                            isAktivAktivitetskravvurdering = true
                        )
                    )

                    val persons = personOversiktStatusRepository.getPersonstatusesWithoutNavnOrFodselsdato(10)

                    persons.size shouldBeEqualTo 2
                    persons.first().fnr shouldBeEqualTo personWithNameMissingFodselsdato.fnr
                    persons[1].fnr shouldBeEqualTo personWithFodselsdatoMissingName.fnr
                }

                it("correctly finds persons with missing name, and not active oppgave, but active oppfolgingstilfelle") {
                    val personNotActiveOppfolgingstilfelle = personOversiktStatusRepository.createPersonOversiktStatus(
                        PersonOversiktStatus(
                            fnr = UserConstants.ARBEIDSTAKER_FNR,
                            latestOppfolgingstilfelle = generateOppfolgingstilfelle(
                                start = LocalDate.now().minusDays(30),
                                end = LocalDate.now().minusDays(20),
                                antallSykedager = 10,
                            )
                        )
                    )
                    val personActiveOppfolgingstilfelle = personOversiktStatusRepository.createPersonOversiktStatus(
                        PersonOversiktStatus(
                            fnr = UserConstants.ARBEIDSTAKER_2_FNR,
                            latestOppfolgingstilfelle = generateOppfolgingstilfelle(
                                start = LocalDate.now().minusDays(10),
                                end = LocalDate.now().minusDays(5),
                                antallSykedager = 10,
                            )
                        )
                    )

                    val personsWithMissingName = personOversiktStatusRepository.getPersonstatusesWithoutNavnOrFodselsdato(10)

                    personsWithMissingName.size shouldBeEqualTo 1
                    personsWithMissingName.first().fnr shouldBeEqualTo personActiveOppfolgingstilfelle.fnr
                    personNotActiveOppfolgingstilfelle shouldNotBeIn personsWithMissingName
                }

                it("correctly returns empty list when no persons with active oppgave, or active oppfolgingstilfelle") {
                    personOversiktStatusRepository.createPersonOversiktStatus(
                        PersonOversiktStatus(
                            fnr = UserConstants.ARBEIDSTAKER_FNR,
                            latestOppfolgingstilfelle = generateOppfolgingstilfelle(
                                start = LocalDate.now().minusDays(40),
                                end = LocalDate.now().minusDays(20),
                                antallSykedager = 10,
                            )
                        )
                    )
                    personOversiktStatusRepository.createPersonOversiktStatus(
                        PersonOversiktStatus(
                            fnr = UserConstants.ARBEIDSTAKER_2_FNR,
                            latestOppfolgingstilfelle = generateOppfolgingstilfelle(
                                start = LocalDate.now().minusDays(30),
                                end = LocalDate.now().minusDays(17),
                                antallSykedager = 10,
                            )
                        )
                    )

                    val personsWithMissingName = personOversiktStatusRepository.getPersonstatusesWithoutNavnOrFodselsdato(10)

                    personsWithMissingName.size shouldBeEqualTo 0
                }
            }
        }
    }
})
