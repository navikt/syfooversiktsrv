package no.nav.syfo.personstatus.application

import kotlinx.coroutines.runBlocking
import no.nav.syfo.personstatus.domain.PersonIdent
import no.nav.syfo.personstatus.domain.PersonOversiktStatus
import no.nav.syfo.testutil.ExternalMockEnvironment
import no.nav.syfo.testutil.UserConstants
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PersonoversiktStatusServiceTest {
    private val externalMockEnvironment = ExternalMockEnvironment.instance
    private val database = externalMockEnvironment.database
    private val personOversiktStatusRepository = externalMockEnvironment.personOversiktStatusRepository
    private val personoversiktStatusService = externalMockEnvironment.personoversiktStatusService

    @BeforeEach
    fun setUp() {
        database.resetDatabase()
    }

    @AfterEach
    fun tearDownAll() {
        database.resetDatabase()
    }

    @Test
    fun `Correctly updates navn and or fodselsdato for persons`() {
        val personMissingNavnAndFodselsdato = personOversiktStatusRepository.createPersonOversiktStatus(
            PersonOversiktStatus(
                fnr = UserConstants.ARBEIDSTAKER_FNR,
                isAktivAktivitetskravvurdering = true,
            )
        )
        val personMissingNavnAndFodselsdatoNotRelevant =
            personOversiktStatusRepository.createPersonOversiktStatus(PersonOversiktStatus(fnr = UserConstants.ARBEIDSTAKER_2_FNR))
        val personMissingNavn = personOversiktStatusRepository.createPersonOversiktStatus(
            PersonOversiktStatus(
                fnr = UserConstants.ARBEIDSTAKER_3_FNR,
                fodselsdato = LocalDate.now(),
                isAktivAktivitetskravvurdering = true,
            )
        )
        val personMissingFodselsdato = personOversiktStatusRepository.createPersonOversiktStatus(
            PersonOversiktStatus(
                fnr = UserConstants.ARBEIDSTAKER_4_FNR,
                navn = "Sylvia Sykmeldt",
                isAktivAktivitetskravvurdering = true,
            )
        )
        val personNotMissingButActive = personOversiktStatusRepository.createPersonOversiktStatus(
            PersonOversiktStatus(
                fnr = UserConstants.ARBEIDSTAKER_5_FNR,
                navn = "Sylvia Sykmeldt",
                fodselsdato = LocalDate.now(),
                isAktivAktivitetskravvurdering = true,
            )
        )

        runBlocking {
            personoversiktStatusService.updateNavnOrFodselsdatoWhereMissing(updateLimit = 10)
        }

        val personMissingNavnAndFodselsdatoEdited =
            personOversiktStatusRepository.getPersonOversiktStatus(PersonIdent(UserConstants.ARBEIDSTAKER_FNR))
        val personMissingNavnAndFodselsdatoNotRelevantEdited =
            personOversiktStatusRepository.getPersonOversiktStatus(PersonIdent(UserConstants.ARBEIDSTAKER_2_FNR))
        val personMissingNavnEdited =
            personOversiktStatusRepository.getPersonOversiktStatus(PersonIdent(UserConstants.ARBEIDSTAKER_3_FNR))
        val personMissingFodselsdatoEdited =
            personOversiktStatusRepository.getPersonOversiktStatus(PersonIdent(UserConstants.ARBEIDSTAKER_4_FNR))
        val personNotMissingButActiveEdited =
            personOversiktStatusRepository.getPersonOversiktStatus(PersonIdent(UserConstants.ARBEIDSTAKER_5_FNR))

        assertEquals(personMissingNavnAndFodselsdato.fnr, personMissingNavnAndFodselsdatoEdited?.fnr)
        assertNotNull(personMissingNavnAndFodselsdatoEdited?.navn)
        assertNotNull(personMissingNavnAndFodselsdatoEdited?.fodselsdato)

        assertEquals(
            personMissingNavnAndFodselsdatoNotRelevant.fnr,
            personMissingNavnAndFodselsdatoNotRelevantEdited?.fnr
        )
        assertEquals(
            personMissingNavnAndFodselsdatoNotRelevant.navn,
            personMissingNavnAndFodselsdatoNotRelevantEdited?.navn
        )
        assertEquals(
            personMissingNavnAndFodselsdatoNotRelevant.fodselsdato,
            personMissingNavnAndFodselsdatoNotRelevantEdited?.fodselsdato
        )

        assertEquals(personMissingNavn.fnr, personMissingNavnEdited?.fnr)
        assertNull(personMissingNavn.navn)
        assertNotNull(personMissingNavnEdited?.navn)

        assertEquals(personMissingFodselsdato.fnr, personMissingFodselsdatoEdited?.fnr)
        assertNull(personMissingFodselsdato.fodselsdato)
        assertNotNull(personMissingFodselsdatoEdited?.fodselsdato)

        assertEquals(personNotMissingButActive.fnr, personNotMissingButActiveEdited?.fnr)
        assertEquals(personNotMissingButActive.navn, personNotMissingButActiveEdited?.navn)
        assertEquals(personNotMissingButActive.fodselsdato, personNotMissingButActiveEdited?.fodselsdato)
    }

    @Test
    fun `Correctly updates navn for person missing name and fodselsdato and missing fodselsdato in PDL`() {
        val personMissingNavnAndFodselsdato = personOversiktStatusRepository.createPersonOversiktStatus(
            PersonOversiktStatus(
                fnr = UserConstants.ARBEIDSTAKER_NO_FODSELSDATO,
                isAktivAktivitetskravvurdering = true,
            )
        )

        runBlocking {
            personoversiktStatusService.updateNavnOrFodselsdatoWhereMissing(updateLimit = 10)
        }

        val personMissingNavnAndFodselsdatoEdited =
            personOversiktStatusRepository.getPersonOversiktStatus(PersonIdent(UserConstants.ARBEIDSTAKER_NO_FODSELSDATO))

        assertEquals(personMissingNavnAndFodselsdato.fnr, personMissingNavnAndFodselsdatoEdited?.fnr)
        assertNotNull(personMissingNavnAndFodselsdatoEdited?.navn)
        assertNull(personMissingNavnAndFodselsdatoEdited?.fodselsdato)
    }
}
