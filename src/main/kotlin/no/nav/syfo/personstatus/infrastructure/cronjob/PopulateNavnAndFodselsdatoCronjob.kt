package no.nav.syfo.personstatus.infrastructure.cronjob

import no.nav.syfo.personstatus.PersonoversiktStatusService
import org.slf4j.LoggerFactory

class PopulateNavnAndFodselsdatoCronjob(
    private val personoversiktStatusService: PersonoversiktStatusService,
) : Cronjob {
    override val initialDelayMinutes: Long = 10
    override val intervalDelayMinutes: Long = 15

    override suspend fun run() {
        log.info("Run PopulateNavnAndFodselsdatoCronjob")
        personoversiktStatusService.updateNavnOrFodselsdatoWhereMissing(updateLimit = UPDATE_LIMIT)
    }

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)
        private const val UPDATE_LIMIT = 1000
    }
}
