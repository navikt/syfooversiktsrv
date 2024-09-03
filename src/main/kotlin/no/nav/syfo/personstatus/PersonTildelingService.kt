package no.nav.syfo.personstatus

import no.nav.syfo.personstatus.infrastructure.database.DatabaseInterface
import no.nav.syfo.personstatus.db.lagreVeilederForBruker
import no.nav.syfo.personstatus.domain.VeilederBrukerKnytning

class PersonTildelingService(private val database: DatabaseInterface) {

    fun lagreKnytningMellomVeilederOgBruker(veilederBrukerKnytninger: List<VeilederBrukerKnytning>) =
        veilederBrukerKnytninger.map { database.lagreVeilederForBruker(it) }
}
