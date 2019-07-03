package no.nav.syfo.personstatus

import no.nav.syfo.db.DatabaseInterface

class PersonoversiktStatusService(private val database: DatabaseInterface) {

    fun hentPersonoversiktStatusTilknyttetEnhet(enhet: String) = database.hentPersonerTilknyttetEnhet(enhet)
}