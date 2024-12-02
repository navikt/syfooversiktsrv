package no.nav.syfo.personstatus.api.v2.model

import no.nav.syfo.personstatus.domain.Initials
import no.nav.syfo.personstatus.domain.SearchQuery
import java.time.LocalDate

data class SearchQueryDTO(
    val initials: String?,
    val birthdate: LocalDate,
) {
    fun toSearchQuery(): SearchQuery = SearchQuery(
        birthdate = birthdate,
        initials = Initials(
            initials
        )
    )
}
