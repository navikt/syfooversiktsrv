package no.nav.syfo.trengeroppfolging.domain

import no.nav.syfo.personstatus.domain.PersonIdent
import no.nav.syfo.personstatus.domain.PersonOversiktStatus
import java.time.LocalDate
import java.util.UUID

data class TrengerOppfolging private constructor(
    val uuid: UUID,
    val personIdent: PersonIdent,
    val isActive: Boolean,
    val frist: LocalDate?,
) {
    companion object {
        fun create(uuid: UUID, personIdent: String, isActive: Boolean, frist: LocalDate?): TrengerOppfolging = TrengerOppfolging(
            uuid = uuid,
            personIdent = PersonIdent(personIdent),
            isActive = isActive,
            frist = if (isActive) frist else null,
        )
    }

    fun toPersonoversiktStatus() = PersonOversiktStatus(
        fnr = personIdent.value,
    ).copy(
        trengerOppfolging = isActive,
    )
}
