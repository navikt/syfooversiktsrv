package no.nav.syfo.infrastructure.kafka.oppfolgingsenfase

import java.time.OffsetDateTime
import java.util.UUID

data class KandidatStatusRecord(
    val uuid: UUID,
    val createdAt: OffsetDateTime,
    val personident: String,
    val status: StatusDTO,
)

data class StatusDTO(
    val value: Status,
    val isActive: Boolean,
)

enum class Status {
    KANDIDAT,
    FERDIGBEHANDLET
}
