package no.nav.syfo.infrastructure.kafka.frisktilarbeid

import no.nav.syfo.domain.PersonOversiktStatus
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

data class VedtakStatusRecord(
    val uuid: UUID,
    val personident: String,
    val begrunnelse: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val status: Status,
    val statusAt: OffsetDateTime,
    val statusBy: String,
)

enum class Status {
    FATTET,
    FERDIG_BEHANDLET,
}

fun VedtakStatusRecord.toPersonOversiktStatus() = PersonOversiktStatus(
    fnr = this.personident,
    friskmeldingTilArbeidsformidlingFom = if (this.status == Status.FATTET) this.fom else null,
)
