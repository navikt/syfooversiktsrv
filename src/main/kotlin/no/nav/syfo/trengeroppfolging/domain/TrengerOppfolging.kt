package no.nav.syfo.trengeroppfolging.domain

import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.personstatus.domain.PersonOversiktStatus
import java.time.LocalDate
import java.util.UUID

data class TrengerOppfolging(
    val uuid: UUID,
    val personIdent: PersonIdent,
    val isActive: Boolean,
    val frist: LocalDate?,
) {
    fun toPersonoversiktStatus() = PersonOversiktStatus(
        fnr = personIdent.value,
    ).copy(
        trengerOppfolging = isActive,
        trengerOppfolgingFrist = frist,
    )
}
