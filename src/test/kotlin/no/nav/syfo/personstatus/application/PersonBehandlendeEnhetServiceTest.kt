package no.nav.syfo.personstatus.application

import kotlinx.coroutines.runBlocking
import no.nav.syfo.personstatus.domain.PersonIdent
import no.nav.syfo.personstatus.domain.PersonOversiktStatus
import no.nav.syfo.testutil.ExternalMockEnvironment
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_ENHET_NOT_FOUND_PERSONIDENT
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testutil.mock.behandlendeEnhetDTO
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull

class PersonBehandlendeEnhetServiceTest {
    private val externalMockEnvironment = ExternalMockEnvironment.instance
    private val database = externalMockEnvironment.database
    private val personBehandlendeEnhetService = externalMockEnvironment.personBehandlendeEnhetService
    private val personOversiktStatusRepository = externalMockEnvironment.personOversiktStatusRepository

    @BeforeEach
    fun setUp() {
        database.resetDatabase()
    }

    @AfterEach
    fun tearDown() {
        database.resetDatabase()
    }

    @Test
    fun `Correctly updates enhet when no enhet assigned`() {
        personOversiktStatusRepository.createPersonOversiktStatus(
            PersonOversiktStatus(fnr = ARBEIDSTAKER_FNR, enhet = null)
        )

        runBlocking {
            personBehandlendeEnhetService.updateBehandlendeEnhet(personIdent = PersonIdent(ARBEIDSTAKER_FNR))
        }

        val personOversiktStatus = personOversiktStatusRepository.getPersonOversiktStatus(PersonIdent(ARBEIDSTAKER_FNR))
        assertNotNull(personOversiktStatus)
        assertEquals(behandlendeEnhetDTO.geografiskEnhet.enhetId, personOversiktStatus!!.enhet)
    }

    @Test
    fun `Correctly updates enhet when other enhet is already assigned`() {
        personOversiktStatusRepository.createPersonOversiktStatus(
            PersonOversiktStatus(fnr = ARBEIDSTAKER_FNR, enhet = "0314")
        )

        runBlocking {
            personBehandlendeEnhetService.updateBehandlendeEnhet(personIdent = PersonIdent(ARBEIDSTAKER_FNR))
        }

        val personOversiktStatus = personOversiktStatusRepository.getPersonOversiktStatus(PersonIdent(ARBEIDSTAKER_FNR))
        assertNotNull(personOversiktStatus)
        assertEquals(behandlendeEnhetDTO.geografiskEnhet.enhetId, personOversiktStatus!!.enhet)
    }

    @Test
    fun `Don't update enhet when call to syfobehandlendeenhet returns no content`() {
        personOversiktStatusRepository.createPersonOversiktStatus(
            PersonOversiktStatus(fnr = ARBEIDSTAKER_ENHET_NOT_FOUND_PERSONIDENT.value, enhet = "0314")
        )

        runBlocking {
            personBehandlendeEnhetService.updateBehandlendeEnhet(personIdent = PersonIdent(ARBEIDSTAKER_ENHET_NOT_FOUND_PERSONIDENT.value))
        }

        val personOversiktStatus = personOversiktStatusRepository.getPersonOversiktStatus(PersonIdent(ARBEIDSTAKER_ENHET_NOT_FOUND_PERSONIDENT.value))
        assertNotNull(personOversiktStatus)
        assertNotEquals(behandlendeEnhetDTO.geografiskEnhet.enhetId, personOversiktStatus!!.enhet)
        assertEquals("0314", personOversiktStatus.enhet)
    }
}
