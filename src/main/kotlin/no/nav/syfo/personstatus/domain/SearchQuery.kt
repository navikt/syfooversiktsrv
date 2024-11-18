package no.nav.syfo.personstatus.domain

data class SearchQuery(
    val initials: Initials,
)

@JvmInline
value class Initials(val value: String) {
    init {
        require(value.length > 1) { "Initials must be more than one characters long" }
    }
}
