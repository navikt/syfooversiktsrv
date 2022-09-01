package no.nav.syfo.personstatus

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.personstatus.db.hentBrukereTilknyttetVeileder
import no.nav.syfo.personstatus.db.lagreBrukerKnytningPaEnhet
import no.nav.syfo.personstatus.domain.VeilederBrukerKnytning

class PersonTildelingService(private val database: DatabaseInterface) {

    fun hentBrukertilknytningerPaVeileder(veilederIdent: String) = database.hentBrukereTilknyttetVeileder(veilederIdent).map { VeilederBrukerKnytning(it.veilederIdent, it.fnr, it.enhet) }

    fun lagreKnytningMellomVeilederOgBruker(veilederBrukerKnytninger: List<VeilederBrukerKnytning>) = veilederBrukerKnytninger.map { database.lagreBrukerKnytningPaEnhet(it) }
}
