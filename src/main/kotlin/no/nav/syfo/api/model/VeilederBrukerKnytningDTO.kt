package no.nav.syfo.api.model

import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.domain.PersonOversiktStatus

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
