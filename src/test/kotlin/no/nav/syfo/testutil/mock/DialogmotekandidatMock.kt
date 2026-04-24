package no.nav.syfo.testutil.mock

import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import no.nav.syfo.application.dialogmotekandidat.DialogmotekandidatDTO
import no.nav.syfo.application.dialogmotekandidat.DialogmotekandidatResponseDTO
import no.nav.syfo.testutil.UserConstants
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
                    ),
                ),
                Pair(
                    UserConstants.ARBEIDSTAKER_2_FNR,
                    DialogmotekandidatDTO(
                        createdAt = LocalDateTime.now(),
                        personident = UserConstants.ARBEIDSTAKER_2_FNR,
                        isKandidat = true,
                    ),
                ),
            ),
        )
    )
}
