package no.nav.syfo.application.meroppfolging

import no.nav.syfo.domain.PersonIdent

interface IMeroppfolgingClient {
    suspend fun getSenOppfolgingKandidater(
        callId: String,
        token: String,
        personidenter: List<PersonIdent>,
    ): SenOppfolgingKandidaterResponseDTO?
}
