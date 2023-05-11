package no.nav.syfo.cronjob.preloadcache

import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import no.nav.syfo.application.cache.RedisStore
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.client.veiledertilgang.VeilederTilgangskontrollClient
import no.nav.syfo.personstatus.domain.PersonOversiktStatus
import no.nav.syfo.personstatus.domain.toPersonOversiktStatus
import no.nav.syfo.testutil.*
import no.nav.syfo.testutil.generator.generatePPersonOversiktStatus
import org.amshove.kluent.shouldBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.*
import java.util.*

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
                clientEnvironment = externalMockEnvironment.environment.clients.syfotilgangskontroll,
            )
        )

        describe(PreloadCacheCronjobSpek::class.java.simpleName) {
            describe("Successful processing") {
                afterEachTest {
                    database.connection.dropData()
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

fun generatePersonOversiktStatus(
    fnr: String = UserConstants.ARBEIDSTAKER_FNR,
    enhet: String = UserConstants.NAV_ENHET,
): PersonOversiktStatus =
    generatePPersonOversiktStatus(fnr).copy(
        veilederIdent = "Z999999",
        enhet = enhet,
        oppfolgingstilfelleUpdatedAt = OffsetDateTime.now(),
        oppfolgingstilfelleGeneratedAt = OffsetDateTime.now(),
        oppfolgingstilfelleStart = LocalDate.now().minusDays(14),
        oppfolgingstilfelleEnd = LocalDate.now(),
        oppfolgingstilfelleBitReferanseInntruffet = OffsetDateTime.now(),
        oppfolgingstilfelleBitReferanseUuid = UUID.randomUUID(),
        dialogmotekandidat = true,
        dialogmotekandidatGeneratedAt = OffsetDateTime.now().minusDays(8),
    ).toPersonOversiktStatus(emptyList())
