package no.nav.syfo.oversikthendelsetilfelle.domain

import java.time.LocalDate
import java.time.LocalDateTime

data class KOversikthendelsetilfelleV2(
        val fnr: String,
        val enhetId: String,
        val virksomhetsnummer: String,
        val virksomhetsnavn: String?,
        val gradert: Boolean,
        val fom: LocalDate,
        val tom: LocalDate,
        val tidspunkt: LocalDateTime
)
