package no.nav.syfo.identhendelse

import kotlinx.coroutines.runBlocking
import no.nav.syfo.personstatus.infrastructure.database.DatabaseInterface
import no.nav.syfo.client.pdl.PdlClient
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.identhendelse.database.queryDeletePersonOversiktStatusFnr
import no.nav.syfo.identhendelse.database.updatePersonOversiktStatusFnr
import no.nav.syfo.identhendelse.database.updatePersonOversiktStatusVeileder
import no.nav.syfo.identhendelse.kafka.KafkaIdenthendelseDTO
import no.nav.syfo.metric.COUNT_KAFKA_CONSUMER_PDL_AKTOR_UPDATES
import no.nav.syfo.personstatus.db.getPersonOversiktStatusList
import no.nav.syfo.personstatus.domain.PPersonOversiktStatus
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class IdenthendelseService(
    private val database: DatabaseInterface,
    private val pdlClient: PdlClient,
) {

    private val log: Logger = LoggerFactory.getLogger(IdenthendelseService::class.java)

    fun handleIdenthendelse(identhendelse: KafkaIdenthendelseDTO) {
        if (identhendelse.folkeregisterIdenter.size > 1) {
            val activeIdent = identhendelse.getActivePersonident()
            if (activeIdent != null) {
                val inactiveIdenter = identhendelse.getInactivePersonidenter()
                val personOversiktStatusWithOldIdent = inactiveIdenter.flatMap { personident ->
                    database.getPersonOversiktStatusList(personident.value)
                }

                if (personOversiktStatusWithOldIdent.isNotEmpty()) {
                    checkThatPdlIsUpdated(activeIdent)
                    val numberOfUpdatedIdenter =
                        updateOrOverrideAndDeletePersonOversiktStatus(activeIdent, personOversiktStatusWithOldIdent)
                    if (numberOfUpdatedIdenter > 0) {
                        log.info("Identhendelse: Updated $numberOfUpdatedIdenter personoversiktstatus based on Identhendelse from PDL")
                        COUNT_KAFKA_CONSUMER_PDL_AKTOR_UPDATES.increment(numberOfUpdatedIdenter.toDouble())
                    }
                }
            } else {
                log.warn("Mangler gyldig ident fra PDL")
            }
        }
    }

    // Erfaringer fra andre team tilsier at vi burde dobbeltsjekke at ting har blitt oppdatert i PDL før vi gjør endringer
    private fun checkThatPdlIsUpdated(nyIdent: PersonIdent) {
        runBlocking {
            val pdlIdenter = pdlClient.hentIdenter(nyIdent.value) ?: throw RuntimeException("Fant ingen identer fra PDL")
            if (nyIdent.value != pdlIdenter.aktivIdent && pdlIdenter.identhendelseIsNotHistorisk(nyIdent.value)) {
                throw IllegalStateException("Ny ident er ikke aktiv ident i PDL")
            }
        }
    }

    private fun updateOrOverrideAndDeletePersonOversiktStatus(
        activeIdent: PersonIdent,
        personOversiktStatusWithOldIdent: List<PPersonOversiktStatus>
    ): Int {
        var updatedRows = 0
        val personOversiktStatusActiveIdentList = database.getPersonOversiktStatusList(activeIdent.value)
        if (personOversiktStatusActiveIdentList.isNotEmpty()) {
            val personOversiktStatusActiveIdent = personOversiktStatusActiveIdentList.first()
            val oldStatus = personOversiktStatusWithOldIdent.first()
            if (personOversiktStatusActiveIdent.veilederIdent == null && oldStatus.veilederIdent != null) {
                updatedRows += database.updatePersonOversiktStatusVeileder(oldStatus.veilederIdent, activeIdent)
            }
            database.queryDeletePersonOversiktStatusFnr(oldStatus.fnr)
            log.info("Identhendelse: Deleted entry with an inactive personident from database.")
        } else {
            personOversiktStatusWithOldIdent
                .forEach {
                    val inactiveIdent = PersonIdent(it.fnr)
                    updatedRows += database.updatePersonOversiktStatusFnr(activeIdent, inactiveIdent)
                }
        }
        return updatedRows
    }
}
