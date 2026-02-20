package no.nav.syfo.testutil.mock

import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import no.nav.syfo.application.dialogmotekandidat.AvventDTO
import no.nav.syfo.application.dialogmotekandidat.DialogmotekandidatDTO
import no.nav.syfo.application.dialogmotekandidat.DialogmotekandidatResponseDTO
import no.nav.syfo.testutil.UserConstants
import java.time.LocalDate
import java.time.LocalDateTime

fun MockRequestHandleScope.dialogmotekandidatMockResponse(request: HttpRequestData): HttpResponseData {
    return respondOk(
        DialogmotekandidatResponseDTO(
            mapOf(
                Pair(
                    UserConstants.ARBEIDSTAKER_FNR,
                    DialogmotekandidatDTO(
                        createdAt = LocalDateTime.now(),
                        personident = UserConstants.ARBEIDSTAKER_FNR,
                        isKandidat = true,
                        avvent = null,
                    ),
                ),
                Pair(
                    UserConstants.ARBEIDSTAKER_2_FNR,
                    DialogmotekandidatDTO(
                        createdAt = LocalDateTime.now(),
                        personident = UserConstants.ARBEIDSTAKER_2_FNR,
                        isKandidat = true,
                        avvent = AvventDTO(
                            frist = LocalDate.now().plusWeeks(2),
                            createdBy = "Z999999",
                            beskrivelse = "Avventer tilbakemelding fra behandler",
                        ),
                    ),
                ),
            ),
        )
    )
}
