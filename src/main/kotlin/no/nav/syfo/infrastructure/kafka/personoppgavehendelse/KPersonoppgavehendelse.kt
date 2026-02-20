package no.nav.syfo.infrastructure.kafka.personoppgavehendelse

import no.nav.syfo.domain.OversikthendelseType

data class KPersonoppgavehendelse(
    val personident: String,
    val hendelsetype: OversikthendelseType,
)
