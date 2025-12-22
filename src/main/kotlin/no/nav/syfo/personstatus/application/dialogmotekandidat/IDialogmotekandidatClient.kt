package no.nav.syfo.personstatus.application.dialogmotekandidat

import no.nav.syfo.personstatus.domain.PersonIdent

interface IDialogmotekandidatClient {
    suspend fun getDialogmotekandidater(
        callId: String,
        token: String,
        personidenter: List<PersonIdent>,
    ): DialogmotekandidatResponseDTO?
}
