package no.nav.syfo.domain

import no.nav.syfo.testutil.generator.generateOppfolgingstilfelle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class OppfolgingstilfelleTest {

    @Test
    fun `Calculates varighet based on start and end`() {
        val oppfolgingstilfelle = generateOppfolgingstilfelle(
            start = LocalDate.now().minusDays(19),
            end = LocalDate.now(),
            antallSykedager = null,
        )

        assertEquals(2, oppfolgingstilfelle.varighetUker())
    }

    @Test
    fun `Calculates varighet based on start and now when end is in the future`() {
        val oppfolgingstilfelle = generateOppfolgingstilfelle(
            start = LocalDate.now().minusDays(19),
            end = LocalDate.now().plusDays(1),
            antallSykedager = null,
        )

        assertEquals(2, oppfolgingstilfelle.varighetUker())
    }

    @Test
    fun `Calculates varighet based on start and antallSykedager`() {
        val oppfolgingstilfelle = generateOppfolgingstilfelle(
            start = LocalDate.now().minusDays(19),
            end = LocalDate.now(),
            antallSykedager = 10,
        )

        assertEquals(1, oppfolgingstilfelle.varighetUker())
    }

    @Test
    fun `Calculates varighet based on start and antallSykedager when end is in the future`() {
        val oppfolgingstilfelle = generateOppfolgingstilfelle(
            start = LocalDate.now().minusDays(19),
            end = LocalDate.now().plusDays(100),
            antallSykedager = 110,
        )

        assertEquals(1, oppfolgingstilfelle.varighetUker())
    }

    @Test
    fun `Sets varighet to 0 when missing sykedager in the future`() {
        val oppfolgingstilfelle = generateOppfolgingstilfelle(
            start = LocalDate.now().minusDays(19),
            end = LocalDate.now().plusDays(100),
            antallSykedager = 10,
        )

        assertEquals(0, oppfolgingstilfelle.varighetUker())
    }

    @Test
    fun `Sets varighet to 0 when start is in the future`() {
        val oppfolgingstilfelle = generateOppfolgingstilfelle(
            start = LocalDate.now().plusDays(9),
            end = LocalDate.now().plusDays(20),
            antallSykedager = null,
        )

        assertEquals(0, oppfolgingstilfelle.varighetUker())
    }
}
