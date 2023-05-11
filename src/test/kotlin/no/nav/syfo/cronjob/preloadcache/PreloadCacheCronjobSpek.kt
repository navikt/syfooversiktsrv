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
import java.time.LocalDate
import java.time.OffsetDateTime
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

                it("should cache persons in enhetens oversikt") {
                    database.createPersonOversiktStatus(generatePersonOversiktStatus())

                    runBlocking {
                        val result = preloadCacheCronjob.runJob()

                        result.failed shouldBeEqualTo 0
                        result.updated shouldBeEqualTo 1
                    }
                }
            }
        }
    }
})

fun generatePersonOversiktStatus(): PersonOversiktStatus =
    generatePPersonOversiktStatus().copy(
        veilederIdent = "Z999999",
        enhet = UserConstants.NAV_ENHET,
        oppfolgingstilfelleUpdatedAt = OffsetDateTime.now(),
        oppfolgingstilfelleGeneratedAt = OffsetDateTime.now(),
        oppfolgingstilfelleStart = LocalDate.now().minusDays(14),
        oppfolgingstilfelleEnd = LocalDate.now(),
        oppfolgingstilfelleBitReferanseInntruffet = OffsetDateTime.now(),
        oppfolgingstilfelleBitReferanseUuid = UUID.randomUUID(),
        dialogmotekandidat = true,
        dialogmotekandidatGeneratedAt = OffsetDateTime.now().minusDays(8),
    ).toPersonOversiktStatus(emptyList())
