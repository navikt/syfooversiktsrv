package no.nav.syfo.util

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class StringUtilTest {

    @Nested
    inner class CapitalizeNames {
        @Test
        fun `Should capitalize navn`() {
            "OLA".lowerCapitalize() shouldBeEqualTo "Ola"
        }

        @Test
        fun `Should capitalize navn with space`() {
            "JAN OLA".lowerCapitalize() shouldBeEqualTo "Jan Ola"
        }

        @Test
        fun `Should capitalize navn with dashes`() {
            "JAN-OLA".lowerCapitalize() shouldBeEqualTo "Jan-Ola"
        }

        @Test
        fun `Should capitalize navn with dashes and spaces`() {
            "JAN-OLA JON OLA-JAN JON JAN-O-JUL".lowerCapitalize() shouldBeEqualTo "Jan-Ola Jon Ola-Jan Jon Jan-O-Jul"
        }
    }
}
