package no.nav.syfo.personstatus.application.arbeidsuforhet

import java.time.LocalDate
import java.time.LocalDateTime

data class ArbeidsuforhetvurderingerRequestDTO(
    val personidenter: List<String>,
)

data class ArbeidsuforhetvurderingerResponseDTO(
    val vurderinger: Map<String, ArbeidsuforhetvurderingDTO>,
)

data class ArbeidsuforhetvurderingDTO(
    val createdAt: LocalDateTime,
    val type: String,
    val varsel: VarselDTO?,
)

data class VarselDTO(
    val svarfrist: LocalDate,
)
