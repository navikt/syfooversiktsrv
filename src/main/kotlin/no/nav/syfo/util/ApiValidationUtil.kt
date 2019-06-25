package no.nav.syfo.util

fun validateEnhet(enhet: String): Boolean {
    return enhet.isNotEmpty() && enhet.matches("\\d{4}$".toRegex())
}
