package no.nav.syfo.personstatus.api.v2.model

import no.nav.syfo.personstatus.domain.Initials
import no.nav.syfo.personstatus.domain.Name
import no.nav.syfo.personstatus.domain.Search
import java.time.LocalDate

data class SearchQueryDTO(
    val initials: String? = null,
    val name: String? = null,
    val birthdate: LocalDate? = null,
) {
    fun toSearchQuery(): Search =
        if (name != null && birthdate != null) {
            Search.ByNameAndDate(
                name = Name(name),
                birthdate = birthdate
            )
        } else if (initials != null && initials.isNotEmpty() && birthdate != null) {
            Search.ByInitialsAndDate(initials = Initials(initials), birthdate = birthdate)
        } else if (name != null) {
            Search.ByName(name = Name(name))
        } else if (birthdate != null) {
            Search.ByDate(birthdate = birthdate)
        } else {
            throw IllegalArgumentException("SearchQueryDTO values did not conform to allowed search rules")
        }
}
