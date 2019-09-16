package no.nav.syfo.tilgangskontroll

import io.ktor.util.InternalAPI
import org.amshove.kluent.*
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

@InternalAPI
class MidlertidigTilgangsKontrollSpek : Spek({

    describe("MidlertidigTilgangsSjekk") {
        val tilgangsSjekk = MidlertidigTilgangsSjekk(javaClass.classLoader.getResource("tilganger.json").path)
        it("Skal lese tilganger") {
            tilgangsSjekk.tilgangListe.size shouldEqualTo 1
            tilgangsSjekk.tilgangListe shouldContain "Z123456"
        }

        it("Skal gi tilgang til en ident i listen") {
            val harTilgang = tilgangsSjekk.harTilgang("Z123456")
            harTilgang shouldEqual true
        }

        it("Skal ikke gi tilgang til ident som ikke finnes i listen") {
            val harTilgang = tilgangsSjekk.harTilgang("Z123455")
            harTilgang shouldEqual false
        }
    }
})
