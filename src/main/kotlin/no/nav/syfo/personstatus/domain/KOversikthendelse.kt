package no.nav.syfo.personstatus.domain

import java.time.LocalDateTime

data class KOversikthendelse(
        val fnr: String,
        val hendelseId: String,
        val enhetId: String,
        val tidspunkt: LocalDateTime
)
