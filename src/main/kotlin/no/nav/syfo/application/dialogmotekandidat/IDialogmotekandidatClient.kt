package no.nav.syfo.application.dialogmotekandidat

import no.nav.syfo.domain.PersonIdent

interface IDialogmotekandidatClient {
    suspend fun getDialogmotekandidater(
        callId: String,
        token: String,
        personidenter: List<PersonIdent>,
    ): DialogmotekandidatResponseDTO?
}
