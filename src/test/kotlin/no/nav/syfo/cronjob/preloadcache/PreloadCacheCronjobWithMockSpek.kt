package no.nav.syfo.cronjob.preloadcache

import io.ktor.server.testing.*
import io.mockk.*
import kotlinx.coroutines.runBlocking
import no.nav.syfo.personstatus.infrastructure.clients.veiledertilgang.VeilederTilgangskontrollClient
import no.nav.syfo.personstatus.infrastructure.cronjob.preloadcache.PreloadCacheCronjob
import no.nav.syfo.testutil.*
import no.nav.syfo.testutil.generator.generatePersonOversiktStatus
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContain
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object PreloadCacheCronjobWithMockSpek : Spek({

    with(TestApplicationEngine()) {
        start()

        val externalMockEnvironment = ExternalMockEnvironment.instance
        val database = externalMockEnvironment.database
        val veilederTilgangskontrollMockClient = mockk<VeilederTilgangskontrollClient>(relaxed = true)

        val preloadCacheCronjob = PreloadCacheCronjob(
            database = database,
            tilgangskontrollClient = veilederTilgangskontrollMockClient,
        )
        beforeEachTest {
            clearMocks(veilederTilgangskontrollMockClient)
            coEvery { veilederTilgangskontrollMockClient.preloadCache(any()) } returns true
        }
        describe(PreloadCacheCronjobSpek::class.java.simpleName) {
            describe("Successful processing") {
                afterEachTest {
                    database.dropData()
                }
                it("should cache persons in enhetens oversikt") {
                    database.createPersonOversiktStatus(generatePersonOversiktStatus())

                    runBlocking {
                        val result = preloadCacheCronjob.runJob()
                        val personIdListSlot = slot<List<String>>()
                        coVerify(exactly = 1) {
                            veilederTilgangskontrollMockClient.preloadCache(capture(personIdListSlot))
                        }
                        personIdListSlot.captured.size shouldBeEqualTo 1
                        personIdListSlot.captured[0] shouldBeEqualTo UserConstants.ARBEIDSTAKER_FNR
                        result.failed shouldBeEqualTo 0
                        result.updated shouldBeEqualTo 1
                    }
                }
                it("should cache multiple persons in same enhet") {
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
                        personIdListSlot.captured.size shouldBeEqualTo 2
                        personIdListSlot.captured shouldContain UserConstants.ARBEIDSTAKER_FNR
                        personIdListSlot.captured shouldContain UserConstants.ARBEIDSTAKER_2_FNR
                        result.failed shouldBeEqualTo 0
                        result.updated shouldBeEqualTo 2
                    }
                }
                it("should cache all enhets with persons") {
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
                        personIdListSlot.size shouldBeEqualTo 2
                        personIdListSlot[0].size shouldBeEqualTo 1
                        personIdListSlot[1].size shouldBeEqualTo 1
                        result.failed shouldBeEqualTo 0
                        result.updated shouldBeEqualTo 2
                    }
                }
                it("should only cache persons with active oppgave") {
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
                        personIdListSlot.captured.size shouldBeEqualTo 1
                        personIdListSlot.captured[0] shouldBeEqualTo UserConstants.ARBEIDSTAKER_FNR
                        result.failed shouldBeEqualTo 0
                        result.updated shouldBeEqualTo 1
                    }
                }
            }
        }
    }
})
