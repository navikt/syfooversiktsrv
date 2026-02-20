package no.nav.syfo.testutil.mock

import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import no.nav.syfo.application.manglendemedvirkning.ManglendeMedvirkningDTO
import no.nav.syfo.application.manglendemedvirkning.ManglendeMedvirkningResponseDTO
import no.nav.syfo.application.manglendemedvirkning.ManglendeMedvirkningVarselDTO
import no.nav.syfo.application.manglendemedvirkning.ManglendeMedvirkningVurderingType
import no.nav.syfo.testutil.UserConstants
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

fun MockRequestHandleScope.manglendeMedvirkningMockResponse(): HttpResponseData =
    respondOk(
        ManglendeMedvirkningResponseDTO(
            mapOf(
                Pair(
                    UserConstants.ARBEIDSTAKER_FNR,
                    ManglendeMedvirkningDTO(
                        uuid = UUID.randomUUID(),
                        personident = UserConstants.ARBEIDSTAKER_FNR,
                        createdAt = LocalDateTime.now().minusDays(1),
                        vurderingType = ManglendeMedvirkningVurderingType.FORHANDSVARSEL,
                        begrunnelse = "begrunnelse",
                        veilederident = UserConstants.VEILEDER_ID,
                        varsel = ManglendeMedvirkningVarselDTO(
                            uuid = UUID.randomUUID(),
                            createdAt = LocalDateTime.now().minusDays(1),
                            svarfrist = LocalDate.now().plusDays(1),
                        )
                    ),
                ),
            ),
        )
    )
