package no.nav.syfo.personstatus.application

import no.nav.syfo.domain.PersonIdent

interface IPersonOversiktStatusRepository {

    fun updateArbeidsuforhetVurderingStatus(personIdent: PersonIdent, isAktivVurdering: Boolean)
}
