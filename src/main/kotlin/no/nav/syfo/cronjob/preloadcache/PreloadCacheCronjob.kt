package no.nav.syfo.cronjob.preloadcache

import kotlinx.coroutines.runBlocking
import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.client.veiledertilgang.VeilederTilgangskontrollClient
import no.nav.syfo.cronjob.Cronjob
import no.nav.syfo.cronjob.CronjobResult
import no.nav.syfo.personstatus.db.hentUbehandledePersonerTilknyttetEnhet
import org.slf4j.LoggerFactory
import java.time.*

class PreloadCacheCronjob(
    private val database: DatabaseInterface,
    private val tilgangskontrollClient: VeilederTilgangskontrollClient,
) : Cronjob {
    private val log = LoggerFactory.getLogger(PreloadCacheCronjob::class.java)
    private val runAtHour = 6
    private val chunkSize = 50

    override val initialDelayMinutes: Long = calculateInitialDelay()
    override val intervalDelayMinutes: Long = 60L * 24

    override suspend fun run() {
        runJob()
    }

    fun runJob(): CronjobResult {
        log.info("Run PreloadCacheCronjob")
        val result = CronjobResult()

        database.getEnheter()
            .forEach { enhetNr ->
                try {
                    val personer = database.hentUbehandledePersonerTilknyttetEnhet(enhetNr)
                    log.info("Caching ${personer.size} for enhet $enhetNr")
                    personer.chunked(chunkSize).forEach { subList ->
                        if (subList.isNotEmpty()) {
                            runBlocking {
                                val response = tilgangskontrollClient.preloadCache(
                                    subList.map { personOversiktStatus -> personOversiktStatus.fnr }
                                )
                                if (!response) {
                                    log.warn("Caching for $enhetNr failed")
                                    result.failed += subList.size
                                } else {
                                    result.updated += subList.size
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

    private fun calculateInitialDelay(): Long {
        val now = LocalDateTime.now()
        val nowDate = LocalDate.now()
        val nextTimeToRun = LocalDateTime.of(
            if (now.hour < runAtHour) nowDate else nowDate.plusDays(1),
            LocalTime.of(runAtHour, 0),
        )
        val initialDelay = Duration.between(LocalDateTime.now(), nextTimeToRun).toMinutes()
        log.info("PreloadCacheCronJob will run in $initialDelay minutes at $nextTimeToRun")
        return initialDelay
    }
}
