package no.nav.syfo.trengeroppfolging.domain

import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.personstatus.domain.PersonOversiktStatus
import java.util.UUID

data class TrengerOppfolging(
    val uuid: UUID,
    val personIdent: PersonIdent,
    val isActive: Boolean,
) {
    fun toPersonoversiktStatus() = PersonOversiktStatus(
        fnr = personIdent.value,
    ).copy(
        trengerOppfolging = isActive,
    )
}
