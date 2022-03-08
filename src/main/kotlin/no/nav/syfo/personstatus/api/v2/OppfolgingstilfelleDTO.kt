package no.nav.syfo.personstatus.api.v2

import no.nav.syfo.personstatus.domain.Oppfolgingstilfelle
import java.time.LocalDate

data class OppfolgingstilfelleDTO(
    val virksomhetsnummer: String,
    val virksomhetsnavn: String,
    val fom: LocalDate,
    val tom: LocalDate,
)

fun Oppfolgingstilfelle.toOppfolgingstilfelleDTO() =
    OppfolgingstilfelleDTO(
        virksomhetsnummer = this.virksomhetsnummer,
        virksomhetsnavn = this.virksomhetsnavn,
        fom = this.fom,
        tom = this.tom,
    )
