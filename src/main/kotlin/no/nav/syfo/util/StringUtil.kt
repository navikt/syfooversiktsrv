package no.nav.syfo.util

import java.util.*

fun String.lowerCapitalize() =
    this.lowercase(Locale.getDefault()).replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
    }
