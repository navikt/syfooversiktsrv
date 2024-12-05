package no.nav.syfo.personstatus.infrastructure.cronjob.reaper

import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.personstatus.infrastructure.cronjob.Cronjob
import no.nav.syfo.personstatus.infrastructure.cronjob.CronjobResult
import org.slf4j.LoggerFactory

class ReaperCronjob(
    private val reaperService: ReaperService,
) : Cronjob {
    private val log = LoggerFactory.getLogger(ReaperCronjob::class.java)

    override val initialDelayMinutes: Long = 2
    override val intervalDelayMinutes: Long = 60L * 24

    override suspend fun run() {
        runJob()
    }

    fun runJob(): CronjobResult {
        log.info("Run ReaperCronjob")
        val result = CronjobResult()

        /*
        Formålet med denne cronjob'en er å nulle ut veilederknytning for personer som ikke lengre er under
        oppfølging. Problemet som løses er at vi ikke ønsker at personer som blir syke på nytt tar med seg
        en veilederknytning fra et tidligere oppfølgingstilfelle (veilederen kan ha sluttet eller flyttet
        til en annen enhet, eller personen kan ha flyttet i mellomtiden).
        Desember 2024: Endrer slik at veilederknytningen nulles når det har gått 2 måneder (fra 3) siden siste
        oppfølgingstilfelle-end og minst 2 måneder siden siste oppdatering.
         */
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
