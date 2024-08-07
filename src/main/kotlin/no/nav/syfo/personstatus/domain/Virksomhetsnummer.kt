package no.nav.syfo.personstatus.domain

import java.lang.IllegalArgumentException

data class Virksomhetsnummer(
    val value: String,
) {
    private val nineDigits = Regex("^\\d{9}\$")

    init {
        if (!nineDigits.matches(value)) {
            throw IllegalArgumentException("$value is not a valid Virksomhetsnummer")
        }
    }
}
