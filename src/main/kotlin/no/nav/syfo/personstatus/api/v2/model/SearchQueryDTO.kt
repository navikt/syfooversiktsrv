package no.nav.syfo.personstatus.api.v2.model

import no.nav.syfo.personstatus.domain.Initials
import no.nav.syfo.personstatus.domain.SearchQuery

data class SearchQueryDTO(
    val initials: String,
) {
    fun toSearchQuery(): SearchQuery = SearchQuery(
        Initials(
            initials
        )
    )
}
