package no.nav.syfo.personstatus

import no.nav.syfo.personstatus.application.IPersonOversiktStatusRepository
import no.nav.syfo.personstatus.domain.PersonIdent
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
        veilederBrukerKnytninger.map {
            val personident = PersonIdent(it.fnr)
            personoversiktStatusRepository.createPersonOversiktStatusIfMissing(personident)
            val personoversiktStatus = personoversiktStatusRepository.getPersonOversiktStatus(personident)
            if (personoversiktStatus!!.enhet == null) {
                personBehandlendeEnhetService.updateBehandlendeEnhet(personident)
            }
            personoversiktStatusRepository.lagreVeilederForBruker(
                veilederBrukerKnytning = it,
                tildeltAv = tildeltAv,
            )
        }
    }
}
