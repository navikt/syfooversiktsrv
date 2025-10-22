package no.nav.syfo.personstatus.infrastructure.database

import no.nav.syfo.personstatus.api.v2.model.SearchQueryDTO
import no.nav.syfo.personstatus.domain.PersonIdent
import no.nav.syfo.personstatus.domain.PersonOversiktStatus
import no.nav.syfo.personstatus.infrastructure.database.queries.createPersonOversiktStatus
import no.nav.syfo.personstatus.infrastructure.database.queries.getPersonOversiktStatusList
import no.nav.syfo.testutil.ExternalMockEnvironment
import no.nav.syfo.testutil.UserConstants
import no.nav.syfo.testutil.generator.generateOppfolgingstilfelle
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
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

class PersonOversiktStatusRepositoryTest {

    private val externalMockEnvironment = ExternalMockEnvironment.instance
    private val database = externalMockEnvironment.database
    private val personOversiktStatusRepository = externalMockEnvironment.personOversiktStatusRepository
    private val arbeidstakerFnr = PersonIdent(UserConstants.ARBEIDSTAKER_FNR)

    @BeforeEach
    fun setUp() {
        database.resetDatabase()
    }

    @AfterEach
    fun tearDown() {
        database.resetDatabase()
    }

    @Nested
    @DisplayName("Update arbeidsuforhetvurderingStatus")
    inner class UpdateArbeidsuforhetvurderingStatus {

        @Test
        fun `Successfully updates arbeidsuforhet vurdering status to active`() {
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

            assertTrue(result.isSuccess)
            val personOversiktStatus = database.getPersonOversiktStatusList(fnr = UserConstants.ARBEIDSTAKER_FNR).first()
            assertNotEquals(newPersonOversiktStatus.isAktivArbeidsuforhetvurdering, personOversiktStatus.isAktivArbeidsuforhetvurdering)
            assertTrue(personOversiktStatus.isAktivArbeidsuforhetvurdering)
        }

        @Test
        fun `Successfully updates arbeidsuforhet vurdering status from active to not active`() {
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

            assertTrue(result.isSuccess)
            val personOversiktStatus = database.getPersonOversiktStatusList(fnr = UserConstants.ARBEIDSTAKER_FNR).first()
            assertNotEquals(newPersonOversiktStatus.isAktivArbeidsuforhetvurdering, personOversiktStatus.isAktivArbeidsuforhetvurdering)
            assertFalse(personOversiktStatus.isAktivArbeidsuforhetvurdering)
        }

        @Test
        fun `Creates new person when none exist when updating arbeidsuforhet vurdering status`() {
            val result = personOversiktStatusRepository.updateArbeidsuforhetvurderingStatus(
                personident = arbeidstakerFnr,
                isAktivVurdering = false
            )

            val personOversiktStatus = database.getPersonOversiktStatusList(fnr = UserConstants.ARBEIDSTAKER_FNR)
            assertTrue(personOversiktStatus.isNotEmpty())
            assertTrue(result.isSuccess)
        }
    }

    @Nested
    @DisplayName("Upsert Sen oppfolging kandidatStatus")
    inner class UpsertSenOppfolgingKandidatStatus {

        @Test
        fun `Successfully updates mer oppfolging kandidat status to active`() {
            val newPersonOversiktStatus = PersonOversiktStatus(fnr = UserConstants.ARBEIDSTAKER_FNR)
            database.connection.use { connection ->
                connection.createPersonOversiktStatus(
                    commit = true,
                    personOversiktStatus = newPersonOversiktStatus,
                )
            }

            val result = personOversiktStatusRepository.upsertSenOppfolgingKandidat(
                personident = arbeidstakerFnr,
                isAktivKandidat = true,
            )

            assertTrue(result.isSuccess)
            val pPersonOversiktStatus = database.getPersonOversiktStatusList(fnr = UserConstants.ARBEIDSTAKER_FNR).first()
            assertNotEquals(newPersonOversiktStatus.isAktivSenOppfolgingKandidat, pPersonOversiktStatus.isAktivSenOppfolgingKandidat)
            assertTrue(pPersonOversiktStatus.isAktivSenOppfolgingKandidat)
        }

        @Test
        fun `Successfully updates mer oppfolging kandidat status from active to not active`() {
            val newPersonOversiktStatus = PersonOversiktStatus(fnr = UserConstants.ARBEIDSTAKER_FNR)
                .copy(isAktivSenOppfolgingKandidat = true)
            database.connection.use { connection ->
                connection.createPersonOversiktStatus(
                    commit = true,
                    personOversiktStatus = newPersonOversiktStatus,
                )
            }

            val result = personOversiktStatusRepository.upsertSenOppfolgingKandidat(
                personident = arbeidstakerFnr,
                isAktivKandidat = false,
            )

            assertTrue(result.isSuccess)
            val pPersonOversiktStatus = database.getPersonOversiktStatusList(fnr = UserConstants.ARBEIDSTAKER_FNR).first()
            assertNotEquals(newPersonOversiktStatus.isAktivSenOppfolgingKandidat, pPersonOversiktStatus.isAktivSenOppfolgingKandidat)
            assertFalse(pPersonOversiktStatus.isAktivSenOppfolgingKandidat)
        }

        @Test
        fun `Creates new person when none exist when upserting mer oppfolging kandidat status`() {
            val result = personOversiktStatusRepository.upsertSenOppfolgingKandidat(
                personident = arbeidstakerFnr,
                isAktivKandidat = true,
            )

            val pPersonOversiktStatus = database.getPersonOversiktStatusList(fnr = UserConstants.ARBEIDSTAKER_FNR)
            assertTrue(pPersonOversiktStatus.isNotEmpty())
            assertTrue(result.isSuccess)
        }
    }

    @Nested
    @DisplayName("Upsert kartleggingsspormal vurdering")
    inner class UpsertKartleggingssporsmalVurdering {
        @Test
        fun `Successfully updates kartleggingssporsmal vurdering status to active`() {
            val newPersonOversiktStatus = PersonOversiktStatus(fnr = UserConstants.ARBEIDSTAKER_FNR)
            personOversiktStatusRepository.createPersonOversiktStatus(newPersonOversiktStatus)

            val result = personOversiktStatusRepository.upsertKartleggingssporsmalKandidatStatus(
                personident = arbeidstakerFnr,
                isAktivKandidat = true,
            )

            assertTrue(result.isSuccess)
            val pPersonOversiktStatus = personOversiktStatusRepository.getPersonOversiktStatus(personident = arbeidstakerFnr)!!
            assertNotEquals(
                newPersonOversiktStatus.isAktivKartleggingssporsmalVurdering,
                pPersonOversiktStatus.isAktivKartleggingssporsmalVurdering
            )
            assertTrue(pPersonOversiktStatus.isAktivKartleggingssporsmalVurdering)
        }

        @Test
        fun `Successfully updates kartleggingssporsmal vurdering status from active to not active`() {
            val newPersonOversiktStatus = PersonOversiktStatus(fnr = UserConstants.ARBEIDSTAKER_FNR)
                .copy(isAktivKartleggingssporsmalVurdering = true)
            personOversiktStatusRepository.createPersonOversiktStatus(newPersonOversiktStatus)

            val result = personOversiktStatusRepository.upsertKartleggingssporsmalKandidatStatus(
                personident = arbeidstakerFnr,
                isAktivKandidat = false,
            )

            assertTrue(result.isSuccess)
            val pPersonOversiktStatus = personOversiktStatusRepository.getPersonOversiktStatus(personident = arbeidstakerFnr)!!
            assertNotEquals(
                newPersonOversiktStatus.isAktivKartleggingssporsmalVurdering,
                pPersonOversiktStatus.isAktivKartleggingssporsmalVurdering
            )
            assertFalse(pPersonOversiktStatus.isAktivKartleggingssporsmalVurdering)
        }

        @Test
        fun `Creates new person when none exist when upserting kartleggingssporsmal vurdering status`() {
            val result = personOversiktStatusRepository.upsertKartleggingssporsmalKandidatStatus(
                personident = arbeidstakerFnr,
                isAktivKandidat = true,
            )

            val pPersonOversiktStatus = personOversiktStatusRepository.getPersonOversiktStatus(personident = arbeidstakerFnr)
            assertNotNull(pPersonOversiktStatus)
            assertTrue(result.isSuccess)
        }
    }

    @Nested
    @DisplayName("Get person oversikt status")
    inner class GetPersonOversiktStatus {

        @Test
        fun `Retrieves person oversikt status when one exists`() {
            val fodselsdato = LocalDate.of(1985, Month.MAY, 17)
            val newPersonOversiktStatus = PersonOversiktStatus(fnr = UserConstants.ARBEIDSTAKER_FNR, fodselsdato = fodselsdato)
            database.connection.use { connection ->
                connection.createPersonOversiktStatus(
                    commit = true,
                    personOversiktStatus = newPersonOversiktStatus,
                )
            }

            val personStatus =
                personOversiktStatusRepository.getPersonOversiktStatus(arbeidstakerFnr)
            assertNotNull(personStatus)
            assertEquals(newPersonOversiktStatus.fnr, personStatus?.fnr)
            assertEquals(fodselsdato, personStatus?.fodselsdato)
        }

        @Test
        fun `Handles trying to retrieve non existing person oversikt status`() {
            val personStatus =
                personOversiktStatusRepository.getPersonOversiktStatus(arbeidstakerFnr)

            assertNull(personStatus)
        }
    }

    @Nested
    @DisplayName("Update aktivitetskrav aktiv vurdering")
    inner class UpdateAktivitetskravAktivVurdering {

        @Test
        fun `Successfully updates aktiv aktivitetskrav status to active`() {
            val newPersonOversiktStatus = PersonOversiktStatus(fnr = UserConstants.ARBEIDSTAKER_FNR)
            database.connection.use { connection ->
                connection.createPersonOversiktStatus(
                    commit = true,
                    personOversiktStatus = newPersonOversiktStatus,
                )
            }

            val result = personOversiktStatusRepository.upsertAktivitetskravAktivStatus(
                personident = arbeidstakerFnr,
                isAktivVurdering = true,
            )

            assertTrue(result.isSuccess)
            val pPersonOversiktStatus = database.getPersonOversiktStatusList(fnr = UserConstants.ARBEIDSTAKER_FNR).first()
            assertNotEquals(newPersonOversiktStatus.isAktivAktivitetskravvurdering, pPersonOversiktStatus.isAktivAktivitetskravvurdering)
            assertTrue(pPersonOversiktStatus.isAktivAktivitetskravvurdering)
        }

        @Test
        fun `Successfully updates aktivitetskrav aktiv status from active to not active`() {
            val newPersonOversiktStatus = PersonOversiktStatus(fnr = UserConstants.ARBEIDSTAKER_FNR)
                .copy(isAktivAktivitetskravvurdering = true)
            database.connection.use { connection ->
                connection.createPersonOversiktStatus(
                    commit = true,
                    personOversiktStatus = newPersonOversiktStatus,
                )
            }

            val result = personOversiktStatusRepository.upsertAktivitetskravAktivStatus(
                personident = arbeidstakerFnr,
                isAktivVurdering = false,
            )

            assertTrue(result.isSuccess)
            val pPersonOversiktStatus = personOversiktStatusRepository.getPersonOversiktStatus(personident = arbeidstakerFnr)!!
            assertNotEquals(newPersonOversiktStatus.isAktivAktivitetskravvurdering, pPersonOversiktStatus.isAktivAktivitetskravvurdering)
            assertFalse(pPersonOversiktStatus.isAktivAktivitetskravvurdering)
        }

        @Test
        fun `Creates new person when none exist when upserting aktivitetskrav status`() {
            val result = personOversiktStatusRepository.upsertAktivitetskravAktivStatus(
                personident = arbeidstakerFnr,
                isAktivVurdering = true,
            )

            val pPersonOversiktStatus =
                personOversiktStatusRepository.getPersonOversiktStatus(personident = arbeidstakerFnr)
            assertNotNull(pPersonOversiktStatus)
            assertTrue(result.isSuccess)
        }
    }

    @Nested
    @DisplayName("Person search")
    inner class PersonSearch {
        private val fodselsdato = LocalDate.of(1985, Month.MAY, 17)

        private fun searchPerson(initials: String? = null, name: String? = null, birthdate: LocalDate? = null): List<PersonOversiktStatus> {
            SearchQueryDTO(
                initials = initials,
                name = name,
                birthdate = birthdate,
            ).toSearchQuery().let {
                return personOversiktStatusRepository.searchPerson(it)
            }
        }

        @Test
        fun `Finds relevant sykmeldt person when searching with correct initials and birthdate`() {
            val newPersonOversiktStatus = PersonOversiktStatus(
                fnr = UserConstants.ARBEIDSTAKER_FNR,
                fodselsdato = fodselsdato,
                navn = "Fornavn Mellomnavn Etternavn",
                latestOppfolgingstilfelle = activeOppfolgingstilfelle,
            )
            personOversiktStatusRepository.createPersonOversiktStatus(newPersonOversiktStatus)

            searchPerson(initials = "FME", birthdate = fodselsdato).let {
                assertEquals(1, it.size)
                assertEquals("Fornavn Mellomnavn Etternavn", it.first().navn)
            }
            searchPerson(initials = "FE", birthdate = fodselsdato).let {
                assertEquals(1, it.size)
                assertEquals("Fornavn Mellomnavn Etternavn", it.first().navn)
            }
            searchPerson(initials = "FM", birthdate = fodselsdato).let {
                assertEquals(1, it.size)
                assertEquals("Fornavn Mellomnavn Etternavn", it.first().navn)
            }
        }

        @Test
        fun `Finds person with oppgave but not sykmeldt when searching with correct initials and birthdate`() {
            val newPersonOversiktStatus = PersonOversiktStatus(
                fnr = UserConstants.ARBEIDSTAKER_FNR,
                fodselsdato = fodselsdato,
                navn = "Fornavn Mellomnavn Etternavn",
                isAktivOppfolgingsoppgave = true,
                latestOppfolgingstilfelle = inactiveOppfolgingstilfelle,
            )
            personOversiktStatusRepository.createPersonOversiktStatus(newPersonOversiktStatus)

            searchPerson(initials = "FME", birthdate = fodselsdato).let {
                assertEquals(1, it.size)
                assertEquals("Fornavn Mellomnavn Etternavn", it.first().navn)
            }
            searchPerson(initials = "FE", birthdate = fodselsdato).let {
                assertEquals(1, it.size)
                assertEquals("Fornavn Mellomnavn Etternavn", it.first().navn)
            }
            searchPerson(initials = "FM", birthdate = fodselsdato).let {
                assertEquals(1, it.size)
                assertEquals("Fornavn Mellomnavn Etternavn", it.first().navn)
            }
        }

        @Test
        fun `Finds relevant sykmeldt person when searching with correct lowercase initials and birthdate`() {
            val newPersonOversiktStatus = PersonOversiktStatus(
                fnr = UserConstants.ARBEIDSTAKER_FNR,
                fodselsdato = fodselsdato,
                navn = "Fornavn Mellomnavn Etternavn",
                latestOppfolgingstilfelle = activeOppfolgingstilfelle,
            )
            personOversiktStatusRepository.createPersonOversiktStatus(newPersonOversiktStatus)

            searchPerson(initials = "fme", birthdate = fodselsdato).let {
                assertEquals(1, it.size)
                assertEquals("Fornavn Mellomnavn Etternavn", it.first().navn)
            }
            searchPerson(initials = "fe", birthdate = fodselsdato).let {
                assertEquals(1, it.size)
                assertEquals("Fornavn Mellomnavn Etternavn", it.first().navn)
            }
            searchPerson(initials = "fm", birthdate = fodselsdato).let {
                assertEquals(1, it.size)
                assertEquals("Fornavn Mellomnavn Etternavn", it.first().navn)
            }
        }

        @Test
        fun `Returns empty list when not matching birthdate or initials`() {
            val newPersonOversiktStatus = PersonOversiktStatus(
                fnr = UserConstants.ARBEIDSTAKER_FNR,
                fodselsdato = fodselsdato,
                navn = "Fornavn Mellomnavn Etternavn",
                latestOppfolgingstilfelle = activeOppfolgingstilfelle,
            )
            personOversiktStatusRepository.createPersonOversiktStatus(newPersonOversiktStatus)

            assertEquals(0, searchPerson(initials = "AB", birthdate = LocalDate.now()).size)
        }

        @Test
        fun `Returns empty list when matching birthdate but not initials`() {
            val newPersonOversiktStatus = PersonOversiktStatus(
                fnr = UserConstants.ARBEIDSTAKER_FNR,
                fodselsdato = fodselsdato,
                navn = "Fornavn Mellomnavn Etternavn",
                latestOppfolgingstilfelle = activeOppfolgingstilfelle,
            )
            personOversiktStatusRepository.createPersonOversiktStatus(newPersonOversiktStatus)

            assertEquals(0, searchPerson(initials = "AB", birthdate = fodselsdato).size)
        }

        @Test
        fun `Returns empty list when matching initials but not birthdate`() {
            val newPersonOversiktStatus = PersonOversiktStatus(
                fnr = UserConstants.ARBEIDSTAKER_FNR,
                fodselsdato = fodselsdato,
                navn = "Fornavn Mellomnavn Etternavn",
                latestOppfolgingstilfelle = activeOppfolgingstilfelle,
            )
            personOversiktStatusRepository.createPersonOversiktStatus(newPersonOversiktStatus)

            assertEquals(0, searchPerson(initials = "FME", birthdate = LocalDate.now()).size)
        }

        @Test
        fun `Returns empty list when matching initials but person missing birthdate`() {
            val newPersonOversiktStatus = PersonOversiktStatus(
                fnr = UserConstants.ARBEIDSTAKER_FNR,
                navn = "Fornavn Mellomnavn Etternavn",
                latestOppfolgingstilfelle = activeOppfolgingstilfelle,
            )
            personOversiktStatusRepository.createPersonOversiktStatus(newPersonOversiktStatus)

            assertEquals(0, searchPerson(initials = "FME", birthdate = fodselsdato).size)
        }

        @Test
        fun `Returns empty list when matching initials and birthdate but no active oppfolgingstilfelle or oppgave`() {
            val newPersonOversiktStatus = PersonOversiktStatus(
                fnr = UserConstants.ARBEIDSTAKER_FNR,
                fodselsdato = fodselsdato,
                navn = "Fornavn Mellomnavn Etternavn",
                latestOppfolgingstilfelle = inactiveOppfolgingstilfelle,
            )
            personOversiktStatusRepository.createPersonOversiktStatus(newPersonOversiktStatus)

            assertEquals(0, searchPerson(initials = "FME", birthdate = fodselsdato).size)
        }

        @Test
        fun `Returns several persons when relevant`() {
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

            searchPerson(initials = "FME", birthdate = fodselsdato).let {
                assertEquals(2, it.size)
                assertEquals("Fornavn Mellomnavn Etternavn", it.first().navn)
                assertEquals("Frank Mellomnavnsen Etternavnsen", it[1].navn)
            }
        }

        @Test
        fun `Returns several persons when relevant with different name patterns`() {
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

            searchPerson(initials = "FE", birthdate = fodselsdato).let {
                assertEquals(2, it.size)
                assertEquals("Fornavn Evensen Melonsen", it.first().navn)
                assertEquals("Frank Melonsen Evensen", it[1].navn)
            }
            searchPerson(initials = "FM", birthdate = fodselsdato).let {
                assertEquals(2, it.size)
                assertEquals("Fornavn Evensen Melonsen", it.first().navn)
                assertEquals("Frank Melonsen Evensen", it[1].navn)
            }
        }

        @Test
        fun `Returns person with matching firstname and surname`() {
            personOversiktStatusRepository.createPersonOversiktStatus(
                PersonOversiktStatus(
                    fnr = UserConstants.ARBEIDSTAKER_FNR,
                    fodselsdato = fodselsdato,
                    navn = "Fornavn Etternavnsen",
                    latestOppfolgingstilfelle = activeOppfolgingstilfelle,
                )
            )
            searchPerson(name = "Fornavn Etternavnsen").let {
                assertEquals(1, it.size)
                assertEquals("Fornavn Etternavnsen", it.first().navn)
            }
        }

        @Test
        fun `Returns person with matching firstname and several surnames`() {
            personOversiktStatusRepository.createPersonOversiktStatus(
                PersonOversiktStatus(
                    fnr = UserConstants.ARBEIDSTAKER_FNR,
                    fodselsdato = fodselsdato,
                    navn = "Fornavn Mellomnavnsen Etternavnsen",
                    latestOppfolgingstilfelle = activeOppfolgingstilfelle,
                )
            )
            searchPerson(name = "Fornavn Mellomnavnsen Etternavnsen").let {
                assertEquals(1, it.size)
                assertEquals("Fornavn Mellomnavnsen Etternavnsen", it.first().navn)
            }
        }

        @Test
        fun `Returns person with matching firstname and part of surnames`() {
            personOversiktStatusRepository.createPersonOversiktStatus(
                PersonOversiktStatus(
                    fnr = UserConstants.ARBEIDSTAKER_FNR,
                    fodselsdato = fodselsdato,
                    navn = "Fornavn Mellomnavnsen Etternavnsen",
                    latestOppfolgingstilfelle = activeOppfolgingstilfelle,
                )
            )
            searchPerson(name = "Fornavn Mellomnavnsen").let {
                assertEquals(1, it.size)
                assertEquals("Fornavn Mellomnavnsen Etternavnsen", it.first().navn)
            }
            searchPerson(name = "Fornavn Etternavnsen").let {
                assertEquals(1, it.size)
                assertEquals("Fornavn Mellomnavnsen Etternavnsen", it.first().navn)
            }
        }

        @Test
        fun `Returns person with matching firstname and several incomplete surnames`() {
            personOversiktStatusRepository.createPersonOversiktStatus(
                PersonOversiktStatus(
                    fnr = UserConstants.ARBEIDSTAKER_FNR,
                    fodselsdato = fodselsdato,
                    navn = "Fornavn Mellomnavnsen Etternavnsen",
                    latestOppfolgingstilfelle = activeOppfolgingstilfelle,
                )
            )
            searchPerson(name = "Fornavn Mell Ette").let {
                assertEquals(1, it.size)
                assertEquals("Fornavn Mellomnavnsen Etternavnsen", it.first().navn)
            }
        }

        @Test
        fun `Returns person with matching incomplete firstname`() {
            personOversiktStatusRepository.createPersonOversiktStatus(
                PersonOversiktStatus(
                    fnr = UserConstants.ARBEIDSTAKER_FNR,
                    fodselsdato = fodselsdato,
                    navn = "Fornavn Mellomnavnsen Etternavnsen",
                    latestOppfolgingstilfelle = activeOppfolgingstilfelle,
                )
            )
            searchPerson(name = "For Etternavnsen").let {
                assertEquals(1, it.size)
                assertEquals("Fornavn Mellomnavnsen Etternavnsen", it.first().navn)
            }
        }
    }

    @Nested
    @DisplayName("Update personstatuses with navn and fodselsdato")
    inner class UpdatePersonstatusesWithNavnAndFodselsdato {

        @Test
        fun `Updates navn`() {
            val createdPersonStatus = personOversiktStatusRepository.createPersonOversiktStatus(
                PersonOversiktStatus(fnr = UserConstants.ARBEIDSTAKER_FNR)
            )
            val updatedPersonStatus = createdPersonStatus.updatePersonDetails(navn = "Stian Sykmeldt")

            val updatesPersonStatuses =
                personOversiktStatusRepository.updatePersonstatusesWithNavnAndFodselsdato(listOf(updatedPersonStatus))

            updatesPersonStatuses.first().map { assertEquals("Stian Sykmeldt", it.navn) }
        }

        @Test
        fun `Updates fodselsdato`() {
            val createdPersonStatus = personOversiktStatusRepository.createPersonOversiktStatus(
                PersonOversiktStatus(fnr = UserConstants.ARBEIDSTAKER_FNR)
            )
            val updatedPersonStatus = createdPersonStatus.updatePersonDetails(fodselsdato = LocalDate.now())

            val updatesPersonStatuses =
                personOversiktStatusRepository.updatePersonstatusesWithNavnAndFodselsdato(listOf(updatedPersonStatus))

            updatesPersonStatuses.first().map { assertEquals(updatedPersonStatus.fodselsdato, it.fodselsdato) }
        }

        @Test
        fun `Updates navn and fodselsdato`() {
            val createdPersonStatus = personOversiktStatusRepository.createPersonOversiktStatus(
                PersonOversiktStatus(fnr = UserConstants.ARBEIDSTAKER_FNR)
            )
            val updatedPersonStatus = createdPersonStatus.apply {
                updatePersonDetails(navn = "Stian Sykmeldt")
                updatePersonDetails(fodselsdato = LocalDate.now())
            }

            val updatesPersonStatuses =
                personOversiktStatusRepository.updatePersonstatusesWithNavnAndFodselsdato(listOf(updatedPersonStatus))

            updatesPersonStatuses.first().map { assertEquals(updatedPersonStatus.navn, it.navn) }
            updatesPersonStatuses.first().map { assertEquals(updatedPersonStatus.fodselsdato, it.fodselsdato) }
        }
    }

    @Nested
    @DisplayName("Get personstatuses without navn or fodselsdato")
    inner class GetPersonstatusesWithoutNavnOrFodselsdato {

        @Test
        fun `Correctly finds persons with missing name or fodselsdato, and active oppgave, but not active oppfolgingstilfelle`() {
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

            assertEquals(1, personsWithMissingName.size)
            assertEquals(personMissingName.fnr, personsWithMissingName.first().fnr)
            assertFalse(personsWithMissingName.contains(personNotMissingAnything))
        }

        @Test
        fun `Correctly finds persons missing either name or fodselsdato`() {
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

            assertEquals(2, persons.size)
            val fnrList = persons.map { it.fnr }
            assertTrue(fnrList.contains(personWithNameMissingFodselsdato.fnr))
            assertTrue(fnrList.contains(personWithFodselsdatoMissingName.fnr))
        }

        @Test
        fun `Correctly finds persons with missing name, and not active oppgave, but active oppfolgingstilfelle`() {
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

            assertEquals(1, personsWithMissingName.size)
            assertEquals(personActiveOppfolgingstilfelle.fnr, personsWithMissingName.first().fnr)
            assertFalse(personsWithMissingName.contains(personNotActiveOppfolgingstilfelle))
        }

        @Test
        fun `Correctly returns empty list when no persons with active oppgave, or active oppfolgingstilfelle`() {
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

            assertEquals(0, personsWithMissingName.size)
        }
    }
}
