package no.nav.syfo.oppfolgingstilfelle.domain

import no.nav.syfo.domain.Virksomhetsnummer
import no.nav.syfo.personstatus.api.v2.PersonOppfolgingstilfelleDTO
import no.nav.syfo.personstatus.api.v2.PersonOppfolgingstilfelleVirksomhetDTO
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.*

private const val DAYS_IN_WEEK = 7

data class Oppfolgingstilfelle(
    val updatedAt: OffsetDateTime,
    val generatedAt: OffsetDateTime,
    val oppfolgingstilfelleStart: LocalDate,
    val oppfolgingstilfelleEnd: LocalDate,
    val antallSykedager: Int?,
    val oppfolgingstilfelleBitReferanseInntruffet: OffsetDateTime,
    val oppfolgingstilfelleBitReferanseUuid: UUID,
    val virksomhetList: List<PersonOppfolgingstilfelleVirksomhet>,
) {
    fun varighetUker(): Int {
        val currentVarighetDaysBrutto = ChronoUnit.DAYS.between(oppfolgingstilfelleStart, minOf(LocalDate.now(), oppfolgingstilfelleEnd)) + 1
        val currentVarighetDays = if (antallSykedager == null) {
            currentVarighetDaysBrutto
        } else {
            val totalVarighetDays = ChronoUnit.DAYS.between(oppfolgingstilfelleStart, oppfolgingstilfelleEnd) + 1
            val ikkeSykedager = totalVarighetDays - antallSykedager
            currentVarighetDaysBrutto - ikkeSykedager
        }
        return currentVarighetDays.toInt() / DAYS_IN_WEEK
    }
}

fun Oppfolgingstilfelle.toPersonOppfolgingstilfelleDTO() =
    PersonOppfolgingstilfelleDTO(
        oppfolgingstilfelleStart = this.oppfolgingstilfelleStart,
        oppfolgingstilfelleEnd = this.oppfolgingstilfelleEnd,
        virksomhetList = this.virksomhetList.toPersonOppfolgingstilfelleVirksomhetDTO(),
        varighetUker = this.varighetUker(),
    )

data class PersonOppfolgingstilfelleVirksomhet(
    val uuid: UUID,
    val createdAt: OffsetDateTime,
    val virksomhetsnummer: Virksomhetsnummer,
    val virksomhetsnavn: String?,
)

fun List<PersonOppfolgingstilfelleVirksomhet>.toPersonOppfolgingstilfelleVirksomhetDTO() =
    this.map { virksomhet ->
        PersonOppfolgingstilfelleVirksomhetDTO(
            virksomhetsnummer = virksomhet.virksomhetsnummer.value,
            virksomhetsnavn = virksomhet.virksomhetsnavn,
        )
    }
