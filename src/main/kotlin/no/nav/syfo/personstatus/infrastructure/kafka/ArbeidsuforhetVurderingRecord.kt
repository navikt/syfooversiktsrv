package no.nav.syfo.personstatus.infrastructure.kafka

import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

data class ArbeidsuforhetVurderingRecord(
    val uuid: UUID,
    val createdAt: OffsetDateTime,
    val personident: String,
    val veilederident: String,
    val type: VurderingType,
    val begrunnelse: String,
    val gjelderFom: LocalDate?,
    val isFinalVurdering: Boolean,
)

enum class VurderingType {
    FORHANDSVARSEL, OPPFYLT, AVSLAG
}
