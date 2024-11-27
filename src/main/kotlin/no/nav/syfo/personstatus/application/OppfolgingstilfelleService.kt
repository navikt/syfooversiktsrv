package no.nav.syfo.personstatus.application

import no.nav.syfo.oppfolgingstilfelle.domain.Oppfolgingstilfelle
import no.nav.syfo.personstatus.domain.PersonIdent
import no.nav.syfo.personstatus.domain.PersonOversiktStatus
import no.nav.syfo.personstatus.infrastructure.clients.pdl.model.fodselsdato
import no.nav.syfo.personstatus.infrastructure.clients.pdl.model.fullName
import org.slf4j.LoggerFactory
import java.time.temporal.ChronoUnit

class OppfolgingstilfelleService(
    private val pdlClient: IPdlClient,
    private val personOversiktStatusRepository: IPersonOversiktStatusRepository,
) {
    suspend fun upsertPersonOversiktStatus(personStatus: PersonOversiktStatus, newPersonOppfolgingsTilfelle: Oppfolgingstilfelle) {
        val existingPerson: PersonOversiktStatus? = personOversiktStatusRepository.getPersonOversiktStatus(PersonIdent(personStatus.fnr))
        if (existingPerson == null) {
            pdlClient.getPerson(PersonIdent(personStatus.fnr))
                .map {
                    val editedPersonStatues =
                        personStatus.updatePersonDetails(navn = it.fullName(), fodselsdato = it.fodselsdato())
                    personOversiktStatusRepository.createPersonOversiktStatus(editedPersonStatues)
                }.onFailure { throwable ->
                    log.error("Failed to get person from PDL: ${throwable.message}. Creating person without name and fodselsdato")
                    personOversiktStatusRepository.createPersonOversiktStatus(personStatus)
                }
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

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)
    }
}
