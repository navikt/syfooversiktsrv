package no.nav.syfo.personstatus

import no.nav.syfo.personstatus.application.IPersonOversiktStatusRepository
import no.nav.syfo.personstatus.domain.PersonOversiktStatus
import no.nav.syfo.personstatus.domain.SearchQuery
import org.slf4j.LoggerFactory

class PersonoversiktSearchService(
    private val personoversiktStatusRepository: IPersonOversiktStatusRepository,
) {
    fun searchSykmeldt(searchQuery: SearchQuery): List<PersonOversiktStatus> {
        val searchResult = personoversiktStatusRepository.searchPerson(searchQuery = searchQuery)
        log.trace("Completed search for sykmeldt in repository, got ${searchResult.size} results")

        return searchResult
    }

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)
    }
}
