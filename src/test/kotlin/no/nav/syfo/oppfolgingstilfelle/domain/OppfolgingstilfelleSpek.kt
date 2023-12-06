package no.nav.syfo.oppfolgingstilfelle.domain

import no.nav.syfo.testutil.generator.generateOppfolgingstilfelle
import org.amshove.kluent.shouldBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.LocalDate

class OppfolgingstilfelleSpek : Spek({

    describe("Varighet uker") {
        it("Calculates varighet based on start and end") {
            val oppfolgingstilfelle = generateOppfolgingstilfelle(
                start = LocalDate.now().minusDays(19),
                end = LocalDate.now(),
                antallSykedager = null,
            )

            oppfolgingstilfelle.varighetUker() shouldBeEqualTo 2
        }

        it("Calculates varighet based on start and end when end is in the future") {
            val oppfolgingstilfelle = generateOppfolgingstilfelle(
                start = LocalDate.now().minusDays(19),
                end = LocalDate.now().plusDays(1),
                antallSykedager = null,
            )

            oppfolgingstilfelle.varighetUker() shouldBeEqualTo 2
        }

        it("Calculates varighet based on start and antallSykedager") {
            val oppfolgingstilfelle = generateOppfolgingstilfelle(
                start = LocalDate.now().minusDays(19),
                end = LocalDate.now(),
                antallSykedager = 10,
            )

            oppfolgingstilfelle.varighetUker() shouldBeEqualTo 1
        }
    }
})
