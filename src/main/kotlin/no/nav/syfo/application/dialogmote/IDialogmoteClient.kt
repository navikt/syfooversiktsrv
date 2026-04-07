package no.nav.syfo.application.dialogmote

import no.nav.syfo.domain.PersonIdent

interface IDialogmoteClient {
    suspend fun getDialogmoteAvvent(
        callId: String,
        token: String,
        personidenter: List<PersonIdent>,
    ): List<DialogmoteAvventDTO>?
}
