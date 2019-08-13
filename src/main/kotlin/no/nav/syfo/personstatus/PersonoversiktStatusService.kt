package no.nav.syfo.personstatus

import kotlinx.coroutines.*
import no.nav.syfo.db.DatabaseInterface
import no.nav.syfo.personstatus.domain.PersonOversiktStatus

class PersonoversiktStatusService(
        private val database: DatabaseInterface,
        private val veilederConsumer: VeilederConsumer
) {

    suspend fun hentPersonoversiktStatusTilknyttetEnhet(enhet: String, token: String) = database.hentPersonerTilknyttetEnhet(enhet)
            .filter { it.motebehovUbehandlet == true || it.moteplanleggerUbehandlet == true }
            .map { personOversiktStatus -> leggTilVeilederNavn(personOversiktStatus, token) }


    suspend fun leggTilVeilederNavn(personOversiktStatus: PersonOversiktStatus, token: String): PersonOversiktStatus {
            if (personOversiktStatus.veilederIdent != null) {
                personOversiktStatus.veileder = veilederConsumer.hentNavn(personOversiktStatus.enhet, personOversiktStatus.veilederIdent, token)
            }
        return personOversiktStatus
    }

}
