package no.nav.syfo.application.oppfolgingsoppgave

import no.nav.syfo.application.IPersonOversiktStatusRepository
import no.nav.syfo.application.PersonBehandlendeEnhetService
import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.domain.PersonOversiktStatus
import no.nav.syfo.infrastructure.kafka.oppfolgingsoppgave.COUNT_KAFKA_CONSUMER_TRENGER_OPPFOLGING_READ
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.jvm.java

class OppfolgingsoppgaveService(
    private val personBehandlendeEnhetService: PersonBehandlendeEnhetService,
    private val personOversiktStatusRepository: IPersonOversiktStatusRepository,
) {

    fun createOrUpdatePersonOversiktStatus(personIdent: PersonIdent, isActiveOppfolgingsoppgave: Boolean) {
        val existingPersonOversiktStatus =
            personOversiktStatusRepository.getPersonOversiktStatus(personIdent)
        if (existingPersonOversiktStatus == null) {
            val personoversiktStatus = PersonOversiktStatus(
                fnr = personIdent.value,
                isAktivOppfolgingsoppgave = isActiveOppfolgingsoppgave,
            )
            personOversiktStatusRepository.createPersonOversiktStatus(
                personOversiktStatus = personoversiktStatus,
            )
        } else {
            personOversiktStatusRepository.updateOppfolgingsoppgave(
                personIdent = personIdent,
                isActive = isActiveOppfolgingsoppgave,
            )
        }

        COUNT_KAFKA_CONSUMER_TRENGER_OPPFOLGING_READ.increment()
    }

    suspend fun updateBehandlendeEnhet(personIdent: PersonIdent) {
        try {
            personBehandlendeEnhetService.updateBehandlendeEnhet(personIdent = personIdent)
            log.info("Updated Behandlende Enhet of person after received active oppfolgingsoppgave")
        } catch (ex: Exception) {
            log.error(
                "Exception caught while attempting to update Behandlende Enhet of person after received active oppfolgingsoppgave",
                ex
            )
        }
    }

    companion object {
        val log: Logger = LoggerFactory.getLogger(this::class.java)
    }
}
