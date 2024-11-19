package no.nav.syfo.personstatus

import no.nav.syfo.personstatus.application.IPersonOversiktStatusRepository
import no.nav.syfo.personstatus.domain.PersonOversiktStatus
import no.nav.syfo.personstatus.domain.SearchQuery
import org.slf4j.LoggerFactory
import kotlin.time.measureTimedValue

class PersonoversiktSearchService(
    private val personoversiktStatusRepository: IPersonOversiktStatusRepository,
) {
    fun searchSykmeldt(searchQuery: SearchQuery): List<PersonOversiktStatus> {
        val (searchResult, duration) = measureTimedValue {
            personoversiktStatusRepository.searchPerson(searchQuery = searchQuery)
        }

        log.info("Completed search for sykmeldt in repository, got ${searchResult.size} results in ${duration.inWholeMilliseconds} ms")

        return searchResult
    }

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)
    }
}
