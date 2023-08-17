package no.nav.syfo.personoppgavehendelse.kafka

import no.nav.syfo.personstatus.domain.OversikthendelseType

data class KPersonoppgavehendelse(
    val personident: String,
    val hendelsetype: OversikthendelseType,
)
