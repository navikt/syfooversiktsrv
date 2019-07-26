package no.nav.syfo.tilgangskontroll

import no.nav.syfo.auth.getVeilederTokenPayload
import no.nav.syfo.isPreProd
import no.nav.syfo.util.allToUpperCase

val veilederIdenterMedTilgang = arrayListOf(
        "Z991598",
        "T152136",
        "S139136",
        "H103404",
        "L126710",
        "W126199",
        "F125384",
        "H146483",
        "K105407",
        "M152421",
        "H152380",
        "H148938",
        "H131999",
        "V134908",
        "H139248",
        "H149140",
        "B144544",
        "S113562",
        "V111088",
        "F140344",
        "M106428",
        "N149853", // - Lisa
        "R144807"  // - Tor Halle
).allToUpperCase()

val utviklereMedTilgangIPreProd = arrayListOf(
        "Z990197" // - John Martin
).allToUpperCase()

class MidlertidigTilgangsSjekk(private var tilgangListe: List<String> = veilederIdenterMedTilgang) {

    init {
        if (isPreProd()) {
            tilgangListe = tilgangListe.plus(utviklereMedTilgangIPreProd)
        }
    }

    fun harTilgang(token: String): Boolean = getVeilederTokenPayload(token).let { tilgangListe.contains(it.navIdent.toUpperCase()) }
}
