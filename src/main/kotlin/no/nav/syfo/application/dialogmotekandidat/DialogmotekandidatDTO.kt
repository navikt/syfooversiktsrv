package no.nav.syfo.application.dialogmotekandidat

import java.time.LocalDate
import java.time.LocalDateTime

data class DialogmotekandidatRequestDTO(
    val personidenter: List<String>,
)

data class DialogmotekandidatResponseDTO(
    val dialogmotekandidater: Map<String, DialogmotekandidatDTO>,
)

data class DialogmotekandidatDTO(
    val createdAt: LocalDateTime,
    val personident: String,
    val isKandidat: Boolean,
    val avvent: AvventDTO?,
)

data class AvventDTO(
    val frist: LocalDate,
    val createdBy: String,
    val beskrivelse: String,
)
