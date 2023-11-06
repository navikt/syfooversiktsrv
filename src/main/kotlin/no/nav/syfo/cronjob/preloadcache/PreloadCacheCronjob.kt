package no.nav.syfo.cronjob.preloadcache

import kotlinx.coroutines.runBlocking
import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.client.veiledertilgang.VeilederTilgangskontrollClient
import no.nav.syfo.cronjob.Cronjob
import no.nav.syfo.cronjob.CronjobResult
import no.nav.syfo.personstatus.db.hentUbehandledePersonerTilknyttetEnhet
import no.nav.syfo.personstatus.domain.hasActiveOppgave
import no.nav.syfo.personstatus.domain.toPersonOversiktStatus
import org.slf4j.LoggerFactory
import java.time.*

class PreloadCacheCronjob(
    private val database: DatabaseInterface,
    private val tilgangskontrollClient: VeilederTilgangskontrollClient,
    private val arenaCutoff: LocalDate,
) : Cronjob {
    private val log = LoggerFactory.getLogger(PreloadCacheCronjob::class.java)
    private val runAtHour = 6
    private val chunkSize = 50

    override val initialDelayMinutes: Long = 10L
    override val intervalDelayMinutes: Long = 30L

    override suspend fun run() {
        runJob()
    }

    fun runJob(): CronjobResult {
        log.info("Run PreloadCacheCronjob")
        val result = CronjobResult()

        database.getEnheter()
            .forEach { enhetNr ->
                try {
                    val personer = database.hentUbehandledePersonerTilknyttetEnhet(enhetNr).map { pPersonOversiktStatus ->
                        pPersonOversiktStatus.toPersonOversiktStatus(emptyList())
                    }.filter { personOversiktStatus ->
                        personOversiktStatus.hasActiveOppgave(arenaCutoff)
                    }

                    log.info("Caching ${personer.size} for enhet $enhetNr")
                    personer.chunked(chunkSize).forEach { subList ->
                        if (subList.isNotEmpty()) {
                            runBlocking {
                                val isResponseOK = tilgangskontrollClient.preloadCache(
                                    subList.map { personOversiktStatus -> personOversiktStatus.fnr }
                                )
                                if (isResponseOK) {
                                    result.updated += subList.size
                                } else {
                                    log.warn("Caching for $enhetNr failed")
                                    result.failed += subList.size
                                }
                            }
                        }
                    }
                } catch (ex: Exception) {
                    log.error(
                        "Exception caught while attempting to preload cache for enhet $enhetNr",
                        ex
                    )
                    result.failed++
                }
            }

        log.info(
            "Completed PreloadCacheCronjob with result: {}, {}",
            StructuredArguments.keyValue("failed", result.failed),
            StructuredArguments.keyValue("updated", result.updated),
        )
        return result
    }

    internal fun calculateInitialDelay() = calculateInitialDelay(LocalDateTime.now())

    internal fun calculateInitialDelay(from: LocalDateTime): Long {
        val nowDate = LocalDate.now()
        val nextTimeToRun = LocalDateTime.of(
            if (from.hour < runAtHour) nowDate else nowDate.plusDays(1),
            LocalTime.of(runAtHour, 0),
        )
        val initialDelay = Duration.between(from, nextTimeToRun).toMinutes()
        log.info("PreloadCacheCronJob will run in $initialDelay minutes at $nextTimeToRun")
        return initialDelay
    }
}
