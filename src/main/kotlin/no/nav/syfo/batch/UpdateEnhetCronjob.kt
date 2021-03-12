package no.nav.syfo.batch

import kotlinx.coroutines.*
import no.nav.syfo.ApplicationState
import no.nav.syfo.batch.enhet.BehandlendeEnhetClient
import no.nav.syfo.batch.leaderelection.PodLeaderCoordinator
import no.nav.syfo.db.DatabaseInterface
import org.slf4j.LoggerFactory
import java.time.*

private val log = LoggerFactory.getLogger("no.nav.syfo.batch")

suspend fun updateEnhetCronjob(
    databaseInterface: DatabaseInterface,
    behandlendeEnhetClient: BehandlendeEnhetClient
): UpdateEnhetResultat {
    val resultat = UpdateEnhetResultat()

    log.info("CRONJOB-TRACE: Kjører oversikt update enhet cronjob")

    val personerSomKanskjeSkalOppdateres = databaseInterface.getPersonToUpdateEnhet()
    log.info("CRONJOB-TRACE: Oppdaterer personer anntal=${personerSomKanskjeSkalOppdateres.size}")
    personerSomKanskjeSkalOppdateres.forEachIndexed { index, pPersonOversiktStatus ->
        try {
            val newBehandlendeEnhet = behandlendeEnhetClient.getEnhet(
                fnr = pPersonOversiktStatus.fnr,
                callId = ""
            )
            val currentEnhetId = pPersonOversiktStatus.enhet
            if (newBehandlendeEnhet != null && newBehandlendeEnhet.enhetId != currentEnhetId) {
                log.info("CRONJOB-TRACE: Oppdaterer person sin enhet new=${newBehandlendeEnhet.enhetId}, old=$currentEnhetId")
//                databaseInterface.oppdaterPersonEnhetBatch(
//                    enhetId = newBehandlendeEnhet.enhetId,
//                    pPersonOversiktStatus.fnr
//                )
            }
            resultat.oppdatert++
            if (personerSomKanskjeSkalOppdateres.size % 100 == 0) {
                log.info("CRONJOB-TRACE: Progress $index / ${personerSomKanskjeSkalOppdateres.size}")
            }
            log.info("CRONJOB-TRACE: Oppdatert person sin enhet ${pPersonOversiktStatus.id}")
        } catch (e: Exception) {
            resultat.feilet++
            log.error("CRONJOB-TRACE: Feil ved oppdatering av person sin enhet${pPersonOversiktStatus.enhet}", e)
        }
    }
    return resultat
}

data class UpdateEnhetResultat(
    var oppdatert: Int = 0,
    var feilet: Int = 0
)

class UpdateEnhetCronjob(
    private val applicationState: ApplicationState,
    private val databaseInterface: DatabaseInterface,
    private val podLeaderCoordinator: PodLeaderCoordinator,
    private val behandlendeEnhetClient: BehandlendeEnhetClient
) {
    suspend fun start() = coroutineScope {
        val (initialDelay, interval) = hentKjøretider()
        log.info("CRONJOB-TRACE: Schedulerer UpdateEnhetCronjob start: $initialDelay ms, interval: $interval ms")
        delay(initialDelay)

        while (applicationState.initialized) {
            val job = launch { run() }
            delay(interval)
            if (job.isActive) {
                log.warn("CRONJOB-TRACE: UpdateEnhetCronjob er ikke ferdig, venter til den er ferdig")
                job.join()
            }
        }
        log.info("CRONJOB-TRACE: Avslutter UpdateEnhetCronjob")
    }

    private suspend fun run() {
        try {
            if (podLeaderCoordinator.isLeader()) {
                val resultat = updateEnhetCronjob(
                    databaseInterface = databaseInterface,
                    behandlendeEnhetClient = behandlendeEnhetClient
                )
                log.debug("CRONJOB-TRACE: resultat antallFeilet=${resultat.feilet} oppdatert=${resultat.oppdatert}")
            } else {
                log.debug("CRONJOB-TRACE: Jeg er ikke leder")
            }
        } catch (ex: Exception) {
            log.error("CRONJOB-TRACE: Feil i VedtakCronjob, kjøres på nytt neste gang", ex)
        }
    }
}

private fun hentKjøretider(): Pair<Long, Long> {
    // TODO: Set date and time to run cronjob-batch
    val osloTz = ZoneId.of("Europe/Oslo")
    val now = ZonedDateTime.now(osloTz)

    val nesteTime = now.plusHours(1).toInstant().toEpochMilli()
    val enUke = Duration.ofDays(7).toMillis()
    return Pair(nesteTime, enUke)
}

private fun ZonedDateTime.next(atTime: LocalTime): Long {
    return if (this.toLocalTime().isAfter(atTime)) {
        this.plusDays(1).withHour(atTime.hour).withMinute(atTime.minute).withSecond(atTime.second).toInstant().toEpochMilli()
    } else {
        this.withHour(atTime.hour).withMinute(atTime.minute).withSecond(atTime.second).toInstant().toEpochMilli()
    }
}
