package no.nav.syfo.cronjob.virksomhetsnavn

import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.client.ereg.EregClient
import no.nav.syfo.cronjob.Cronjob
import no.nav.syfo.cronjob.CronjobResult
import org.slf4j.LoggerFactory

class PersonOppfolgingstilfelleVirksomhetnavnCronjob(
    eregClient: EregClient,
) : Cronjob {

    override val initialDelayMinutes: Long = 2
    override val intervalDelayMinutes: Long = 60L

    override suspend fun run() {
        runJob()
    }

    private fun runJob(): CronjobResult {
        val result = CronjobResult()

        log.info(
            "Completed PersonOppfolgingstilfelleVirksomhetnavnCronjob with result: {}, {}",
            StructuredArguments.keyValue("failed", result.failed),
            StructuredArguments.keyValue("updated", result.updated),
        )
        return result
    }

    companion object {
        private val log = LoggerFactory.getLogger(PersonOppfolgingstilfelleVirksomhetnavnCronjob::class.java)
    }
}
