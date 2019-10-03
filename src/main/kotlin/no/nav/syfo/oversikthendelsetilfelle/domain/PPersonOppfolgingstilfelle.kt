package no.nav.syfo.oversikthendelsetilfelle.domain

import java.time.LocalDate

data class PPersonOppfolgingstilfelle(
        val id: Int,
        val personOversiktStatusId: Int,
        val virksomhetsnummer: String,
        val virksomhetsnavn: String?,
        val fom: LocalDate,
        val tom: LocalDate,
        val gradert: Boolean
)
