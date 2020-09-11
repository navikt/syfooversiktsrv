package no.nav.syfo.personstatus

import no.nav.syfo.db.DatabaseInterface
import no.nav.syfo.oversikthendelsetilfelle.domain.PPersonOppfolgingstilfelle
import no.nav.syfo.oversikthendelsetilfelle.hentOppfolgingstilfellerForPerson
import no.nav.syfo.personstatus.domain.*

class PersonoversiktStatusService(
    private val database: DatabaseInterface
) {
    fun hentPersonoversiktStatusTilknyttetEnhet(enhet: String): List<PersonOversiktStatus> {
        val personListe = database.hentUbehandledePersonerTilknyttetEnhet(enhet)
        return personListe.map {
            val pOppfolgingstilfeller = database.hentOppfolgingstilfellerForPerson(it.id)
            val oppfolgingstilfeller: List<Oppfolgingstilfelle> = pOppfolgingstilfeller.map { pOppfolgingstilfelle ->
                mapOppfolgingstilfelle(pOppfolgingstilfelle)
            }
            mapPersonOversiktStatus(it, oppfolgingstilfeller)
        }.filter {
            it.oppfolgingstilfeller.isNotEmpty()
        }
    }
}

var mapPersonOversiktStatus = { pPersonOversiktStatus: PPersonOversiktStatus, oppfolgingstilfeller: List<Oppfolgingstilfelle> ->
    PersonOversiktStatus(
        fnr = pPersonOversiktStatus.fnr,
        navn = pPersonOversiktStatus.navn ?: "",
        enhet = pPersonOversiktStatus.enhet,
        veilederIdent = pPersonOversiktStatus.veilederIdent,
        motebehovUbehandlet = pPersonOversiktStatus.motebehovUbehandlet,
        moteplanleggerUbehandlet = pPersonOversiktStatus.moteplanleggerUbehandlet,
        oppfolgingsplanLPSBistandUbehandlet = pPersonOversiktStatus.oppfolgingsplanLPSBistandUbehandlet,
        oppfolgingstilfeller = oppfolgingstilfeller
    )
}

var mapOppfolgingstilfelle = { pPersonOppfolgingstilfelle: PPersonOppfolgingstilfelle ->
    Oppfolgingstilfelle(
        virksomhetsnummer = pPersonOppfolgingstilfelle.virksomhetsnummer,
        fom = pPersonOppfolgingstilfelle.fom,
        tom = pPersonOppfolgingstilfelle.tom,
        virksomhetsnavn = pPersonOppfolgingstilfelle.virksomhetsnavn ?: ""
    )
}
