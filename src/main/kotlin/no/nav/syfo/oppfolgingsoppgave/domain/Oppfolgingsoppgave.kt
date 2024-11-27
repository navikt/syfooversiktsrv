package no.nav.syfo.oppfolgingsoppgave.domain

import no.nav.syfo.personstatus.domain.PersonIdent
import no.nav.syfo.personstatus.domain.PersonOversiktStatus
import java.time.LocalDate
import java.util.UUID

data class Oppfolgingsoppgave private constructor(
    val uuid: UUID,
    val personIdent: PersonIdent,
    val isActive: Boolean,
    val frist: LocalDate?,
) {
    companion object {
        fun create(uuid: UUID, personIdent: String, isActive: Boolean, frist: LocalDate?): Oppfolgingsoppgave = Oppfolgingsoppgave(
            uuid = uuid,
            personIdent = PersonIdent(personIdent),
            isActive = isActive,
            frist = if (isActive) frist else null,
        )
    }

    fun toPersonoversiktStatus() = PersonOversiktStatus(
        fnr = personIdent.value,
    ).copy(
        isAktivOppfolgingsoppgave = isActive,
    )
}
