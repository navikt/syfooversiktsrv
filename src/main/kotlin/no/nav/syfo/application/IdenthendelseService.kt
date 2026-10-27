package no.nav.syfo.application

import kotlinx.coroutines.runBlocking
import no.nav.syfo.infrastructure.kafka.identhendelse.KafkaIdenthendelseDTO
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.domain.PersonOversiktStatus
import no.nav.syfo.infrastructure.COUNT_KAFKA_CONSUMER_PDL_AKTOR_UPDATES
import no.nav.syfo.infrastructure.database.DatabaseInterface
import no.nav.syfo.infrastructure.database.queries.queryDeletePersonOversiktStatusFnr
import no.nav.syfo.infrastructure.database.queries.updatePersonOversiktStatusFnr
import no.nav.syfo.infrastructure.database.queries.updatePersonOversiktStatusVeileder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class IdenthendelseService(
    private val database: DatabaseInterface,
    private val pdlClient: IPdlClient,
    private val personOversiktStatusRepository: IPersonOversiktStatusRepository,
) {

    private val log: Logger = LoggerFactory.getLogger(IdenthendelseService::class.java)

    fun handleIdenthendelse(identhendelse: KafkaIdenthendelseDTO) {
        if (identhendelse.folkeregisterIdenter.size > 1) {
            val activeIdent = identhendelse.getActivePersonident()
            if (activeIdent != null) {
                val inactiveIdenter = identhendelse.getInactivePersonidenter()
                val personOversiktStatusWithOldIdent = inactiveIdenter.mapNotNull { personident ->
                    personOversiktStatusRepository.getPersonOversiktStatus(PersonIdent(personident.value))
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
        personOversiktStatusWithOldIdent: List<PersonOversiktStatus>,
    ): Int {
        var updatedRows = 0
        val personstatusActiveIdent = personOversiktStatusRepository.getPersonOversiktStatus(PersonIdent(activeIdent.value))
        if (personstatusActiveIdent != null) {
            val oldStatus = personOversiktStatusWithOldIdent.first()
            if (personstatusActiveIdent.veilederIdent == null && oldStatus.veilederIdent != null) {
                updatedRows += database.updatePersonOversiktStatusVeileder(oldStatus.veilederIdent, activeIdent)
            }
            // Kopier noen felter om det trengs, og lag logginnslag slik at vi kan ettergå det
            // Forventer ikke at disse tingene er noe som treffer mange av duplikatforekomstene
            // Kan slette dette når vi er ferdig med rerun, for dette er en mindre aktuell problemstilling når vi er ajour med identhendelsene.
            if (oldStatus.isAktivOppfolgingsoppgave && !personstatusActiveIdent.isAktivOppfolgingsoppgave) {
                personOversiktStatusRepository.updateOppfolgingsoppgave(
                    personIdent = activeIdent,
                    isActive = true,
                ).getOrThrow()
                log.info("Identhendelse: trenger_oppfolging set to true for ${personstatusActiveIdent.uuid}.")
            }
            if (oldStatus.isAktivAktivitetskravvurdering && !personstatusActiveIdent.isAktivAktivitetskravvurdering) {
                personOversiktStatusRepository.upsertAktivitetskravAktivStatus(
                    personident = activeIdent,
                    isAktivVurdering = true,
                ).getOrThrow()
                log.info("Identhendelse: is_aktiv_aktivitetskrav_vurdering set to true for ${personstatusActiveIdent.uuid}.")
            }
            if (oldStatus.isAktivArbeidsuforhetvurdering && !personstatusActiveIdent.isAktivArbeidsuforhetvurdering) {
                personOversiktStatusRepository.updateArbeidsuforhetvurderingStatus(
                    personident = activeIdent,
                    isAktivVurdering = true,
                ).getOrThrow()
                log.info("Identhendelse: is_aktiv_arbeidsuforhet_vurdering set to true for ${personstatusActiveIdent.uuid}.")
            }
            if (oldStatus.isAktivManglendeMedvirkningVurdering && !personstatusActiveIdent.isAktivManglendeMedvirkningVurdering) {
                personOversiktStatusRepository.upsertManglendeMedvirkningStatus(
                    personident = activeIdent,
                    isAktivVurdering = true,
                ).getOrThrow()
                log.info("Identhendelse: is_aktiv_manglende_medvirkning_vurdering set to true for ${personstatusActiveIdent.uuid}.")
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
