package no.nav.syfo.personstatus.infrastructure.kafka.manglendemedvirkning

import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

data class VurderingRecord(
    val uuid: UUID,
    val personident: String,
    val veilederident: String,
    val createdAt: OffsetDateTime,
    val begrunnelse: String,
    val varsel: Varsel?,
    val vurderingType: VurderingTypeDTO,
)

data class VurderingTypeDTO(
    val value: VurderingType,
    val isActive: Boolean,
)

enum class VurderingType {
    FORHANDSVARSEL, OPPFYLT, STANS, IKKE_AKTUELL, UNNTAK
}

data class Varsel(
    val uuid: UUID,
    val createdAt: OffsetDateTime,
    val svarfrist: LocalDate,
)
