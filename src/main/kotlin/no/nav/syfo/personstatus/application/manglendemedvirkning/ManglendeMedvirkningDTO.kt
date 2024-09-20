package no.nav.syfo.personstatus.application.manglendemedvirkning

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class ManglendeMedvirkningRequestDTO(
    val personidenter: List<String>
)

data class ManglendeMedvirkningResponseDTO(
    val vurderinger: Map<String, ManglendeMedvirkningDTO>
)

data class ManglendeMedvirkningDTO(
    val uuid: UUID,
    val personident: String,
    val createdAt: LocalDateTime,
    val vurderingType: ManglendeMedvirkningVurderingType,
    val begrunnelse: String,
    val varsel: ManglendeMedvirkningVarselDTO?,
    val veilederident: String,
)

data class ManglendeMedvirkningVarselDTO(
    val uuid: UUID,
    val createdAt: LocalDateTime,
    val svarfrist: LocalDate,
)

enum class ManglendeMedvirkningVurderingType {
    FORHANDSVARSEL, OPPFYLT, STANS, IKKE_AKTUELL, UNNTAK
}
