package no.nav.syfo.cronjob.virksomhetsnavn

import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.cronjob.Cronjob
import no.nav.syfo.cronjob.CronjobResult
import org.slf4j.LoggerFactory

class PersonOppfolgingstilfelleVirksomhetnavnCronjob(
    private val personOppfolgingstilfelleVirksomhetsnavnService: PersonOppfolgingstilfelleVirksomhetsnavnService,
) : Cronjob {

    override val initialDelayMinutes: Long = 2
    override val intervalDelayMinutes: Long = 60L

    override suspend fun run() {
        runJob()
    }

    suspend fun runJob(): CronjobResult {
        val result = CronjobResult()

        personOppfolgingstilfelleVirksomhetsnavnService.getMissingVirksomhetsnavnList()
            .forEach { idVirksomhetsnummerPair ->
                try {
                    personOppfolgingstilfelleVirksomhetsnavnService.updateVirksomhetsnavn(
                        idVirksomhetsnummerPair = idVirksomhetsnummerPair,
                    )
                    result.updated++
                    COUNT_CRONJOB_OPPFOLGINGSTILFELLE_VIRKSOMHETSNAVN_UPDATE.increment()
                } catch (ex: Exception) {
                    log.error(
                        "Exception caught while attempting to update Virksomhetsnavn of PersonOppfolgingstilfelleVirksomhet",
                        ex
                    )
                    result.failed++
                    COUNT_CRONJOB_OPPFOLGINGSTILFELLE_VIRKSOMHETSNAVN_FAIL.increment()
                }
            }

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
