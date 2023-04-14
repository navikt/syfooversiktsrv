package no.nav.syfo.cronjob.reaper

import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import no.nav.syfo.personstatus.db.getPersonOversiktStatusList
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

object ReaperCronjobSpek : Spek({

    with(TestApplicationEngine()) {
        start()

        val externalMockEnvironment = ExternalMockEnvironment.instance
        val database = externalMockEnvironment.database

        val reaperService = ReaperService(
            database = database,
        )
        val reaperCronjob = ReaperCronjob(
            reaperService = reaperService,
        )

        describe(ReaperCronjobSpek::class.java.simpleName) {
            describe("Successful processing") {
                afterEachTest {
                    database.connection.dropData()
                }

                it("reset tildelt veileder for personer with tilfelle that ended three months ago") {
                    val twoMonthsAgo = LocalDate.now().minusMonths(3)
                    val personOversiktStatus = generatePersonOversiktStatusWithTilfelleEnd(twoMonthsAgo)
                    database.createPersonOversiktStatus(personOversiktStatus)

                    runBlocking {
                        val result = reaperCronjob.runJob()

                        result.failed shouldBeEqualTo 0
                        result.updated shouldBeEqualTo 1
                    }

                    val personOversiktStatuses = database.connection.getPersonOversiktStatusList(UserConstants.ARBEIDSTAKER_FNR)
                    val status = personOversiktStatuses[0]
                    status.veilederIdent shouldBeEqualTo null
                }

                it("don't process personer with tilfelle that ended less than three months ago") {
                    val lessThanTwoMonthsAgo = LocalDate.now().minusMonths(3).plusDays(1)
                    val personOversiktStatus = generatePersonOversiktStatusWithTilfelleEnd(lessThanTwoMonthsAgo)
                    database.createPersonOversiktStatus(personOversiktStatus)

                    runBlocking {
                        val result = reaperCronjob.runJob()

                        result.failed shouldBeEqualTo 0
                        result.updated shouldBeEqualTo 0
                    }
                }
            }
        }
    }
})

fun generatePersonOversiktStatusWithTilfelleEnd(tilfelleEnd: LocalDate): PersonOversiktStatus =
    generatePPersonOversiktStatus().copy(
        veilederIdent = "Z999999",
        oppfolgingstilfelleUpdatedAt = OffsetDateTime.now(),
        oppfolgingstilfelleGeneratedAt = OffsetDateTime.now(),
        oppfolgingstilfelleStart = LocalDate.now(),
        oppfolgingstilfelleEnd = tilfelleEnd,
        oppfolgingstilfelleBitReferanseInntruffet = OffsetDateTime.now(),
        oppfolgingstilfelleBitReferanseUuid = UUID.randomUUID(),
    ).toPersonOversiktStatus(emptyList())
