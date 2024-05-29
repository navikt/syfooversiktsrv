package no.nav.syfo.personstatus.application.arbeidsuforhet

import no.nav.syfo.domain.PersonIdent

interface IArbeidsuforhetvurderingClient {

    suspend fun getVurdering(
        callId: String,
        token: String,
        personIdent: PersonIdent,
    ): ArbeidsuforhetvurderingDTO?
}
