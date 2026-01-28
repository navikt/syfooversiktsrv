package no.nav.syfo.cronjob.preloadcache

import kotlinx.coroutines.runBlocking
import no.nav.syfo.personstatus.infrastructure.clients.azuread.AzureAdClient
import no.nav.syfo.personstatus.infrastructure.clients.veiledertilgang.VeilederTilgangskontrollClient
import no.nav.syfo.personstatus.infrastructure.cronjob.preloadcache.PreloadCacheCronjob
import no.nav.syfo.testutil.ExternalMockEnvironment
import no.nav.syfo.testutil.UserConstants
import no.nav.syfo.testutil.createPersonOversiktStatus
import no.nav.syfo.testutil.generator.generatePersonOversiktStatus
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class PreloadCacheCronjobTest {
    private val externalMockEnvironment = ExternalMockEnvironment.instance
    private val database = externalMockEnvironment.database
    private val azureAdClient = AzureAdClient(
        azureEnvironment = externalMockEnvironment.environment.azure,
        valkeyStore = externalMockEnvironment.valkeyStore,
        httpClient = externalMockEnvironment.mockHttpClient
    )

    private val preloadCacheCronjob = PreloadCacheCronjob(
        database = database,
        tilgangskontrollClient = VeilederTilgangskontrollClient(
            azureAdClient = azureAdClient,
            istilgangskontrollEnv = externalMockEnvironment.environment.clients.istilgangskontroll,
            httpClient = externalMockEnvironment.mockHttpClient
        ),
        personoversiktStatusRepository = externalMockEnvironment.personOversiktStatusRepository,
    )

    @AfterEach
    fun tearDown() {
        database.resetDatabase()
    }

    @Test
    fun `Initial run when restart before 6`() {
        val initalDelay = preloadCacheCronjob.calculateInitialDelay(
            LocalDateTime.of(LocalDate.now(), LocalTime.of(4, 0))
        )
        assertEquals(2 * 60, initalDelay)
    }

    @Test
    fun `Initial run when restart after 6`() {
        val initalDelay = preloadCacheCronjob.calculateInitialDelay(
            LocalDateTime.of(LocalDate.now(), LocalTime.of(7, 0))
        )
        assertEquals(23 * 60, initalDelay)
    }

    @Test
    fun `Should cache persons in enhetens oversikt`() {
        database.createPersonOversiktStatus(generatePersonOversiktStatus())

        runBlocking {
            val result = preloadCacheCronjob.runJob()

            assertEquals(0, result.failed)
            assertEquals(1, result.updated)
        }
    }

    @Test
    fun `Should tolerate errors when caching persons in enhetens oversikt`() {
        database.createPersonOversiktStatus(
            generatePersonOversiktStatus(
                fnr = UserConstants.ARBEIDSTAKER_4_FNR_WITH_ERROR,
                enhet = UserConstants.NAV_ENHET,
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

            assertEquals(1, result.failed)
            assertEquals(1, result.updated)
        }
    }
}
