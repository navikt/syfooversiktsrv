package no.nav.syfo.personstatus.application

import no.nav.syfo.personstatus.domain.PersonIdent
import no.nav.syfo.personstatus.domain.PersonOversiktStatus
import no.nav.syfo.personstatus.domain.VeilederBrukerKnytning

interface IPersonOversiktStatusRepository {

    fun updateArbeidsuforhetvurderingStatus(personident: PersonIdent, isAktivVurdering: Boolean): Result<Int>

    fun upsertSenOppfolgingKandidat(personident: PersonIdent, isAktivKandidat: Boolean): Result<Int>

    fun upsertAktivitetskravAktivStatus(personident: PersonIdent, isAktivVurdering: Boolean): Result<Int>

    fun upsertManglendeMedvirkningStatus(personident: PersonIdent, isAktivVurdering: Boolean): Result<Int>

    fun getPersonOversiktStatus(personident: PersonIdent): PersonOversiktStatus?

    fun createPersonOversiktStatusIfMissing(personident: PersonIdent): Boolean

    fun lagreVeilederForBruker(
        veilederBrukerKnytning: VeilederBrukerKnytning,
        tildeltAv: String,
    )
}
