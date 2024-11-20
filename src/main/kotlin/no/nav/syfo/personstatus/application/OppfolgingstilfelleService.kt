package no.nav.syfo.personstatus.application

import no.nav.syfo.oppfolgingstilfelle.domain.Oppfolgingstilfelle
import no.nav.syfo.personstatus.domain.PersonIdent
import no.nav.syfo.personstatus.domain.PersonOversiktStatus
import java.time.temporal.ChronoUnit

class OppfolgingstilfelleService(
    private val personOversiktStatusRepository: IPersonOversiktStatusRepository,
) {

    fun upsertPersonOversiktStatus(personStatus: PersonOversiktStatus, newPersonOppfolgingsTilfelle: Oppfolgingstilfelle) {
        val existingPerson: PersonOversiktStatus? = personOversiktStatusRepository.getPersonOversiktStatus(PersonIdent(personStatus.fnr))
        if (existingPerson == null) {
            personOversiktStatusRepository.createPersonOversiktStatus(personStatus)
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
}
