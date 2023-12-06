package no.nav.syfo.testutil.generator

import no.nav.syfo.oppfolgingstilfelle.domain.Oppfolgingstilfelle
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

fun generateOppfolgingstilfelle(
    start: LocalDate = LocalDate.now().minusDays(1),
    end: LocalDate = LocalDate.now(),
    antallSykedager: Int? = null
) = Oppfolgingstilfelle(
    updatedAt = OffsetDateTime.now(),
    generatedAt = OffsetDateTime.now(),
    oppfolgingstilfelleStart = start,
    oppfolgingstilfelleEnd = end,
    antallSykedager = antallSykedager,
    oppfolgingstilfelleBitReferanseInntruffet = OffsetDateTime.now(),
    oppfolgingstilfelleBitReferanseUuid = UUID.randomUUID(),
    virksomhetList = emptyList(),
)
