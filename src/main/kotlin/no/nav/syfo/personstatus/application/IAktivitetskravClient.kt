package no.nav.syfo.personstatus.application

import no.nav.syfo.personstatus.domain.PersonIdent

interface IAktivitetskravClient {
    suspend fun getAktivitetskravForPersons(
        callId: String,
        token: String,
        personidenter: List<PersonIdent>,
    ): GetAktivitetskravForPersonsResponseDTO?
}
