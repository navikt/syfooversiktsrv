package no.nav.syfo.application

import no.nav.syfo.domain.PersonOversiktStatus
import no.nav.syfo.domain.Search
import org.slf4j.LoggerFactory
import kotlin.jvm.java
import kotlin.time.measureTimedValue

class PersonoversiktSearchService(
    private val personoversiktStatusRepository: IPersonOversiktStatusRepository,
) {
    fun searchSykmeldt(search: Search): List<PersonOversiktStatus> {
        val (searchResult, duration) = measureTimedValue {
            personoversiktStatusRepository.searchPerson(search = search)
        }

        log.info("Completed search for sykmeldt in repository, got ${searchResult.size} results in ${duration.inWholeMilliseconds} ms")

        return searchResult
    }

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)
    }
}
