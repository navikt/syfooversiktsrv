package no.nav.syfo.personstatus.infrastructure.kafka

import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

data class ArbeidsuforhetvurderingRecord(
    val uuid: UUID,
    val createdAt: OffsetDateTime,
    val personident: String,
    val veilederident: String,
    val type: VurderingType,
    val begrunnelse: String,
    val gjelderFom: LocalDate?,
    val isFinal: Boolean,
)

enum class VurderingType {
    FORHANDSVARSEL, OPPFYLT, AVSLAG, IKKE_AKTUELL, AVSLAG_UTEN_FORHANDSVARSEL, OPPFYLT_UTEN_FORHANDSVARSEL
}
