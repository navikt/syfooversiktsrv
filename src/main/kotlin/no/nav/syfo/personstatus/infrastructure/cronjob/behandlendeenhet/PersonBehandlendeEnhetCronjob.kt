package no.nav.syfo.personstatus.infrastructure.cronjob.behandlendeenhet

import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.personstatus.application.PersonBehandlendeEnhetService
import no.nav.syfo.personstatus.infrastructure.cronjob.Cronjob
import no.nav.syfo.personstatus.infrastructure.cronjob.CronjobResult
import org.slf4j.LoggerFactory

class PersonBehandlendeEnhetCronjob(
    private val personBehandlendeEnhetService: PersonBehandlendeEnhetService,
    override val intervalDelayMinutes: Long,
) : Cronjob {

    override val initialDelayMinutes: Long = 2

    override suspend fun run() {
        runJob()
    }

    suspend fun runJob(): CronjobResult {
        log.info("Run PersonBehandlendeEnhetCronjob")
        val result = CronjobResult()

        personBehandlendeEnhetService.getPersonerToCheckForUpdatedEnhet()
            .forEach { personIdentTildeltEnhetPair ->
                try {
                    val (personIdent, tildeltEnhet) = personIdentTildeltEnhetPair
                    personBehandlendeEnhetService.updateBehandlendeEnhet(
                        personIdent = personIdent,
                        tildeltEnhet = tildeltEnhet,
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
