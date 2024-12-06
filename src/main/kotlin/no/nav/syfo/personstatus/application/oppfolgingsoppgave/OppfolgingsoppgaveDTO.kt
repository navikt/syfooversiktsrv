package no.nav.syfo.personstatus.application.oppfolgingsoppgave

import no.nav.syfo.personstatus.domain.PersonIdent
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
){
    companion object {
        fun fromOppfolgingsoppgaveNewDTO(oppfolgingsoppgaveNewDTO: OppfolgingsoppgaveNewDTO): OppfolgingsoppgaveDTO =
            OppfolgingsoppgaveDTO(
                uuid = oppfolgingsoppgaveNewDTO.uuid,
                updatedAt = oppfolgingsoppgaveNewDTO.updatedAt,
                createdAt = oppfolgingsoppgaveNewDTO.createdAt,
                createdBy = oppfolgingsoppgaveNewDTO.sisteVersjon().createdBy,
                tekst = oppfolgingsoppgaveNewDTO.sisteVersjon().tekst,
                oppfolgingsgrunn = oppfolgingsoppgaveNewDTO.sisteVersjon().oppfolgingsgrunn,
                frist = oppfolgingsoppgaveNewDTO.sisteVersjon().frist
            )
    }
}

data class OppfolgingsoppgaverNewResponseDTO(
    val oppfolgingsoppgaver: Map<String, OppfolgingsoppgaveNewDTO>
)

data class OppfolgingsoppgaveNewDTO(
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