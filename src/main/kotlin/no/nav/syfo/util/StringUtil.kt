package no.nav.syfo.util

fun List<String>.allToUpperCase(): List<String> {
    return map { it.toUpperCase() }
}
