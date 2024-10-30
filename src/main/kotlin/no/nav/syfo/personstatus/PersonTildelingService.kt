package no.nav.syfo.personstatus

import no.nav.syfo.personstatus.application.IPersonOversiktStatusRepository
import no.nav.syfo.personstatus.domain.PersonIdent
import no.nav.syfo.personstatus.domain.PersonOversiktStatus
import no.nav.syfo.personstatus.domain.VeilederBrukerKnytning
import no.nav.syfo.personstatus.infrastructure.cronjob.behandlendeenhet.PersonBehandlendeEnhetService

class PersonTildelingService(
    private val personoversiktStatusRepository: IPersonOversiktStatusRepository,
    private val personBehandlendeEnhetService: PersonBehandlendeEnhetService,
) {
    suspend fun lagreKnytningMellomVeilederOgBruker(
        veilederBrukerKnytninger: List<VeilederBrukerKnytning>,
        tildeltAv: String,
    ) {
        veilederBrukerKnytninger.forEach {
            val personident = PersonIdent(it.fnr)
            val personoversiktStatus = personoversiktStatusRepository.getPersonOversiktStatus(personident)
                ?: PersonOversiktStatus(fnr = it.fnr).also { personOversiktStatus ->
                    personoversiktStatusRepository.createPersonOversiktStatus(personOversiktStatus)
                }

            if (personoversiktStatus.enhet == null) {
                personBehandlendeEnhetService.updateBehandlendeEnhet(personident)
            }
            personoversiktStatusRepository.lagreVeilederForBruker(
                veilederBrukerKnytning = it,
                tildeltAv = tildeltAv,
            )
        }
    }

    fun getVeilederHistorikk(personident: PersonIdent) =
        personoversiktStatusRepository.getVeilederHistorikk(personident)
}
