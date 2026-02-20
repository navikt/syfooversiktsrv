package no.nav.syfo.domain

import java.time.LocalDate

sealed class Search {
    data class ByName(val name: Name) : Search()
    data class ByNameAndDate(val name: Name, val birthdate: LocalDate) : Search()
    data class ByDate(val birthdate: LocalDate) : Search()
    data class ByInitialsAndDate(val initials: Initials, val birthdate: LocalDate) : Search()
}

@JvmInline
value class Initials(val value: String) {
    init {
        require(value.length > 1) { "Initials must be more than one characters long" }
    }
}

@JvmInline
value class Name(val value: String) {
    init {
        require(value.split(" ").size > 1) { "Name must consist of two continuous strings or more" }
    }
}
