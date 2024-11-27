package no.nav.syfo.oppfolgingsoppgave.kafka

import no.nav.syfo.oppfolgingsoppgave.domain.Oppfolgingsoppgave
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class OppfolgingsoppgaveRecord(
    val uuid: UUID,
    val personIdent: String,
    val veilederIdent: String,
    val tekst: String?,
    val oppfolgingsgrunner: List<String>,
    val frist: LocalDate?,
    val isActive: Boolean,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
) {
    fun toOppfolgingsoppgave() = Oppfolgingsoppgave.create(
        uuid = uuid,
        personIdent = personIdent,
        isActive = isActive,
        frist = frist,
    )
}
