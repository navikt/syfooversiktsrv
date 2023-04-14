package no.nav.syfo.cronjob.reaper

import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.cronjob.Cronjob
import no.nav.syfo.cronjob.CronjobResult
import org.slf4j.LoggerFactory

class ReaperCronjob(
    private val reaperService: ReaperService,
) : Cronjob {
    private val log = LoggerFactory.getLogger(ReaperCronjob::class.java)

    override val initialDelayMinutes: Long = 2
    override val intervalDelayMinutes: Long = 60L

    override suspend fun run() {
        runJob()
    }

    fun runJob(): CronjobResult {
        log.info("Run ReaperCronjob")
        val result = CronjobResult()

        reaperService.getPersonerForReaper()
            .forEach { uuid ->
                try {
                    reaperService.reap(uuid)
                    result.updated++
                    COUNT_CRONJOB_REAPER_UPDATE.increment()
                } catch (ex: Exception) {
                    log.error(
                        "Exception caught while attempting to reap oppgave with uuid $uuid",
                        ex
                    )
                    result.failed++
                    COUNT_CRONJOB_REAPER_FAIL.increment()
                }
            }

        log.info(
            "Completed ReaperCronjob with result: {}, {}",
            StructuredArguments.keyValue("failed", result.failed),
            StructuredArguments.keyValue("updated", result.updated),
        )
        return result
    }
}
