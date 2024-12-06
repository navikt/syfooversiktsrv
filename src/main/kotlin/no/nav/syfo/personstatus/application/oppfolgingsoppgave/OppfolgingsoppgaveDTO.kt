package no.nav.syfo.personstatus.application.oppfolgingsoppgave

import no.nav.syfo.personstatus.domain.PersonIdent
import java.time.LocalDate
import java.time.LocalDateTime

data class OppfolgingsoppgaverRequestDTO(
    val personidenter: List<String>
)

data class OppfolgingsoppgaverLatestVersionResponseDTO(
    val oppfolgingsoppgaver: Map<String, OppfolgingsoppgaveLatestVersionDTO>
)

data class OppfolgingsoppgaveLatestVersionDTO(
    val uuid: String,
    val createdBy: String,
    val updatedAt: LocalDateTime,
    val createdAt: LocalDateTime,
    val tekst: String?,
    val oppfolgingsgrunn: String,
    val frist: LocalDate?,
) {
    companion object {
        fun fromOppfolgingsoppgaveDTO(oppfolgingsoppgaveDTO: OppfolgingsoppgaveDTO): OppfolgingsoppgaveLatestVersionDTO =
            OppfolgingsoppgaveLatestVersionDTO(
                uuid = oppfolgingsoppgaveDTO.uuid,
                updatedAt = oppfolgingsoppgaveDTO.updatedAt,
                createdAt = oppfolgingsoppgaveDTO.createdAt,
                createdBy = oppfolgingsoppgaveDTO.sisteVersjon().createdBy,
                tekst = oppfolgingsoppgaveDTO.sisteVersjon().tekst,
                oppfolgingsgrunn = oppfolgingsoppgaveDTO.sisteVersjon().oppfolgingsgrunn,
                frist = oppfolgingsoppgaveDTO.sisteVersjon().frist
            )
    }
}

data class OppfolgingsoppgaverResponseDTO(
    val oppfolgingsoppgaver: Map<String, OppfolgingsoppgaveDTO>
)

data class OppfolgingsoppgaveDTO(
    val uuid: String,
    val personIdent: PersonIdent,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val isActive: Boolean,
    val publishedAt: LocalDateTime?,
    val removedBy: String?,
    val versjoner: List<OppfolgingoppgaveVersjonDTO>
) {
    fun sisteVersjon() = versjoner.first()
}

data class OppfolgingoppgaveVersjonDTO(
    val uuid: String,
    val createdBy: String,
    val createdAt: LocalDateTime,
    val tekst: String?,
    val oppfolgingsgrunn: String,
    val frist: LocalDate?,
)
