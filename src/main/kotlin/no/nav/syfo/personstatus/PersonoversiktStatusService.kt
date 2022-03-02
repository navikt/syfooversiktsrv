package no.nav.syfo.personstatus

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.oversikthendelsetilfelle.domain.toOppfolgingstilfelle
import no.nav.syfo.oversikthendelsetilfelle.hentOppfolgingstilfellerForPerson
import no.nav.syfo.personstatus.domain.*

class PersonoversiktStatusService(
    private val database: DatabaseInterface
) {
    fun hentPersonoversiktStatusTilknyttetEnhet(enhet: String): List<PersonOversiktStatus> {
        val personListe = database.hentUbehandledePersonerTilknyttetEnhet(enhet)
        return personListe.map { pPersonOversikStatus ->
            val pOppfolgingstilfeller = database.hentOppfolgingstilfellerForPerson(pPersonOversikStatus.id)
            val oppfolgingstilfeller: List<Oppfolgingstilfelle> = pOppfolgingstilfeller.map { pOppfolgingstilfelle ->
                pOppfolgingstilfelle.toOppfolgingstilfelle()
            }
            pPersonOversikStatus.toPersonOversiktStatus(oppfolgingstilfeller = oppfolgingstilfeller)
        }.filter { pPersonOversikStatus ->
            pPersonOversikStatus.oppfolgingsplanLPSBistandUbehandlet == true || pPersonOversikStatus.oppfolgingstilfeller.isNotEmpty()
        }
    }
}
