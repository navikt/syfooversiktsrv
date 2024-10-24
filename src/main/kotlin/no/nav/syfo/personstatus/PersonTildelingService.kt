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
            if (personoversiktStatusRepository.createPersonOversiktStatusIfMissing(PersonIdent(it.fnr))) {
                personBehandlendeEnhetService.updateBehandlendeEnhet(PersonIdent(it.fnr))
            }
            personoversiktStatusRepository.lagreVeilederForBruker(
                veilederBrukerKnytning = it,
                tildeltAv = tildeltAv,
            )
        }
    }
}
