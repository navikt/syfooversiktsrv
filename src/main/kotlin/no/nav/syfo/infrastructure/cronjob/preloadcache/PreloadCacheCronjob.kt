package no.nav.syfo.infrastructure.cronjob.preloadcache

import kotlinx.coroutines.runBlocking
import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.application.IPersonOversiktStatusRepository
import no.nav.syfo.domain.filterHasActiveOppgave
import no.nav.syfo.infrastructure.clients.veiledertilgang.VeilederTilgangskontrollClient
import no.nav.syfo.infrastructure.cronjob.Cronjob
import no.nav.syfo.infrastructure.cronjob.CronjobResult
import no.nav.syfo.infrastructure.database.DatabaseInterface
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class PreloadCacheCronjob(
    private val database: DatabaseInterface,
    private val tilgangskontrollClient: VeilederTilgangskontrollClient,
    private val personoversiktStatusRepository: IPersonOversiktStatusRepository,
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
                    val ubehandledePersonerWithActiveOppgave =
                        personoversiktStatusRepository.hentUbehandledePersonerTilknyttetEnhet(enhetNr)
                            .filterHasActiveOppgave()

                    log.info("Caching ${ubehandledePersonerWithActiveOppgave.size} for enhet $enhetNr")
                    ubehandledePersonerWithActiveOppgave.chunked(chunkSize).forEach { personstatuserChunk ->
                        if (personstatuserChunk.isNotEmpty()) {
                            runBlocking {
                                val isResponseOK = tilgangskontrollClient.preloadCache(
                                    personstatuserChunk.map { personOversiktStatus -> personOversiktStatus.fnr }
                                )
                                if (isResponseOK) {
                                    result.updated += personstatuserChunk.size
                                } else {
                                    log.warn("Caching for $enhetNr failed")
                                    result.failed += personstatuserChunk.size
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
