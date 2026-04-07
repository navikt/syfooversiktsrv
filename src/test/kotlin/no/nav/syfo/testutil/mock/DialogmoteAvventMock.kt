package no.nav.syfo.testutil.mock

import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import no.nav.syfo.application.dialogmote.DialogmoteAvventDTO
import no.nav.syfo.testutil.UserConstants
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

fun MockRequestHandleScope.dialogmoteAvventMockResponse(request: HttpRequestData): HttpResponseData =
    respondOk(
        listOf(
            DialogmoteAvventDTO(
                uuid = UUID.randomUUID(),
                createdAt = OffsetDateTime.now(),
                frist = LocalDate.now().plusWeeks(2),
                createdBy = UserConstants.VEILEDER_ID,
                personident = UserConstants.ARBEIDSTAKER_FNR,
                beskrivelse = "Avventer tilbakemelding",
                isLukket = false,
            ),
        ),
    )
