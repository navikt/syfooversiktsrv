package no.nav.syfo.testutil.mock

import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import no.nav.syfo.personstatus.application.oppfolgingsoppgave.*
import no.nav.syfo.personstatus.domain.PersonIdent
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_2_FNR
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_3_FNR
import no.nav.syfo.testutil.UserConstants.ARBEIDSTAKER_FNR
import no.nav.syfo.testutil.UserConstants.VEILEDER_ID
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

fun MockRequestHandleScope.oppfolgingsoppgaveMockResponse(): HttpResponseData =
    respondOk(oppfolgingsoppgaverResponseDTO)

private val oppfolgingsoppgaverResponseDTO = OppfolgingsoppgaverResponseDTO(
    oppfolgingsoppgaver = mapOf(
        ARBEIDSTAKER_FNR to generateOppfolgingsoppgave("FOLG_OPP_ETTER_NESTE_SYKMELDING"),
        ARBEIDSTAKER_2_FNR to generateOppfolgingsoppgave("VURDER_ANNEN_YTELSE"),
        ARBEIDSTAKER_3_FNR to generateOppfolgingsoppgave("VURDER_14A"),
    )
)

private fun generateOppfolgingsoppgave(
    oppfolgingsgrunn: String,
): OppfolgingsoppgaveDTO = OppfolgingsoppgaveDTO(
    uuid = UUID.randomUUID().toString(),
    updatedAt = LocalDateTime.now(),
    createdAt = LocalDateTime.now(),
    isActive = true,
    personIdent = PersonIdent(ARBEIDSTAKER_FNR),
    publishedAt = null,
    removedBy = null,
    versjoner = listOf(
        OppfolgingoppgaveVersjonDTO(
            uuid = UUID.randomUUID().toString(),
            createdAt = LocalDateTime.now(),
            createdBy = VEILEDER_ID,
            tekst = "En tekst",
            oppfolgingsgrunn = oppfolgingsgrunn,
            frist = LocalDate.now().plusDays(14),
        )
    ),
)
