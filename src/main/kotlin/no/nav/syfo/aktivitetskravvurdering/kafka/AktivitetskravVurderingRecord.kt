package no.nav.syfo.aktivitetskravvurdering.kafka

import java.time.LocalDate
import java.time.OffsetDateTime

data class AktivitetskravVurderingRecord(
    val uuid: String,
    val personIdent: String,
    val createdAt: OffsetDateTime,
    val status: String,
    val isFinal: Boolean,
    val stoppunktAt: LocalDate,
    val beskrivelse: String?,
    val sistVurdert: OffsetDateTime?,
    val frist: LocalDate?,
)
