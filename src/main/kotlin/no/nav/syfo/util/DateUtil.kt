package no.nav.syfo.util

import java.sql.Timestamp
import java.time.*

fun convert(localDate: LocalDate): Timestamp =
    Timestamp.valueOf(localDate.atStartOfDay())

fun convert(timestamp: Timestamp): LocalDate =
    timestamp.toLocalDateTime().toLocalDate()

val defaultZoneOffset: ZoneOffset = ZoneOffset.UTC

fun nowUTC(): OffsetDateTime = OffsetDateTime.now(defaultZoneOffset)
