package no.nav.syfo.personstatus.application.meroppfolging

import java.time.LocalDateTime
import java.util.*

data class SenOppfolgingKandidaterRequestDTO(
    val personidenter: List<String>
)

data class SenOppfolgingKandidaterResponseDTO(
    val kandidater: Map<String, SenOppfolgingKandidatDTO>
)

data class SenOppfolgingKandidatDTO(
    val uuid: UUID,
    val personident: String,
    val varselAt: LocalDateTime?,
    val svar: SvarResponseDTO?,
)

data class SvarResponseDTO(
    val svarAt: LocalDateTime,
    val onskerOppfolging: OnskerOppfolging,
)

enum class OnskerOppfolging {
    JA,
    NEI,
}
