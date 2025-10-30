package no.nav.syfo.personstatus.domain

import java.time.OffsetDateTime
import java.util.*

data class PPersonOppfolgingstilfelleVirksomhet(
    val id: Int,
    val uuid: UUID,
    val createdAt: OffsetDateTime,
    val virksomhetsnummer: Virksomhetsnummer,
    val virksomhetsnavn: String?,
    val personOversiktStatusId: Int,
)

fun List<PPersonOppfolgingstilfelleVirksomhet>.toPersonOppfolgingstilfelleVirksomhet() =
    this.map {
        PersonOppfolgingstilfelleVirksomhet(
            uuid = it.uuid,
            createdAt = it.createdAt,
            virksomhetsnummer = it.virksomhetsnummer,
            virksomhetsnavn = it.virksomhetsnavn,
        )
    }
