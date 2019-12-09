package no.nav.syfo.tilgangskontroll

import io.ktor.util.InternalAPI
import org.amshove.kluent.*
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

@InternalAPI
class MidlertidigTilgangsKontrollSpek : Spek({
    val tilgangerTestPath = "./src/test/resources/tilganger.json"

    describe("MidlertidigTilgangsSjekk") {
        val identWithAccess = "Z123456"
        val identWithoutAccess = "Z234567"

        val tilgangsSjekk = MidlertidigTilgangsSjekk(tilgangerTestPath)
        it("Should read $tilgangerTestPath") {
            tilgangsSjekk.tilgangListe.size shouldEqualTo 1
            tilgangsSjekk.tilgangListe shouldContain identWithAccess
        }

        it("Should grant access to a whitelisted Ident") {
            val harTilgang = tilgangsSjekk.harTilgang(identWithAccess)
            harTilgang shouldEqual true
        }

        it("Should not grant access to a non-whitelisted Ident") {
            val harTilgang = tilgangsSjekk.harTilgang(identWithoutAccess)
            harTilgang shouldEqual false
        }
    }
})
