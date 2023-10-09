package no.nav.syfo.huskelapp.domain

import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.personstatus.domain.PersonOversiktStatus
import java.util.UUID

data class Huskelapp(
    val uuid: UUID,
    val personIdent: PersonIdent,
    val isActive: Boolean,
) {
    fun toPersonoversiktStatus() = PersonOversiktStatus(
        fnr = personIdent.value,
    ).copy(
        huskelappActive = isActive,
    )
}
