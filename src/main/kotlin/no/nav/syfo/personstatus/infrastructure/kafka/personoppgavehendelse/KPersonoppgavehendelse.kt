package no.nav.syfo.personstatus.infrastructure.kafka.personoppgavehendelse

import no.nav.syfo.personstatus.domain.OversikthendelseType

data class KPersonoppgavehendelse(
    val personident: String,
    val hendelsetype: OversikthendelseType,
)
