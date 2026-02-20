package no.nav.syfo.application.oppfolgingsoppgave

import no.nav.syfo.domain.PersonIdent

interface IOppfolgingsoppgaveClient {

    suspend fun getActiveOppfolgingsoppgaver(
        callId: String,
        token: String,
        personidenter: List<PersonIdent>,
    ): OppfolgingsoppgaverLatestVersionResponseDTO?
}
