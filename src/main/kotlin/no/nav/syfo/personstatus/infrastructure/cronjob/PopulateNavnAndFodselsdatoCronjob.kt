package no.nav.syfo.personstatus.infrastructure.cronjob

import no.nav.syfo.personstatus.application.PersonoversiktStatusService
import org.slf4j.LoggerFactory

class PopulateNavnAndFodselsdatoCronjob(
    private val personoversiktStatusService: PersonoversiktStatusService,
) : Cronjob {
    override val initialDelayMinutes: Long = 10
    override val intervalDelayMinutes: Long = 15

    override suspend fun run() {
        log.info("Run PopulateNavnAndFodselsdatoCronjob")
        val (success, failure) = personoversiktStatusService.updateNavnOrFodselsdatoWhereMissing(updateLimit = UPDATE_LIMIT)
            .partition { it.isSuccess }
        log.info("PopulateNavnAndFodselsdatoCronjob finished. ${success.size} entries updated. ${failure.size} entries failed to update")
        if (failure.isNotEmpty()) {
            log.error("Failed when running PopulateNavnAndFodselsdatoCronjob.", failure.first().exceptionOrNull())
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)
        private const val UPDATE_LIMIT = 1000
    }
}
