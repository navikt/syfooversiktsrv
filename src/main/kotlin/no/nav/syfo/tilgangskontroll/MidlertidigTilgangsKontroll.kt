package no.nav.syfo.tilgangskontroll

import no.nav.syfo.auth.getTokenPayload
import java.util.*

val veilederIdenterMedTilgang = Arrays.asList(
        "Z991598","Z992668"
).map { it.toUpperCase() }

class TilgangsSjekk(private val tilgangListe: List<String> = veilederIdenterMedTilgang) {
    fun harTilgang(token: String): Boolean = getTokenPayload(token).let { tilgangListe.contains(it.navIdent.toUpperCase()) }
}
