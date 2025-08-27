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
import no.nav.syfo.personstatus.infrastructure.database.repository.PersonOversiktStatusRepository
import no.nav.syfo.testutil.*
import no.nav.syfo.testutil.generator.generatePPersonOversiktStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.sql.Timestamp
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

class ReaperCronjobTest {

    private val externalMockEnvironment = ExternalMockEnvironment.instance
    private val database = externalMockEnvironment.database
    private val personoversiktRepository = PersonOversiktStatusRepository(database)
    private val personoversiktStatusService = externalMockEnvironment.personoversiktStatusService
    private val behandlendeEnhetClient = mockk<BehandlendeEnhetClient>(relaxed = true)
    private val reaperCronjob = ReaperCronjob(
        personOversiktStatusService = personoversiktStatusService,
        behandlendeEnhetClient = behandlendeEnhetClient,
    )

    @AfterEach
    fun tearDown() {
        database.resetDatabase()
        clearMocks(behandlendeEnhetClient)
    }

    @Test
    fun `Reset tildelt veileder and enhet for personer with tilfelle that ended two months ago`() {
        val threeMonthsAgo = LocalDate.now().minusMonths(2).minusDays(1)
        val personOversiktStatus = generatePersonOversiktStatusWithTilfelleEnd(threeMonthsAgo)
        database.createPersonOversiktStatus(personOversiktStatus)
        database.setSistEndret(
            fnr = personOversiktStatus.fnr,
            sistEndret = Timestamp.from(OffsetDateTime.now().minusMonths(2).minusDays(1).toInstant()),
        )

        runBlocking {
            val result = reaperCronjob.runJob()

            assertEquals(0, result.failed)
            assertEquals(1, result.updated)
        }

        val status = personoversiktRepository.getPersonOversiktStatus(PersonIdent(personOversiktStatus.fnr))!!
        assertNull(status.veilederIdent)
        assertNull(status.enhet)
        coVerify(exactly = 1) {
            behandlendeEnhetClient.unsetOppfolgingsenhet(any(), PersonIdent(personOversiktStatus.fnr))
        }
    }

    @Test
    fun `Does not reset tildelt veileder or enhet for personer with sist_endret less than two months ago`() {
        val threeMonthsAgo = LocalDate.now().minusMonths(3)
        val personOversiktStatus = generatePersonOversiktStatusWithTilfelleEnd(threeMonthsAgo)
        database.createPersonOversiktStatus(personOversiktStatus)
        database.setSistEndret(
            fnr = personOversiktStatus.fnr,
            sistEndret = Timestamp.from(OffsetDateTime.now().minusMonths(2).plusDays(4).toInstant()),
        )

        runBlocking {
            val result = reaperCronjob.runJob()

            assertEquals(0, result.failed)
            assertEquals(0, result.updated)
        }

        val status = personoversiktRepository.getPersonOversiktStatus(PersonIdent(personOversiktStatus.fnr))!!
        assertNotNull(status.veilederIdent)
        assertNotNull(status.enhet)
        coVerify(exactly = 0) {
            behandlendeEnhetClient.unsetOppfolgingsenhet(any(), any())
        }
    }

    @Test
    fun `Don't process personer with tilfelle that ended less than two months ago`() {
        val lessThanTwoMonthsAgo = LocalDate.now().minusMonths(2).plusDays(1)
        val personOversiktStatus = generatePersonOversiktStatusWithTilfelleEnd(lessThanTwoMonthsAgo)
        database.createPersonOversiktStatus(personOversiktStatus)

        runBlocking {
            val result = reaperCronjob.runJob()

            assertEquals(0, result.failed)
            assertEquals(0, result.updated)
            coVerify(exactly = 0) {
                behandlendeEnhetClient.unsetOppfolgingsenhet(any(), any())
            }
        }
    }
}

fun generatePersonOversiktStatusWithTilfelleEnd(tilfelleEnd: LocalDate): PersonOversiktStatus =
    generatePPersonOversiktStatus().copy(
        enhet = UserConstants.NAV_ENHET,
        veilederIdent = "Z999999",
        oppfolgingstilfelleUpdatedAt = OffsetDateTime.now(),
        oppfolgingstilfelleGeneratedAt = OffsetDateTime.now(),
        oppfolgingstilfelleStart = tilfelleEnd.minusDays(14),
        oppfolgingstilfelleEnd = tilfelleEnd,
        oppfolgingstilfelleBitReferanseInntruffet = OffsetDateTime.now(),
        oppfolgingstilfelleBitReferanseUuid = UUID.randomUUID(),
    ).toPersonOversiktStatus(emptyList())
