package no.nav.syfo.application

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.syfo.application.aktivitetskrav.IAktivitetskravClient
import no.nav.syfo.application.arbeidsuforhet.IArbeidsuforhetvurderingClient
import no.nav.syfo.application.dialogmote.DialogmoteAvventDTO
import no.nav.syfo.application.dialogmote.IDialogmoteClient
import no.nav.syfo.application.dialogmotekandidat.IDialogmotekandidatClient
import no.nav.syfo.application.manglendemedvirkning.IManglendeMedvirkningClient
import no.nav.syfo.application.meroppfolging.IMeroppfolgingClient
import no.nav.syfo.application.oppfolgingsoppgave.IOppfolgingsoppgaveClient
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.domain.PersonOversiktStatus
import no.nav.syfo.testutil.UserConstants
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class PersonoversiktOppgaverServiceTest {

    private val dialogmoteClient = mockk<IDialogmoteClient>()
    private val service = PersonoversiktOppgaverService(
        arbeidsuforhetvurderingClient = mockk<IArbeidsuforhetvurderingClient>().also {
            coEvery { it.getLatestVurderinger(any(), any(), any()) } returns null
        },
        manglendeMedvirkningClient = mockk<IManglendeMedvirkningClient>().also {
            coEvery { it.getLatestVurderinger(any(), any(), any()) } returns null
        },
        aktivitetskravClient = mockk<IAktivitetskravClient>().also {
            coEvery { it.getAktivitetskravForPersons(any(), any(), any()) } returns null
        },
        oppfolgingsoppgaveClient = mockk<IOppfolgingsoppgaveClient>().also {
            coEvery { it.getActiveOppfolgingsoppgaver(any(), any(), any()) } returns null
        },
        merOppfolgingClient = mockk<IMeroppfolgingClient>().also {
            coEvery { it.getSenOppfolgingKandidater(any(), any(), any()) } returns null
        },
        dialogmotekandidatClient = mockk<IDialogmotekandidatClient>().also {
            coEvery { it.getDialogmotekandidater(any(), any(), any()) } returns null
        },
        dialogmoteClient = dialogmoteClient,
    )

    private val avvent = DialogmoteAvventDTO(
        uuid = UUID.randomUUID(),
        createdAt = OffsetDateTime.now(),
        frist = LocalDate.now().plusWeeks(2),
        createdBy = UserConstants.VEILEDER_ID,
        personident = UserConstants.ARBEIDSTAKER_FNR,
        beskrivelse = "Avventer tilbakemelding",
        isLukket = false,
    )

    @BeforeEach
    fun setUp() {
        coEvery { dialogmoteClient.getDialogmoteAvvent(any(), any()) } returns listOf(avvent)
    }

    @Test
    fun `getDialogmoteAvventForPersons fetches avvent when dialogmotesvarUbehandlet is true`() {
        val person = PersonOversiktStatus(
            fnr = UserConstants.ARBEIDSTAKER_FNR,
            dialogmotesvarUbehandlet = true,
        )

        runBlocking { service.getAktiveOppgaver("callId", "token", listOf(person)) }

        coVerify(exactly = 1) {
            dialogmoteClient.getDialogmoteAvvent(
                token = "token",
                personidenter = match { it.map(PersonIdent::value).contains(UserConstants.ARBEIDSTAKER_FNR) },
            )
        }
    }

    @Test
    fun `getDialogmoteAvventForPersons fetches avvent when dialogmotekandidat is true`() {
        val person = PersonOversiktStatus(
            fnr = UserConstants.ARBEIDSTAKER_FNR,
            dialogmotekandidat = true,
        )

        runBlocking { service.getAktiveOppgaver("callId", "token", listOf(person)) }

        coVerify(exactly = 1) {
            dialogmoteClient.getDialogmoteAvvent(any(), any())
        }
    }

    @Test
    fun `getDialogmoteAvventForPersons fetches avvent when motebehovUbehandlet is true`() {
        val person = PersonOversiktStatus(
            fnr = UserConstants.ARBEIDSTAKER_FNR,
            motebehovUbehandlet = true,
        )

        runBlocking { service.getAktiveOppgaver("callId", "token", listOf(person)) }

        coVerify(exactly = 1) {
            dialogmoteClient.getDialogmoteAvvent(any(), any())
        }
    }

    @Test
    fun `getDialogmoteAvventForPersons does not fetch avvent when no dialogmote conditions are true`() {
        val person = PersonOversiktStatus(
            fnr = UserConstants.ARBEIDSTAKER_FNR,
            dialogmotesvarUbehandlet = false,
            dialogmotekandidat = false,
            motebehovUbehandlet = false,
        )

        runBlocking { service.getAktiveOppgaver("callId", "token", listOf(person)) }

        coVerify(exactly = 0) {
            dialogmoteClient.getDialogmoteAvvent(any(), any())
        }
    }

    @Test
    fun `getDialogmoteAvventForPersons does not fetch avvent when motebehovUbehandlet is null`() {
        val person = PersonOversiktStatus(
            fnr = UserConstants.ARBEIDSTAKER_FNR,
            dialogmotesvarUbehandlet = false,
            dialogmotekandidat = null,
            motebehovUbehandlet = null,
        )

        runBlocking { service.getAktiveOppgaver("callId", "token", listOf(person)) }

        coVerify(exactly = 0) {
            dialogmoteClient.getDialogmoteAvvent(any(), any())
        }
    }

    @Test
    fun `getDialogmoteAvventForPersons only fetches avvent for eligible persons in a mixed list`() {
        val eligiblePerson = PersonOversiktStatus(
            fnr = UserConstants.ARBEIDSTAKER_FNR,
            dialogmotesvarUbehandlet = true,
        )
        val ineligiblePerson = PersonOversiktStatus(
            fnr = UserConstants.ARBEIDSTAKER_2_FNR,
            dialogmotesvarUbehandlet = false,
            dialogmotekandidat = false,
            motebehovUbehandlet = false,
        )

        runBlocking { service.getAktiveOppgaver("callId", "token", listOf(eligiblePerson, ineligiblePerson)) }

        coVerify(exactly = 1) {
            dialogmoteClient.getDialogmoteAvvent(
                token = any(),
                personidenter = match { personidenter ->
                    personidenter.map(PersonIdent::value).let {
                        it.contains(UserConstants.ARBEIDSTAKER_FNR) && !it.contains(UserConstants.ARBEIDSTAKER_2_FNR)
                    }
                },
            )
        }
    }

    @Test
    fun `getAktiveOppgaver returns avvent for person when client returns result`() {
        val person = PersonOversiktStatus(
            fnr = UserConstants.ARBEIDSTAKER_FNR,
            dialogmotesvarUbehandlet = true,
        )

        val result = runBlocking { service.getAktiveOppgaver("callId", "token", listOf(person)) }

        assertNotNull(result[UserConstants.ARBEIDSTAKER_FNR]?.dialogmoteAvvent)
        assertEquals(avvent.uuid, result[UserConstants.ARBEIDSTAKER_FNR]?.dialogmoteAvvent?.uuid)
    }

    @Test
    fun `getAktiveOppgaver returns null avvent for person with no dialogmote conditions`() {
        val person = PersonOversiktStatus(
            fnr = UserConstants.ARBEIDSTAKER_FNR,
            dialogmotesvarUbehandlet = false,
            dialogmotekandidat = false,
            motebehovUbehandlet = false,
        )

        val result = runBlocking { service.getAktiveOppgaver("callId", "token", listOf(person)) }

        assertNull(result[UserConstants.ARBEIDSTAKER_FNR]?.dialogmoteAvvent)
    }
}
