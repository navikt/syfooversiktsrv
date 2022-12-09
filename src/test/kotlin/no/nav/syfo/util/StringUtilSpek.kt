package no.nav.syfo.util

import org.amshove.kluent.shouldBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object StringUtilSpek : Spek({

    describe("Test for String Utils") {

        describe("Capitalize names") {
            it("Should capitalize navn") {
                "OLA".lowerCapitalize() shouldBeEqualTo "Ola"
            }
            it("Should capitalize navn with space") {
                "JAN OLA".lowerCapitalize() shouldBeEqualTo "Jan Ola"
            }
            it("Should capitalize navn with dashes") {
                "JAN-OLA".lowerCapitalize() shouldBeEqualTo "Jan-Ola"
            }
            it("Should capitalize navn with dashes and spaces") {
                "JAN-OLA JON OLA-JAN JON JAN-O-JUL".lowerCapitalize() shouldBeEqualTo "Jan-Ola Jon Ola-Jan Jon Jan-O-Jul"
            }
        }
    }
})
