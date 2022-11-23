package no.nav.syfo.aktivitetskravvurdering

import io.mockk.*
import no.nav.syfo.aktivitetskravvurdering.domain.Aktivitetskrav
import no.nav.syfo.aktivitetskravvurdering.domain.AktivitetskravVurderingStatus
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.personstatus.db.*
import no.nav.syfo.personstatus.domain.PersonOversiktStatus
import no.nav.syfo.testutil.UserConstants
import no.nav.syfo.testutil.generator.generatePPersonOversiktStatus
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.sql.Connection
import java.time.LocalDate
import java.time.OffsetDateTime

class AktivitetskravServiceSpek : Spek({
    val CREATE_OR_UPDATE_PERSON_OVERSIKT_STATUS_PATH = "no.nav.syfo.personstatus.db.CreateOrUpdatePersonOversiktStatusKt"
    val UPDATE_PERSON_OVERSIKT_STATUS_COLUMN_PATH = "no.nav.syfo.personstatus.db.UpdatePersonOversiktStatusColumnKt"
    val GET_FROM_PERSON_OVERSIKT_STATUS_PATH = "no.nav.syfo.personstatus.db.GetFromPersonOversiktStatusKt"

    describe("Persist aktivitetskrav in database") {

        val connection = mockk<Connection>(relaxed = true)

        beforeEachTest {
            mockkStatic(CREATE_OR_UPDATE_PERSON_OVERSIKT_STATUS_PATH)
            mockkStatic(UPDATE_PERSON_OVERSIKT_STATUS_COLUMN_PATH)
            mockkStatic(GET_FROM_PERSON_OVERSIKT_STATUS_PATH)
        }

        afterEachTest {
            clearMocks(connection)
            unmockkStatic(CREATE_OR_UPDATE_PERSON_OVERSIKT_STATUS_PATH)
            unmockkStatic(UPDATE_PERSON_OVERSIKT_STATUS_COLUMN_PATH)
            unmockkStatic(GET_FROM_PERSON_OVERSIKT_STATUS_PATH)
        }

        it("Create new personoversiktstatus with aktivitetskravinfo if no row exist for innbygger") {
            val updatedAt = OffsetDateTime.now()
            val stoppunkt = LocalDate.now().plusDays(7)
            val aktivitetskrav = Aktivitetskrav(
                personIdent = PersonIdent(UserConstants.ARBEIDSTAKER_FNR),
                status = AktivitetskravVurderingStatus.NY,
                updatedAt = updatedAt,
                stoppunkt = stoppunkt,
            )
            val expectedPersonOversiktStatus = PersonOversiktStatus(fnr = UserConstants.ARBEIDSTAKER_FNR).copy(
                aktivitetskrav = AktivitetskravVurderingStatus.NY,
                aktivitetskravStoppunkt = stoppunkt,
                aktivitetskravUpdatedAt = updatedAt,
            )
            every { connection.getPersonOversiktStatusList(any()) } returns emptyList()
            justRun { connection.createPersonOversiktStatus(any(), any()) }

            persistAktivitetskrav(
                connection = connection,
                aktivitetskrav = aktivitetskrav,
            )

            verify(exactly = 1) { connection.getPersonOversiktStatusList(UserConstants.ARBEIDSTAKER_FNR) }
            verify(exactly = 1) { connection.createPersonOversiktStatus(commit = false, personOversiktStatus = expectedPersonOversiktStatus) }
            verify(exactly = 0) { connection.updatePersonOversiktStatusAktivitetskrav(any(), any()) }
        }

        it("Update personoversiktstatus with aktivitetskravinfo if innbygger already exists in database") {
            val aktivitetskrav = Aktivitetskrav(
                personIdent = PersonIdent(UserConstants.ARBEIDSTAKER_FNR),
                status = AktivitetskravVurderingStatus.UNNTAK,
                updatedAt = OffsetDateTime.now(),
                stoppunkt = LocalDate.now().plusDays(7),
            )
            val existingPPersonOversiktStatus = generatePPersonOversiktStatus()
            every { connection.getPersonOversiktStatusList(any()) } returns listOf(existingPPersonOversiktStatus)
            justRun { connection.updatePersonOversiktStatusAktivitetskrav(any(), any()) }

            persistAktivitetskrav(
                connection = connection,
                aktivitetskrav = aktivitetskrav,
            )

            verify(exactly = 1) { connection.getPersonOversiktStatusList(UserConstants.ARBEIDSTAKER_FNR) }
            verify(exactly = 0) { connection.createPersonOversiktStatus(any(), any()) }
            verify(exactly = 1) { connection.updatePersonOversiktStatusAktivitetskrav(pPersonOversiktStatus = existingPPersonOversiktStatus, aktivitetskrav) }
        }
    }
})
