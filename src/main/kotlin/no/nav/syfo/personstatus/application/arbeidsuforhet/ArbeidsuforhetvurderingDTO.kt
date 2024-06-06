package no.nav.syfo.personstatus.application.arbeidsuforhet

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class ArbeidsuforhetvurderingDTO(
    val createdAt: LocalDateTime,
    val type: VurderingType,
    val varsel: VarselDTO?,
)

data class VarselDTO(
    val svarfrist: LocalDate,
)

enum class VurderingType {
    FORHANDSVARSEL, OPPFYLT, AVSLAG
}
