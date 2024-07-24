package no.nav.syfo.oppfolgingstilfelle.domain

import no.nav.syfo.personstatus.domain.Virksomhetsnummer
import no.nav.syfo.personstatus.api.v2.model.PersonOppfolgingstilfelleDTO
import no.nav.syfo.personstatus.api.v2.model.PersonOppfolgingstilfelleVirksomhetDTO
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.*

private const val DAYS_IN_WEEK = 7
private val log = LoggerFactory.getLogger(Oppfolgingstilfelle::class.java)

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
        val currentVarighetDaysBrutto =
            ChronoUnit.DAYS.between(oppfolgingstilfelleStart, minOf(LocalDate.now(), oppfolgingstilfelleEnd)) + 1
        val currentVarighetDays = if (antallSykedager == null) {
            currentVarighetDaysBrutto
        } else {
            val totalVarighetDays = ChronoUnit.DAYS.between(oppfolgingstilfelleStart, oppfolgingstilfelleEnd) + 1
            val ikkeSykedager = totalVarighetDays - antallSykedager
            if (currentVarighetDaysBrutto - ikkeSykedager < 0 && oppfolgingstilfelleStart < LocalDate.now()) {
                log.warn("Calculation of varighetUker is a negative value for tilfellebitReferanseUuid=$oppfolgingstilfelleBitReferanseUuid")
            }
            currentVarighetDaysBrutto - ikkeSykedager
        }
        return maxOf(currentVarighetDays.toInt() / DAYS_IN_WEEK, 0)
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
