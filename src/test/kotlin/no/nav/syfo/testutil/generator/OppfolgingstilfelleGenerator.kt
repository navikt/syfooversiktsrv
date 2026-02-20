package no.nav.syfo.testutil.generator

import no.nav.syfo.domain.Oppfolgingstilfelle
import no.nav.syfo.domain.PersonOppfolgingstilfelleVirksomhet
import no.nav.syfo.domain.Virksomhetsnummer
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

fun generateOppfolgingstilfelle(
    start: LocalDate,
    end: LocalDate,
    antallSykedager: Int?,
    virksomhetList: List<PersonOppfolgingstilfelleVirksomhet> = emptyList(),
) = Oppfolgingstilfelle(
    updatedAt = OffsetDateTime.now(),
    generatedAt = OffsetDateTime.now(),
    oppfolgingstilfelleStart = start,
    oppfolgingstilfelleEnd = end,
    antallSykedager = antallSykedager,
    oppfolgingstilfelleBitReferanseInntruffet = OffsetDateTime.now(),
    oppfolgingstilfelleBitReferanseUuid = UUID.randomUUID(),
    virksomhetList = virksomhetList,
)

fun generateOppfolgingstilfelleVirksomhet(virksomhetsnummer: Virksomhetsnummer, virksomhetsnavn: String?) =
    PersonOppfolgingstilfelleVirksomhet(
        uuid = UUID.randomUUID(),
        createdAt = OffsetDateTime.now(),
        virksomhetsnummer = virksomhetsnummer,
        virksomhetsnavn = virksomhetsnavn,
    )
