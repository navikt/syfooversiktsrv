package no.nav.syfo.util

import java.time.*

val defaultZoneOffset: ZoneOffset = ZoneOffset.UTC

fun nowUTC(): OffsetDateTime = OffsetDateTime.now(defaultZoneOffset)

fun OffsetDateTime.toLocalDateOslo(): LocalDate = atZoneSameInstant(ZoneId.of("Europe/Oslo")).toLocalDate()
fun OffsetDateTime.toLocalDateTimeOslo(): LocalDateTime = atZoneSameInstant(ZoneId.of("Europe/Oslo")).toLocalDateTime()

fun LocalDate.isBeforeOrEqual(anotherDate: LocalDate): Boolean = (this == anotherDate || this.isBefore(anotherDate))
