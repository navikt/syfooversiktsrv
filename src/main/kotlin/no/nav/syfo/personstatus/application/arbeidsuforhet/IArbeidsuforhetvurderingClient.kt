package no.nav.syfo.personstatus.application.arbeidsuforhet

import no.nav.syfo.personstatus.domain.PersonIdent

interface IArbeidsuforhetvurderingClient {

    suspend fun getLatestVurderinger(
        callId: String,
        token: String,
        personidenter: List<PersonIdent>,
    ): ArbeidsuforhetvurderingerResponseDTO?
}
