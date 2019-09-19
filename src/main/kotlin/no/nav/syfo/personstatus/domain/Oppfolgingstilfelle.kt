package no.nav.syfo.personstatus.domain

import java.time.LocalDate

data class Oppfolgingstilfelle(
        val virksomhetsnummer: String,
        val fom: LocalDate,
        val tom: LocalDate
)
