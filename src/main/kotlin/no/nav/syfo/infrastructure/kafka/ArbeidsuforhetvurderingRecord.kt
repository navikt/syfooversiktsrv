package no.nav.syfo.infrastructure.kafka

import java.util.*

data class ArbeidsuforhetvurderingRecord(
    val uuid: UUID,
    val personident: String,
    val isFinal: Boolean,
)
