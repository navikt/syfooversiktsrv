package no.nav.syfo.personstatus.application.oppfolgingsoppgave

import java.time.LocalDate
import java.time.LocalDateTime

data class OppfolgingsoppgaverRequestDTO(
    val personidenter: List<String>
)

data class OppfolgingsoppgaverResponseDTO(
    val oppfolgingsoppgaver: Map<String, OppfolgingsoppgaveDTO>
)

data class OppfolgingsoppgaveDTO(
    val uuid: String,
    val createdBy: String,
    val updatedAt: LocalDateTime,
    val createdAt: LocalDateTime,
    val tekst: String?,
    val oppfolgingsgrunn: String,
    val frist: LocalDate?,
)
