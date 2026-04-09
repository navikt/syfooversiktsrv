package no.nav.syfo.application.dialogmote

import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class DialogmoteAvventDTO(
    val uuid: UUID,
    val createdAt: OffsetDateTime,
    val frist: LocalDate,
    val createdBy: String,
    val personident: String,
    val beskrivelse: String,
    val isLukket: Boolean,
)
