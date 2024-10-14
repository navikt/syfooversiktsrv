package no.nav.syfo.testutil.mock

import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import no.nav.syfo.personstatus.application.aktivitetskrav.AktivitetskravDTO
import no.nav.syfo.personstatus.application.aktivitetskrav.AktivitetskravStatus
import no.nav.syfo.personstatus.application.aktivitetskrav.GetAktivitetskravForPersonsResponseDTO
import no.nav.syfo.testutil.UserConstants
import java.time.LocalDateTime
import java.util.UUID

fun MockRequestHandleScope.aktivitetskravMockResponse(): HttpResponseData =
    respondOk(
        GetAktivitetskravForPersonsResponseDTO(
            mapOf(
                Pair(
                    UserConstants.ARBEIDSTAKER_FNR,
                    AktivitetskravDTO(
                        uuid = UUID.randomUUID().toString(),
                        createdAt = LocalDateTime.now(),
                        status = AktivitetskravStatus.NY,
                        vurderinger = emptyList(),
                    ),
                ),
            ),
        )
    )
