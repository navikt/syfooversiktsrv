package no.nav.syfo.testutil.mock

import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import no.nav.syfo.application.meroppfolging.OnskerOppfolging
import no.nav.syfo.application.meroppfolging.SenOppfolgingKandidatDTO
import no.nav.syfo.application.meroppfolging.SenOppfolgingKandidaterResponseDTO
import no.nav.syfo.application.meroppfolging.SvarResponseDTO
import no.nav.syfo.testutil.UserConstants
import java.time.LocalDateTime
import java.util.UUID

val senOppfolgingKandidatDTO = SenOppfolgingKandidatDTO(
    uuid = UUID.randomUUID(),
    personident = UserConstants.ARBEIDSTAKER_FNR,
    varselAt = LocalDateTime.now().minusDays(3),
    svar = SvarResponseDTO(
        svarAt = LocalDateTime.now(),
        onskerOppfolging = OnskerOppfolging.JA,
    ),
)

fun MockRequestHandleScope.merOppfolgingMockResponse(): HttpResponseData = respondOk(
    SenOppfolgingKandidaterResponseDTO(
        mapOf(UserConstants.ARBEIDSTAKER_FNR to senOppfolgingKandidatDTO),
    )
)
