package no.nav.syfo.personstatus.application

import no.nav.syfo.oppfolgingstilfelle.domain.Oppfolgingstilfelle
import no.nav.syfo.oppfolgingstilfelle.kafka.COUNT_KAFKA_CONSUMER_OPPFOLGINGSTILFELLE_PERSON_TILDELT_VEILEDER_NOT_FOUND_OR_NOT_ENABLED
import no.nav.syfo.personstatus.domain.PersonIdent
import no.nav.syfo.personstatus.domain.PersonOversiktStatus
import no.nav.syfo.personstatus.infrastructure.clients.pdl.model.fodselsdato
import no.nav.syfo.personstatus.infrastructure.clients.pdl.model.fullName
import no.nav.syfo.personstatus.infrastructure.clients.veileder.VeilederClient
import org.slf4j.LoggerFactory
import java.time.temporal.ChronoUnit
import java.util.*

class OppfolgingstilfelleService(
    private val pdlClient: IPdlClient,
    private val personOversiktStatusRepository: IPersonOversiktStatusRepository,
    private val veilederClient: VeilederClient,
) {
    suspend fun upsertPersonOversiktStatus(personStatus: PersonOversiktStatus, newPersonOppfolgingsTilfelle: Oppfolgingstilfelle) {
        val existingPerson: PersonOversiktStatus? = personOversiktStatusRepository.getPersonOversiktStatus(PersonIdent(personStatus.fnr))
        if (existingPerson == null) {
            createPersonWithNameAndFodselsdato(personStatus)
        } else {
            val shouldUpdateOppfolgingstilfelle =
                shouldUpdatePersonOppfolgingstilfelle(
                    newOppfolgingstilfelle = newPersonOppfolgingsTilfelle,
                    existingOppfolgingstilfelle = existingPerson.latestOppfolgingstilfelle
                )
            if (shouldUpdateOppfolgingstilfelle) {
                personOversiktStatusRepository.updatePersonOversiktStatusOppfolgingstilfelle(
                    personstatus = existingPerson,
                    oppfolgingstilfelle = newPersonOppfolgingsTilfelle,
                )
            }
            if (existingPerson.veilederIdent != null) {
                checkTildeltVeileder(existingPerson.veilederIdent)
            }
        }
    }

    private suspend fun createPersonWithNameAndFodselsdato(personStatus: PersonOversiktStatus) {
        pdlClient.getPerson(PersonIdent(personStatus.fnr))
            .map {
                val editedPersonStatues =
                    personStatus.updatePersonDetails(navn = it.fullName(), fodselsdato = it.fodselsdato())
                personOversiktStatusRepository.createPersonOversiktStatus(editedPersonStatues)
            }.onFailure { throwable ->
                log.error("Failed to get person from PDL: ${throwable.message}. Creating person without name and fodselsdato")
                personOversiktStatusRepository.createPersonOversiktStatus(personStatus)
            }
    }

    private fun shouldUpdatePersonOppfolgingstilfelle(
        newOppfolgingstilfelle: Oppfolgingstilfelle,
        existingOppfolgingstilfelle: Oppfolgingstilfelle?,
    ): Boolean {
        val isSameOppfolgingstilfelleBit =
            newOppfolgingstilfelle.oppfolgingstilfelleBitReferanseUuid == existingOppfolgingstilfelle?.oppfolgingstilfelleBitReferanseUuid
        return if (isSameOppfolgingstilfelleBit) {
            val isNewTilfelleGeneratedAfterExisting =
                existingOppfolgingstilfelle.generatedAt.let { newOppfolgingstilfelle.generatedAt.isAfter(it) }
            isNewTilfelleGeneratedAfterExisting
        } else {
            existingOppfolgingstilfelle?.oppfolgingstilfelleBitReferanseInntruffet?.let {
                val existingInntruffet = it.truncatedTo(ChronoUnit.MILLIS)
                val newInntruffet = newOppfolgingstilfelle.oppfolgingstilfelleBitReferanseInntruffet.truncatedTo(ChronoUnit.MILLIS)
                if (newInntruffet == existingInntruffet) {
                    existingOppfolgingstilfelle.generatedAt.let { newOppfolgingstilfelle.generatedAt.isAfter(it) }
                } else {
                    newInntruffet.isAfter(existingInntruffet)
                }
            } ?: true
        }
    }

    private suspend fun checkTildeltVeileder(veilederIdent: String) {
        veilederClient.getVeileder(
            callId = UUID.randomUUID().toString(),
            veilederIdent = veilederIdent,
        ).fold(
            onSuccess = { veileder ->
                if (veileder == null || !veileder.enabled) {
                    log.warn("Tildelt veileder $veilederIdent not found or not enabled")
                    COUNT_KAFKA_CONSUMER_OPPFOLGINGSTILFELLE_PERSON_TILDELT_VEILEDER_NOT_FOUND_OR_NOT_ENABLED.increment()
                }
            },
            onFailure = {
                log.error("Failed to get tildelt veileder from syfoveileder: ${it.message}")
            }
        )
    }

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)
    }
}
