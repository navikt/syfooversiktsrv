package no.nav.syfo.personoppgavehendelse.kafka

import no.nav.syfo.personstatus.domain.OversikthendelseType

data class KPersonoppgavehendelse(
    val personident: String,
    val hendelsetype: String,
)

fun KPersonoppgavehendelse.oversikthendelseType(): OversikthendelseType? =
    OversikthendelseType.values().firstOrNull {
        it.name == this.hendelsetype
    }
