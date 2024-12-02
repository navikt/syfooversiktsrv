package no.nav.syfo.personstatus.domain

import java.time.LocalDate

data class SearchQuery(
    val initials: Initials?,
    val birthdate: LocalDate,
)

@JvmInline
value class Initials(val value: String?) {
    init {
        require(value == null || value.length > 1) { "Initials must be null or more than one characters long" }
    }
}
