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
import java.time.OffsetDateTime

class IdenthendelseService(
    private val database: DatabaseInterface,
    private val pdlClient: IPdlClient,
    private val personOversiktStatusRepository: IPersonOversiktStatusRepository,
    private val dryRun: Boolean = false,
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
                    if (dryRun) {
                        logDryRun(activeIdent, personOversiktStatusWithOldIdent)
                        return
                    }
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

    private fun logDryRun(
        activeIdent: PersonIdent,
        personOversiktStatusWithOldIdent: List<PersonOversiktStatus>,
    ) {
        val oldUuids = personOversiktStatusWithOldIdent.map { it.uuid }
        val pdlIsUpdated = try {
            checkThatPdlIsUpdated(activeIdent)
            true
        } catch (ex: Exception) {
            log.warn("Identhendelse dryrun: PDL check failed for uuid=$oldUuids: ${ex.message}")
            false
        }
        val personstatusActiveIdent = personOversiktStatusRepository.getPersonOversiktStatus(activeIdent)
        if (personstatusActiveIdent != null) {
            val oldStatus = personOversiktStatusWithOldIdent.first()
            val copyVeileder = personstatusActiveIdent.veilederIdent == null && oldStatus.veilederIdent != null
            val fieldsToCopy = fieldsToCopy(oldStatus, personstatusActiveIdent)
            log.info(
                "Identhendelse dryrun: would perform DELETE_OLD(uuid=${oldStatus.uuid}, keep=${personstatusActiveIdent.uuid}, " +
                    "copyVeileder=$copyVeileder, copyOtherFields=${fieldsToCopy.isNotEmpty()}), pdlIsUpdated=$pdlIsUpdated"
            )
            fieldsToCopy.forEach { field ->
                log.info("Identhendelse dryrun: would copy $field for ${personstatusActiveIdent.uuid}.")
            }
        } else {
            log.info("Identhendelse dryrun: would perform UPDATE_FNR(uuid=$oldUuids), pdlIsUpdated=$pdlIsUpdated")
        }
    }

    private fun fieldsToCopy(oldStatus: PersonOversiktStatus, activeStatus: PersonOversiktStatus): List<String> = listOfNotNull(
        "trenger_oppfolging=true".takeIf {
            oldStatus.isAktivOppfolgingsoppgave && !activeStatus.isAktivOppfolgingsoppgave
        },
        "is_aktiv_aktivitetskrav_vurdering=true".takeIf {
            oldStatus.isAktivAktivitetskravvurdering && !activeStatus.isAktivAktivitetskravvurdering
        },
        "is_aktiv_arbeidsuforhet_vurdering=true".takeIf {
            oldStatus.isAktivArbeidsuforhetvurdering && !activeStatus.isAktivArbeidsuforhetvurdering
        },
        "is_aktiv_manglende_medvirkning_vurdering=true".takeIf {
            oldStatus.isAktivManglendeMedvirkningVurdering && !activeStatus.isAktivManglendeMedvirkningVurdering
        },
        "dialogmotekandidat=${oldStatus.dialogmotekandidat} (generated_at ${oldStatus.dialogmotekandidatGeneratedAt}, was ${activeStatus.dialogmotekandidat} generated_at ${activeStatus.dialogmotekandidatGeneratedAt})".takeIf {
            oldStatus.dialogmotekandidatGeneratedAt.isNewerThan(activeStatus.dialogmotekandidatGeneratedAt)
        },
        "motestatus=${oldStatus.motestatus} (generated_at ${oldStatus.motestatusGeneratedAt}, was ${activeStatus.motestatus} generated_at ${activeStatus.motestatusGeneratedAt})".takeIf {
            oldStatus.motestatusGeneratedAt.isNewerThan(activeStatus.motestatusGeneratedAt)
        },
    )

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

private fun OffsetDateTime?.isNewerThan(other: OffsetDateTime?): Boolean =
    this != null && (other == null || this.isAfter(other))
