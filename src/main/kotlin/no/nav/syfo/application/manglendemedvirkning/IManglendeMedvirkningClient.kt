package no.nav.syfo.application.manglendemedvirkning

import no.nav.syfo.domain.PersonIdent

interface IManglendeMedvirkningClient {

    suspend fun getLatestVurderinger(
        callId: String,
        token: String,
        personidenter: List<PersonIdent>,
    ): ManglendeMedvirkningResponseDTO?
}
