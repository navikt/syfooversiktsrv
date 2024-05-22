package no.nav.syfo.personstatus.application.arbeidsuforhet

import no.nav.syfo.domain.PersonIdent

interface IArbeidsuforhetvurderingClient {

    suspend fun getVurdering(
        callId: String,
        personIdent: PersonIdent,
        token: String
    ): ArbeidsuforhetvurderingDTO
}
