package no.nav.syfo.personstatus.application

import no.nav.syfo.oppfolgingstilfelle.domain.Oppfolgingstilfelle
import no.nav.syfo.personstatus.api.v2.model.VeilederTildelingHistorikkDTO
import no.nav.syfo.personstatus.domain.PersonIdent
import no.nav.syfo.personstatus.domain.PersonOversiktStatus
import no.nav.syfo.personstatus.domain.Search
import no.nav.syfo.personstatus.domain.VeilederBrukerKnytning

interface IPersonOversiktStatusRepository {

    fun updateArbeidsuforhetvurderingStatus(personident: PersonIdent, isAktivVurdering: Boolean): Result<Int>

    fun upsertSenOppfolgingKandidat(personident: PersonIdent, isAktivKandidat: Boolean): Result<Int>

    fun upsertAktivitetskravAktivStatus(personident: PersonIdent, isAktivVurdering: Boolean): Result<Int>

    fun upsertManglendeMedvirkningStatus(personident: PersonIdent, isAktivVurdering: Boolean): Result<Int>

    fun getPersonOversiktStatus(personident: PersonIdent): PersonOversiktStatus?

    fun getPersonstatusesWithoutNavnOrFodselsdato(limit: Int): List<PersonOversiktStatus>

    fun createPersonOversiktStatus(personOversiktStatus: PersonOversiktStatus): PersonOversiktStatus

    fun lagreVeilederForBruker(
        veilederBrukerKnytning: VeilederBrukerKnytning,
        tildeltAv: String,
    )

    fun getVeilederTilknytningHistorikk(personident: PersonIdent): List<VeilederTildelingHistorikkDTO>

    fun getPersonerWithOppgaveAndOldEnhet(): List<PersonIdent>

    fun getPersonerWithVeilederTildelingAndOldOppfolgingstilfelle(): List<PersonOversiktStatus>

    fun removeTildeltVeileder(personIdent: PersonIdent)

    fun removeTildeltEnhet(personIdent: PersonIdent)

    fun updatePersonTildeltEnhetAndRemoveTildeltVeileder(personIdent: PersonIdent, enhetId: String)

    fun updatePersonTildeltEnhetUpdatedAt(personIdent: PersonIdent)

    fun updatePersonstatusesWithNavnAndFodselsdato(personer: List<PersonOversiktStatus>): List<Result<PersonOversiktStatus>>

    fun searchPerson(search: Search): List<PersonOversiktStatus>

    fun updatePersonOversiktStatusOppfolgingstilfelle(
        personstatus: PersonOversiktStatus,
        oppfolgingstilfelle: Oppfolgingstilfelle,
    )

    fun updateOppfolgingsoppgave(personIdent: PersonIdent, isActive: Boolean): Result<Int>
}
