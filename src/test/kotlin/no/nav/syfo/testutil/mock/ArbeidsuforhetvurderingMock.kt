package no.nav.syfo.testutil.mock

import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import no.nav.syfo.personstatus.application.arbeidsuforhet.ArbeidsuforhetvurderingDTO
import no.nav.syfo.personstatus.application.arbeidsuforhet.ArbeidsuforhetvurderingerResponseDTO
import no.nav.syfo.personstatus.application.arbeidsuforhet.VarselDTO
import no.nav.syfo.personstatus.application.arbeidsuforhet.VurderingType
import no.nav.syfo.testutil.UserConstants
import java.time.LocalDate
import java.time.LocalDateTime

val latestVurdering: LocalDateTime = LocalDateTime.now().minusDays(1)

fun MockRequestHandleScope.arbeidsuforhetVurderingMockResponse(): HttpResponseData =
    respondOk(
        ArbeidsuforhetvurderingerResponseDTO(
            mapOf(
                Pair(
                    UserConstants.ARBEIDSTAKER_FNR,
                    ArbeidsuforhetvurderingDTO(
                        createdAt = latestVurdering,
                        type = VurderingType.FORHANDSVARSEL,
                        varsel = VarselDTO(LocalDate.now().plusDays(1))
                    ),
                ),
            ),
        )
    )
