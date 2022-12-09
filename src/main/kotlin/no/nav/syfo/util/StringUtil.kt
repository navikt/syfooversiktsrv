package no.nav.syfo.util

import java.util.*

fun String.lowerCapitalize() =
    this.split(" ").joinToString(" ") { name ->
        val nameWithDash = name.split("-")
        if (nameWithDash.size > 1) {
            nameWithDash.joinToString("-") { it.capitalizeName() }
        } else {
            name.capitalizeName()
        }
    }

private fun String.capitalizeName() =
    this.lowercase(Locale.getDefault()).replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
    }
