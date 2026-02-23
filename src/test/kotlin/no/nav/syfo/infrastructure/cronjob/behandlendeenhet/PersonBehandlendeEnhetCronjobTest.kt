package no.nav.syfo.infrastructure.cronjob.behandlendeenhet

import kotlinx.coroutines.runBlocking
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.domain.PersonOversiktStatus
import no.nav.syfo.testutil.*
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_ENHET_ERROR_PERSONIDENT
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_WITH_OPPFOLGINGSENHET
import no.nav.syfo.testutil.UserConstants.NAV_ENHET
import no.nav.syfo.testutil.UserConstants.NAV_ENHET_2
import no.nav.syfo.testutil.mock.behandlendeEnhetDTO
import no.nav.syfo.testutil.mock.behandlendeEnhetDTOWithOppfolgingsenhet
import no.nav.syfo.util.nowUTC
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PersonBehandlendeEnhetCronjobTest {

    private val externalMockEnvironment = ExternalMockEnvironment.instance
    private val database = externalMockEnvironment.database
    private val personOversiktStatusRepository = externalMockEnvironment.personOversiktStatusRepository
    private val internalMockEnvironment = InternalMockEnvironment.instance

    private val personBehandlendeEnhetCronjob = internalMockEnvironment.personBehandlendeEnhetCronjob

    private val personIdentDefault = PersonIdent(ARBEIDSTAKER_FNR)

    @BeforeEach
    fun setUp() {
        database.resetDatabase()
    }

    @Test
    fun `Should not update Enhet of existing PersonOversiktStatus with no ubehandlet oppgave`() {
        personOversiktStatusRepository.createPersonOversiktStatus(
            PersonOversiktStatus(
                fnr = ARBEIDSTAKER_FNR
            )
        )

        val personstatus = database.connection.use {
            with(personOversiktStatusRepository) {
                it.getPersonStatus(personident = PersonIdent(ARBEIDSTAKER_FNR))
            }
        }

        assertNotNull(personstatus)
        assertNull(personstatus?.enhet)
        assertNull(personstatus?.tildeltEnhetUpdatedAt)

        val result = runBlocking { personBehandlendeEnhetCronjob.runJob() }
        assertEquals(0, result.failed)
        assertEquals(0, result.updated)
    }

    @Test
    fun `Should update Enhet and remove Veileder of existing PersonOversiktStatus with other Enhet and ubehandlet oppgave`() {
        val firstEnhet = NAV_ENHET_2
        personOversiktStatusRepository.createPersonOversiktStatus(
            PersonOversiktStatus(
                fnr = ARBEIDSTAKER_FNR,
                motebehovUbehandlet = true,
                enhet = firstEnhet,
                veilederIdent = UserConstants.VEILEDER_ID,
            )
        )
        val tildeltEnhetUpdatedAtBeforeUpdate = nowUTC().minusDays(2)
        database.updateTildeltEnhetUpdatedAt(
            ident = personIdentDefault,
            time = tildeltEnhetUpdatedAtBeforeUpdate,
        )

        runBlocking {
            val result = personBehandlendeEnhetCronjob.runJob()

            assertEquals(0, result.failed)
            assertEquals(1, result.updated)
        }

        val pPersonOversiktStatus = database.connection.use { connection ->
            with(personOversiktStatusRepository) {
                connection.getPersonStatus(personident = personIdentDefault)!!
            }
        }

        assertNotNull(pPersonOversiktStatus)

        assertNotEquals(firstEnhet, pPersonOversiktStatus.enhet)
        assertEquals(behandlendeEnhetDTO.geografiskEnhet.enhetId, pPersonOversiktStatus.enhet)
        assertNotNull(pPersonOversiktStatus.tildeltEnhetUpdatedAt)
        assertTrue(
            pPersonOversiktStatus.tildeltEnhetUpdatedAt!!.toInstant()
                .toEpochMilli() > tildeltEnhetUpdatedAtBeforeUpdate.toInstant()
                .toEpochMilli()
        )
        assertNull(pPersonOversiktStatus.veilederIdent)

        val result = runBlocking { personBehandlendeEnhetCronjob.runJob() }
        assertEquals(0, result.failed)
        assertEquals(0, result.updated)
    }

    @Test
    fun `Should update Enhet and remove Veileder of existing PersonOversiktStatus with other Enhet and ubehandlet oppgave for person with oppfolgingsenhet`() {
        personOversiktStatusRepository.createPersonOversiktStatus(
            PersonOversiktStatus(
                fnr = ARBEIDSTAKER_WITH_OPPFOLGINGSENHET.value,
                motebehovUbehandlet = true,
                enhet = NAV_ENHET,
                veilederIdent = UserConstants.VEILEDER_ID,
            )
        )
        val tildeltEnhetUpdatedAtBeforeUpdate = nowUTC().minusDays(2)
        database.updateTildeltEnhetUpdatedAt(
            ident = ARBEIDSTAKER_WITH_OPPFOLGINGSENHET,
            time = tildeltEnhetUpdatedAtBeforeUpdate,
        )

        runBlocking {
            val result = personBehandlendeEnhetCronjob.runJob()

            assertEquals(0, result.failed)
            assertEquals(1, result.updated)
        }

        database.connection.use { connection ->
            val pPersonOversiktStatus = with(personOversiktStatusRepository) {
                connection.getPersonStatus(personident = ARBEIDSTAKER_WITH_OPPFOLGINGSENHET)!!
            }

            assertNotNull(pPersonOversiktStatus)
            assertEquals(behandlendeEnhetDTOWithOppfolgingsenhet.oppfolgingsenhetDTO!!.enhet.enhetId, pPersonOversiktStatus.enhet)
            assertNotNull(pPersonOversiktStatus.tildeltEnhetUpdatedAt)
            assertNull(pPersonOversiktStatus.veilederIdent)
        }
    }

    @Test
    fun `Should update Enhet if dialogmotekandidat is true`() {
        personOversiktStatusRepository.createPersonOversiktStatus(
            PersonOversiktStatus(
                fnr = ARBEIDSTAKER_FNR,
                dialogmotekandidat = true,
            )
        )

        runBlocking {
            val result = personBehandlendeEnhetCronjob.runJob()

            assertEquals(0, result.failed)
            assertEquals(1, result.updated)
        }

        val pPersonOversiktStatus = database.connection.use {
            with(personOversiktStatusRepository) {
                it.getPersonStatus(personident = personIdentDefault)
            }
        }

        assertNotNull(pPersonOversiktStatus)
        assertNotNull(pPersonOversiktStatus!!.enhet)

        runBlocking {
            val result = personBehandlendeEnhetCronjob.runJob()

            assertEquals(0, result.failed)
            assertEquals(0, result.updated)
        }
    }

    @Test
    fun `Should update Enhet and remove Veileder of existing PersonOversiktStatus with no Enhet and ubehandlet oppgave`() {
        personOversiktStatusRepository.createPersonOversiktStatus(
            PersonOversiktStatus(
                fnr = ARBEIDSTAKER_FNR,
                oppfolgingsplanLPSBistandUbehandlet = true,
                veilederIdent = UserConstants.VEILEDER_ID,
            )
        )

        val tildeltEnhetUpdatedAtBeforeUpdate = nowUTC().minusDays(2)

        database.updateTildeltEnhetUpdatedAt(
            ident = personIdentDefault,
            time = tildeltEnhetUpdatedAtBeforeUpdate,
        )
        runBlocking {
            val result = personBehandlendeEnhetCronjob.runJob()

            assertEquals(0, result.failed)
            assertEquals(1, result.updated)
        }

        val pPersonOversiktStatus = database.connection.use { connection ->
            with(personOversiktStatusRepository) {
                connection.getPersonStatus(personident = personIdentDefault)!!
            }
        }

        assertNotNull(pPersonOversiktStatus)

        assertEquals(behandlendeEnhetDTO.geografiskEnhet.enhetId, pPersonOversiktStatus.enhet)
        assertNotNull(pPersonOversiktStatus.tildeltEnhetUpdatedAt)
        assertNotEquals(tildeltEnhetUpdatedAtBeforeUpdate, pPersonOversiktStatus.tildeltEnhetUpdatedAt)
        assertNull(pPersonOversiktStatus.veilederIdent)

        val result = runBlocking { personBehandlendeEnhetCronjob.runJob() }
        assertEquals(0, result.failed)
        assertEquals(0, result.updated)
    }

    @Test
    fun `Don't update if ubehandlet oppgave but enhet updated less than 24 hours ago`() {
        val firstEnhet = NAV_ENHET_2
        personOversiktStatusRepository.createPersonOversiktStatus(
            PersonOversiktStatus(
                fnr = ARBEIDSTAKER_FNR,
                motebehovUbehandlet = true,
                enhet = firstEnhet,
            )
        )
        database.updateTildeltEnhetUpdatedAt(
            ident = personIdentDefault,
            time = nowUTC().minusHours(22),
        )

        runBlocking {
            val result = personBehandlendeEnhetCronjob.runJob()

            assertEquals(0, result.failed)
            assertEquals(0, result.updated)
        }
    }

    @Test
    fun `Should fail to update Enhet of existing PersonOversiktStatus exception is thrown when requesting Enhet from Syfobehandlendeenhet`() {
        personOversiktStatusRepository.createPersonOversiktStatus(
            PersonOversiktStatus(
                fnr = ARBEIDSTAKER_ENHET_ERROR_PERSONIDENT.value,
                oppfolgingsplanLPSBistandUbehandlet = true,
            )
        )

        runBlocking {
            val result = personBehandlendeEnhetCronjob.runJob()

            assertEquals(1, result.failed)
            assertEquals(0, result.updated)
        }
    }
}
