package no.nav.syfo.auth

import java.util.*


private val veilederIdenterMedTilgang = Arrays.asList("123")


object TilgangsSjekk {
    fun harTilgang(veilederTokenPayload: VeilederTokenPayload): Boolean = veilederIdenterMedTilgang.contains(veilederTokenPayload.navIdent)
}
