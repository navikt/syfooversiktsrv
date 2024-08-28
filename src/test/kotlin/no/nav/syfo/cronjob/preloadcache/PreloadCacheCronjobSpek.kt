package no.nav.syfo.cronjob.preloadcache

import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import no.nav.syfo.application.cache.RedisStore
import no.nav.syfo.personstatus.infrastructure.clients.veiledertilgang.VeilederTilgangskontrollClient
import no.nav.syfo.personstatus.infrastructure.clients.azuread.AzureAdClient
import no.nav.syfo.personstatus.infrastructure.cronjob.preloadcache.PreloadCacheCronjob
import no.nav.syfo.testutil.*
import no.nav.syfo.testutil.generator.generatePersonOversiktStatus
import org.amshove.kluent.shouldBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.*

object PreloadCacheCronjobSpek : Spek({

    with(TestApplicationEngine()) {
        start()

        val externalMockEnvironment = ExternalMockEnvironment.instance
        val database = externalMockEnvironment.database
        val redisStore = RedisStore(externalMockEnvironment.environment.redis)
        val azureAdClient = AzureAdClient(
            azureEnvironment = externalMockEnvironment.environment.azure,
            redisStore = redisStore,
        )

        val preloadCacheCronjob = PreloadCacheCronjob(
            database = database,
            tilgangskontrollClient = VeilederTilgangskontrollClient(
                azureAdClient = azureAdClient,
                istilgangskontrollEnv = externalMockEnvironment.environment.clients.istilgangskontroll,
            ),
        )

        describe(PreloadCacheCronjobSpek::class.java.simpleName) {
            describe("Successful processing") {
                afterEachTest {
                    database.dropData()
                }

                it("Initial run when restart before 6") {
                    val initalDelay = preloadCacheCronjob.calculateInitialDelay(
                        LocalDateTime.of(LocalDate.now(), LocalTime.of(4, 0))
                    )
                    initalDelay shouldBeEqualTo 2 * 60
                }
                it("Initial run when restart after 6") {
                    val initalDelay = preloadCacheCronjob.calculateInitialDelay(
                        LocalDateTime.of(LocalDate.now(), LocalTime.of(7, 0))
                    )
                    initalDelay shouldBeEqualTo 23 * 60
                }
                it("should cache persons in enhetens oversikt") {
                    database.createPersonOversiktStatus(generatePersonOversiktStatus())

                    runBlocking {
                        val result = preloadCacheCronjob.runJob()

                        result.failed shouldBeEqualTo 0
                        result.updated shouldBeEqualTo 1
                    }
                }
                it("should tolerate errors when caching persons in enhetens oversikt") {
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

                        result.failed shouldBeEqualTo 1
                        result.updated shouldBeEqualTo 1
                    }
                }
            }
        }
    }
})
