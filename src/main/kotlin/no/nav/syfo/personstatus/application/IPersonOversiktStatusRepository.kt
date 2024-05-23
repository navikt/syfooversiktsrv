package no.nav.syfo.personstatus.application

import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.personstatus.domain.PersonOversiktStatus

interface IPersonOversiktStatusRepository {

    fun updateArbeidsuforhetVurderingStatus(personident: PersonIdent, isAktivVurdering: Boolean): Result<Int>

    fun getPersonOversiktStatus(personident: PersonIdent): PersonOversiktStatus?
}
