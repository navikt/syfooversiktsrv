package no.nav.syfo.oversikthendelsetilfelle.domain

import no.nav.syfo.personstatus.domain.Oppfolgingstilfelle
import java.time.LocalDate
import java.time.LocalDateTime

data class PPersonOppfolgingstilfelle(
    val id: Int,
    val sistEndret: LocalDateTime,
    val personOversiktStatusId: Int,
    val virksomhetsnummer: String,
    val virksomhetsnavn: String?,
    val fom: LocalDate,
    val tom: LocalDate,
    val gradert: Boolean,
)

fun PPersonOppfolgingstilfelle.toOppfolgingstilfelle() =
    Oppfolgingstilfelle(
        virksomhetsnummer = this.virksomhetsnummer,
        fom = this.fom,
        tom = this.tom,
        virksomhetsnavn = this.virksomhetsnavn ?: "",
    )
