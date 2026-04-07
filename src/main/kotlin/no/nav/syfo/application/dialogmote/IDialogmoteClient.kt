package no.nav.syfo.application.dialogmote

import no.nav.syfo.domain.PersonIdent

interface IDialogmoteClient {
    suspend fun getDialogmoteAvvent(
        token: String,
        personidenter: List<PersonIdent>,
    ): List<DialogmoteAvventDTO>?
}
