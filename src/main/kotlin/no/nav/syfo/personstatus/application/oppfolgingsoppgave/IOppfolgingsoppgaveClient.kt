package no.nav.syfo.personstatus.application.oppfolgingsoppgave

import no.nav.syfo.personstatus.domain.PersonIdent

interface IOppfolgingsoppgaveClient {

    suspend fun getActiveOppfolgingsoppgaver(
        callId: String,
        token: String,
        personidenter: List<PersonIdent>,
    ): OppfolgingsoppgaverLatestVersionResponseDTO?
}
