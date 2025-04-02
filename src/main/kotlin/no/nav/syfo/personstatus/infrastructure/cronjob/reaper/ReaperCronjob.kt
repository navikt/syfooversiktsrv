package no.nav.syfo.personstatus.infrastructure.cronjob.reaper

import kotlinx.coroutines.runBlocking
import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.personstatus.application.PersonoversiktStatusService
import no.nav.syfo.personstatus.domain.PersonIdent
import no.nav.syfo.personstatus.infrastructure.clients.behandlendeenhet.BehandlendeEnhetClient
import no.nav.syfo.personstatus.infrastructure.cronjob.Cronjob
import no.nav.syfo.personstatus.infrastructure.cronjob.CronjobResult
import org.slf4j.LoggerFactory
import java.util.*


/**
* Formålet med denne cronjob'en er å nulle ut veilederknytning for personer som ikke lengre er under
* oppfølging. Problemet som løses er at vi ikke ønsker at personer som blir syke på nytt tar med seg
* en veilederknytning fra et tidligere oppfølgingstilfelle (veilederen kan ha sluttet eller flyttet
* til en annen enhet, eller personen kan ha flyttet i mellomtiden).
* *Desember 2024:* Endrer slik at veilederknytningen nulles når det har gått 2 måneder (fra 3) siden siste
* oppfølgingstilfelle-end og minst 2 måneder siden siste oppdatering.
* *Mars 2025:* Utvider til også å nulle oppfølgingsenhet
*/
class ReaperCronjob(
    private val personOversiktStatusService: PersonoversiktStatusService,
    private val behandlendeEnhetClient: BehandlendeEnhetClient,
) : Cronjob {
    private val log = LoggerFactory.getLogger(ReaperCronjob::class.java)

    override val initialDelayMinutes: Long = 2
    override val intervalDelayMinutes: Long = 60L * 24

    override suspend fun run() {
        runJob()
    }

    suspend fun runJob(): CronjobResult {
        log.info("Run ReaperCronjob")
        val result = CronjobResult()

        personOversiktStatusService.getPersonerWithVeilederTildelingAndOldOppfolgingstilfelle()
            .forEach {
                try {
                    personOversiktStatusService.removeTildeltVeileder(PersonIdent(it.fnr))
                    behandlendeEnhetClient.unsetOppfolgingsenhet(
                        callId = UUID.randomUUID().toString(),
                        personIdent = PersonIdent(it.fnr),
                    )
                    result.updated++
                    COUNT_CRONJOB_REAPER_UPDATE.increment()
                } catch (ex: Exception) {
                    log.error(
                        "Exception caught while attempting to remove tildelt veileder and oppfolgingsenhet",
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
