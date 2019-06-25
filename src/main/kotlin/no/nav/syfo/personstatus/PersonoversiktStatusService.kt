package no.nav.syfo.personstatus

import no.nav.syfo.db.DatabaseInterface
import no.nav.syfo.personstatus.domain.PersonOversiktStatus

class PersonoversiktStatusService(private val database: DatabaseInterface) {

    fun hentPersonoversiktStatusTilknyttetEnhet(enhet: String) = database.hentPersonerTilknyttetEnhet(enhet).map { PersonOversiktStatus(it.veilederIdent, it.fnr, it.enhet) }
}
