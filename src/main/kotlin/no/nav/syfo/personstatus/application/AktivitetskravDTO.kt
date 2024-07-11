package no.nav.syfo.personstatus.application

import java.time.LocalDate
import java.time.LocalDateTime

data class AktivitetskravRequestDTO(
    val personidenter: List<String>,
)

data class GetAktivitetskravForPersonsResponseDTO(
    val aktivitetskravvurderinger: Map<String, AktivitetskravDTO>,
)

data class AktivitetskravDTO(
    val uuid: String,
    val createdAt: LocalDateTime,
    val status: AktivitetskravStatus,
    val vurderinger: List<AktivitetskravvurderingDTO>,
)

data class AktivitetskravvurderingDTO(
    val createdAt: LocalDateTime,
    val status: AktivitetskravStatus,
    val frist: LocalDate?,
    val varsel: AktivitetskravVarselDTO?,
)

data class AktivitetskravVarselDTO(
    val createdAt: LocalDateTime,
    val svarfrist: LocalDate,
)

enum class AktivitetskravStatus {
    NY,
    NY_VURDERING,
    AVVENT,
    UNNTAK,
    OPPFYLT,
    AUTOMATISK_OPPFYLT,
    FORHANDSVARSEL,
    STANS,
    IKKE_OPPFYLT,
    IKKE_AKTUELL,
    LUKKET,
}
