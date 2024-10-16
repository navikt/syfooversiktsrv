package no.nav.syfo.personstatus.application.meroppfolging

import no.nav.syfo.personstatus.domain.PersonIdent

interface IMeroppfolgingClient {
    suspend fun getSenOppfolgingKandidater(
        callId: String,
        token: String,
        personidenter: List<PersonIdent>,
    ): SenOppfolgingKandidaterResponseDTO?
}
