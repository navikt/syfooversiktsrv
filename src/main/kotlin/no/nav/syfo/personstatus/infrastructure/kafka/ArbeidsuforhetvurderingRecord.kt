package no.nav.syfo.personstatus.infrastructure.kafka

import java.util.*

data class ArbeidsuforhetvurderingRecord(
    val uuid: UUID,
    val personident: String,
    val isFinal: Boolean,
)
