package no.nav.syfo.cronjob.reaper

import io.mockk.clearMocks
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.syfo.personstatus.domain.PersonIdent
import no.nav.syfo.personstatus.domain.PersonOversiktStatus
import no.nav.syfo.personstatus.domain.toPersonOversiktStatus
import no.nav.syfo.personstatus.infrastructure.clients.behandlendeenhet.BehandlendeEnhetClient
import no.nav.syfo.personstatus.infrastructure.cronjob.reaper.ReaperCronjob
import no.nav.syfo.personstatus.infrastructure.database.queries.getPersonOversiktStatusList
import no.nav.syfo.testutil.*
import no.nav.syfo.testutil.generator.generatePPersonOversiktStatus
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.sql.Timestamp
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

object ReaperCronjobSpek : Spek({
    val externalMockEnvironment = ExternalMockEnvironment.instance
    val database = externalMockEnvironment.database
    val personoversiktStatusService = externalMockEnvironment.personoversiktStatusService
    val behandlendeEnhetClient = mockk<BehandlendeEnhetClient>(relaxed = true)
    val reaperCronjob = ReaperCronjob(
        personOversiktStatusService = personoversiktStatusService,
        behandlendeEnhetClient = behandlendeEnhetClient,
    )

    describe(ReaperCronjobSpek::class.java.simpleName) {
        describe("Successful processing") {
            afterEachTest {
                database.dropData()
                clearMocks(behandlendeEnhetClient)
            }

            it("reset tildelt veileder for personer with tilfelle that ended two months ago") {
                val threeMonthsAgo = LocalDate.now().minusMonths(2).minusDays(1)
                val personOversiktStatus = generatePersonOversiktStatusWithTilfelleEnd(threeMonthsAgo)
                database.createPersonOversiktStatus(personOversiktStatus)
                database.setSistEndret(
                    fnr = personOversiktStatus.fnr,
                    sistEndret = Timestamp.from(OffsetDateTime.now().minusMonths(2).minusDays(1).toInstant()),
                )

                runBlocking {
                    val result = reaperCronjob.runJob()

                    result.failed shouldBeEqualTo 0
                    result.updated shouldBeEqualTo 1
                }

                val personOversiktStatuses = database.connection.getPersonOversiktStatusList(personOversiktStatus.fnr)
                val status = personOversiktStatuses[0]
                status.veilederIdent shouldBeEqualTo null
                coVerify(exactly = 1) {
                    behandlendeEnhetClient.unsetOppfolgingsenhet(any(), PersonIdent(personOversiktStatus.fnr))
                }
            }
            it("does not reset tildelt veileder for personer with sist_endret less than two months ago") {
                val threeMonthsAgo = LocalDate.now().minusMonths(3)
                val personOversiktStatus = generatePersonOversiktStatusWithTilfelleEnd(threeMonthsAgo)
                database.createPersonOversiktStatus(personOversiktStatus)
                database.setSistEndret(
                    fnr = personOversiktStatus.fnr,
                    sistEndret = Timestamp.from(OffsetDateTime.now().minusMonths(2).plusDays(4).toInstant()),
                )

                runBlocking {
                    val result = reaperCronjob.runJob()

                    result.failed shouldBeEqualTo 0
                    result.updated shouldBeEqualTo 0
                }

                val personOversiktStatuses = database.connection.getPersonOversiktStatusList(personOversiktStatus.fnr)
                val status = personOversiktStatuses[0]
                status.veilederIdent shouldNotBeEqualTo null
                coVerify(exactly = 0) {
                    behandlendeEnhetClient.unsetOppfolgingsenhet(any(), any())
                }
            }

            it("don't process personer with tilfelle that ended less than two months ago") {
                val lessThanTwoMonthsAgo = LocalDate.now().minusMonths(2).plusDays(1)
                val personOversiktStatus = generatePersonOversiktStatusWithTilfelleEnd(lessThanTwoMonthsAgo)
                database.createPersonOversiktStatus(personOversiktStatus)

                runBlocking {
                    val result = reaperCronjob.runJob()

                    result.failed shouldBeEqualTo 0
                    result.updated shouldBeEqualTo 0
                    coVerify(exactly = 0) {
                        behandlendeEnhetClient.unsetOppfolgingsenhet(any(), any())
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
        oppfolgingstilfelleStart = tilfelleEnd.minusDays(14),
        oppfolgingstilfelleEnd = tilfelleEnd,
        oppfolgingstilfelleBitReferanseInntruffet = OffsetDateTime.now(),
        oppfolgingstilfelleBitReferanseUuid = UUID.randomUUID(),
    ).toPersonOversiktStatus(emptyList())
