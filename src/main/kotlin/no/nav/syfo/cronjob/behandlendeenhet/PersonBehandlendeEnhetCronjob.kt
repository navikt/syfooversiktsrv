package no.nav.syfo.cronjob.behandlendeenhet

import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.cronjob.Cronjob
import no.nav.syfo.cronjob.CronjobResult
import org.slf4j.LoggerFactory

class PersonBehandlendeEnhetCronjob(
    private val personBehandlendeEnhetService: PersonBehandlendeEnhetService,
) : Cronjob {

    override val initialDelayMinutes: Long = 2
    override val intervalDelayMinutes: Long = 60L

    override suspend fun run() {
        runJob()
    }

    suspend fun runJob(): CronjobResult {
        val result = CronjobResult()

        personBehandlendeEnhetService.getPersonIdentToUpdateTildeltEnhetList()
            .forEach { personIdent ->
                try {
                    personBehandlendeEnhetService.updateBehandlendeEnhet(
                        personIdent = personIdent,
                    )
                    result.updated++
                    COUNT_CRONJOB_PERSON_BEHANDLENDE_ENHET_UPDATE.increment()
                } catch (ex: Exception) {
                    log.error(
                        "Exception caught while attempting to update Behandlende Enhet of PersonBehandlendeEnhetCronjob",
                        ex
                    )
                    result.failed++
                    COUNT_CRONJOB_PERSON_BEHANDLENDE_ENHET_FAIL.increment()
                }
            }

        log.info(
            "Completed PersonBehandlendeEnhetCronjob with result: {}, {}",
            StructuredArguments.keyValue("failed", result.failed),
            StructuredArguments.keyValue("updated", result.updated),
        )
        return result
    }

    companion object {
        private val log = LoggerFactory.getLogger(PersonBehandlendeEnhetCronjob::class.java)
    }
}
