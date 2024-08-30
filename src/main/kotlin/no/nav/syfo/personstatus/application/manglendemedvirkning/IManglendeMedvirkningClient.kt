package no.nav.syfo.personstatus.application.manglendemedvirkning

import no.nav.syfo.personstatus.domain.PersonIdent

interface IManglendeMedvirkningClient {

    suspend fun getLatestVurderinger(
        callId: String,
        token: String,
        personidenter: List<PersonIdent>,
    ): ManglendeMedvirkningResponseDTO?
}
