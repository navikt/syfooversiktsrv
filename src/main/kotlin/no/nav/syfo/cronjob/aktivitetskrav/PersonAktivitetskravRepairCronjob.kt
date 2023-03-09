package no.nav.syfo.cronjob.aktivitetskrav

import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.client.oppfolgingstilfelle.OppfolgingstilfelleClient
import no.nav.syfo.cronjob.Cronjob
import no.nav.syfo.cronjob.CronjobResult
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.personstatus.db.updatePersonOversiktStatusOppfolgingstilfelle
import no.nav.syfo.personstatus.domain.Oppfolgingstilfelle
import no.nav.syfo.util.*
import org.slf4j.LoggerFactory
import java.time.*
import java.util.UUID

class PersonAktivitetskravRepairCronjob(
    private val database: DatabaseInterface,
    private val oppfolgingstilfelleClient: OppfolgingstilfelleClient,
) : Cronjob {

    override val initialDelayMinutes: Long = 2
    override val intervalDelayMinutes: Long = 6000L

    override suspend fun run() {
        runJob()
    }

    suspend fun runJob(): CronjobResult {
        log.info("PersonAktivitetskravRepairCronjob: Repair aktivitetskravkandidater without arbeidsforhold")
        val result = CronjobResult()
        database.getPersonOversiktStatusAktivitetskravList().forEach {
            try {
                val stoppunkt = it.aktivitetskravStoppunkt!!
                val oppfolgingstilfellePerson = oppfolgingstilfelleClient.getOppfolgingstilfellePerson(PersonIdent(it.fnr))
                    ?: throw RuntimeException("PersonAktivitetskravRepairCronjob: Found no oppfolgingstilfelle!")
                val oppfolgingstilfelleDTO = oppfolgingstilfellePerson.oppfolgingstilfelleList.firstOrNull { oppfolgingstilfelle ->
                    oppfolgingstilfelle.start.isBeforeOrEqual(stoppunkt) && stoppunkt.isBeforeOrEqual(oppfolgingstilfelle.end)
                } ?: throw RuntimeException("PersonAktivitetskravRepairCronjob: Found no oppfolgingstilfelle!")

                val oppfolgingstilfelle = Oppfolgingstilfelle(
                    updatedAt = nowUTC(),
                    generatedAt = nowUTC(),
                    oppfolgingstilfelleStart = oppfolgingstilfelleDTO.start,
                    oppfolgingstilfelleEnd = oppfolgingstilfelleDTO.end,
                    oppfolgingstilfelleBitReferanseInntruffet = LocalDateTime.of(oppfolgingstilfelleDTO.start, LocalTime.NOON).atOffset(defaultZoneOffset),
                    oppfolgingstilfelleBitReferanseUuid = UUID.randomUUID(),
                    virksomhetList = emptyList(),
                )

                database.connection.use { connection ->
                    connection.updatePersonOversiktStatusOppfolgingstilfelle(
                        pPersonOversiktStatus = it,
                        oppfolgingstilfelle = oppfolgingstilfelle
                    )
                    connection.commit()
                }
                result.updated++
            } catch (exc: Exception) {
                log.error("PersonAktivitetskravRepairCronjob: Processing for ${it.uuid} failed", exc)
                result.failed++
            }
        }
        log.info(
            "Completed PersonAktivitetskravRepairCronjob with result: {}, {}",
            StructuredArguments.keyValue("failed", result.failed),
            StructuredArguments.keyValue("updated", result.updated),
        )
        return result
    }

    companion object {
        private val log = LoggerFactory.getLogger(PersonAktivitetskravRepairCronjob::class.java)
    }
}
