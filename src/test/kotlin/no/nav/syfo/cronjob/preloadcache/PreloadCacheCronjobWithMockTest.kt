package no.nav.syfo.cronjob.preloadcache

import io.mockk.*
import kotlinx.coroutines.runBlocking
import no.nav.syfo.personstatus.infrastructure.clients.veiledertilgang.VeilederTilgangskontrollClient
import no.nav.syfo.personstatus.infrastructure.cronjob.preloadcache.PreloadCacheCronjob
import no.nav.syfo.testutil.ExternalMockEnvironment
import no.nav.syfo.testutil.UserConstants
import no.nav.syfo.testutil.createPersonOversiktStatus
import no.nav.syfo.testutil.dropData
import no.nav.syfo.testutil.generator.generatePersonOversiktStatus
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PreloadCacheCronjobWithMockTest {
    private val externalMockEnvironment = ExternalMockEnvironment.instance
    private val database = externalMockEnvironment.database
    private val veilederTilgangskontrollMockClient = mockk<VeilederTilgangskontrollClient>(relaxed = true)

    private val preloadCacheCronjob = PreloadCacheCronjob(
        database = database,
        tilgangskontrollClient = veilederTilgangskontrollMockClient,
    )

    @BeforeEach
    fun setUp() {
        clearMocks(veilederTilgangskontrollMockClient)
        coEvery { veilederTilgangskontrollMockClient.preloadCache(any()) } returns true
    }

    @AfterEach
    fun tearDown() {
        database.dropData()
    }

    @Test
    fun `Should cache persons in enhetens oversikt`() {
        database.createPersonOversiktStatus(generatePersonOversiktStatus())

        runBlocking {
            val result = preloadCacheCronjob.runJob()
            val personIdListSlot = slot<List<String>>()
            coVerify(exactly = 1) {
                veilederTilgangskontrollMockClient.preloadCache(capture(personIdListSlot))
            }
            assertEquals(1, personIdListSlot.captured.size)
            assertEquals(UserConstants.ARBEIDSTAKER_FNR, personIdListSlot.captured[0])
            assertEquals(0, result.failed)
            assertEquals(1, result.updated)
        }
    }

    @Test
    fun `Should cache multiple persons in same enhet`() {
        database.createPersonOversiktStatus(generatePersonOversiktStatus())
        database.createPersonOversiktStatus(
            generatePersonOversiktStatus(
                fnr = UserConstants.ARBEIDSTAKER_2_FNR,
            )
        )

        runBlocking {
            val result = preloadCacheCronjob.runJob()
            val personIdListSlot = slot<List<String>>()
            coVerify(exactly = 1) {
                veilederTilgangskontrollMockClient.preloadCache(capture(personIdListSlot))
            }
            assertEquals(2, personIdListSlot.captured.size)
            assertTrue(personIdListSlot.captured.contains(UserConstants.ARBEIDSTAKER_FNR))
            assertTrue(personIdListSlot.captured.contains(UserConstants.ARBEIDSTAKER_2_FNR))
            assertEquals(0, result.failed)
            assertEquals(2, result.updated)
        }
    }

    @Test
    fun `Should cache all enhets with persons`() {
        database.createPersonOversiktStatus(generatePersonOversiktStatus())
        database.createPersonOversiktStatus(
            generatePersonOversiktStatus(
                fnr = UserConstants.ARBEIDSTAKER_2_FNR,
                enhet = UserConstants.NAV_ENHET_2,
            )
        )

        runBlocking {
            val result = preloadCacheCronjob.runJob()
            val personIdListSlot = mutableListOf<MutableList<String>>()
            coVerify(exactly = 2) {
                veilederTilgangskontrollMockClient.preloadCache(capture(personIdListSlot))
            }
            assertEquals(2, personIdListSlot.size)
            assertEquals(1, personIdListSlot[0].size)
            assertEquals(1, personIdListSlot[1].size)
            assertEquals(0, result.failed)
            assertEquals(2, result.updated)
        }
    }

    @Test
    fun `Should only cache persons with active oppgave`() {
        database.createPersonOversiktStatus(
            generatePersonOversiktStatus(
                fnr = UserConstants.ARBEIDSTAKER_2_FNR,
                enhet = UserConstants.NAV_ENHET,
                dialogmotekandidat = false,
            )
        )
        database.createPersonOversiktStatus(
            generatePersonOversiktStatus(
                fnr = UserConstants.ARBEIDSTAKER_FNR,
                enhet = UserConstants.NAV_ENHET_2,
            )
        )

        runBlocking {
            val result = preloadCacheCronjob.runJob()
            val personIdListSlot = slot<List<String>>()
            coVerify(exactly = 1) {
                veilederTilgangskontrollMockClient.preloadCache(capture(personIdListSlot))
            }
            assertEquals(1, personIdListSlot.captured.size)
            assertEquals(UserConstants.ARBEIDSTAKER_FNR, personIdListSlot.captured[0])
            assertEquals(0, result.failed)
            assertEquals(1, result.updated)
        }
    }
}
