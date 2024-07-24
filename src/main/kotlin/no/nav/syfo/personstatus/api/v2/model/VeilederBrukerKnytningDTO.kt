package no.nav.syfo.personstatus.api.v2.model

import no.nav.syfo.personstatus.domain.PersonIdent
import no.nav.syfo.personstatus.domain.PersonOversiktStatus

data class VeilederBrukerKnytningDTO(
    val personident: PersonIdent,
    val tildeltVeilederident: String?,
    val tildeltEnhet: String?,
) {

    companion object {
        fun fromPersonstatus(personstatus: PersonOversiktStatus): VeilederBrukerKnytningDTO =
            VeilederBrukerKnytningDTO(
                personident = PersonIdent(personstatus.fnr),
                tildeltVeilederident = personstatus.veilederIdent,
                tildeltEnhet = personstatus.enhet,
            )
    }
}
