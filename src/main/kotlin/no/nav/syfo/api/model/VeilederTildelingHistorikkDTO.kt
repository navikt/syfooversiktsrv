package no.nav.syfo.api.model

import java.time.LocalDate

data class VeilederTildelingHistorikkDTO(
    val tildeltDato: LocalDate,
    val tildeltVeileder: String?,
    val tildeltEnhet: String,
    val tildeltAv: String,
)
