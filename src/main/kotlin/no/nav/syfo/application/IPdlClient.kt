package no.nav.syfo.application

import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.infrastructure.clients.pdl.model.PdlHentPersonBolkData
import no.nav.syfo.infrastructure.clients.pdl.model.PdlIdenter
import no.nav.syfo.infrastructure.clients.pdl.model.PdlPerson

interface IPdlClient {
    suspend fun getPersons(callId: String? = null, personidenter: List<PersonIdent>): PdlHentPersonBolkData?
    suspend fun getPerson(personIdent: PersonIdent): Result<PdlPerson>
    suspend fun hentIdenter(nyPersonIdent: String, callId: String? = null): PdlIdenter?
    suspend fun getPdlPersonIdentNumberNavnMap(callId: String, personIdentList: List<PersonIdent>): Map<String, String>
}
