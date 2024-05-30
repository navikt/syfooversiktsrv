package no.nav.syfo.oppfolgingstilfelle.kafka

import no.nav.syfo.domain.Virksomhetsnummer
import no.nav.syfo.oppfolgingstilfelle.domain.Oppfolgingstilfelle
import no.nav.syfo.oppfolgingstilfelle.domain.PersonOppfolgingstilfelleVirksomhet
import no.nav.syfo.personstatus.domain.*
import no.nav.syfo.util.nowUTC
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

data class KafkaOppfolgingstilfellePerson(
    val uuid: String,
    val createdAt: OffsetDateTime,
    val personIdentNumber: String,
    val oppfolgingstilfelleList: List<KafkaOppfolgingstilfelle>,
    val referanseTilfelleBitUuid: String,
    val referanseTilfelleBitInntruffet: OffsetDateTime,
)

data class KafkaOppfolgingstilfelle(
    val arbeidstakerAtTilfelleEnd: Boolean,
    val start: LocalDate,
    val end: LocalDate,
    val antallSykedager: Int?,
    val virksomhetsnummerList: List<String>,
)

// TODO: Hent latest oppfolgingstilfelle fra this
fun KafkaOppfolgingstilfellePerson.toPersonOversiktStatus(
    latestKafkaOppfolgingstilfelle: KafkaOppfolgingstilfelle,
) = PersonOversiktStatus(
    fnr = this.personIdentNumber,
    latestOppfolgingstilfelle = this.toPersonOppfolgingstilfelle(
        latestKafkaOppfolgingstilfelle = latestKafkaOppfolgingstilfelle,
    ),
)

// TODO: Hent latest oppfolgingstilfelle fra this
fun KafkaOppfolgingstilfellePerson.toPersonOppfolgingstilfelle(
    latestKafkaOppfolgingstilfelle: KafkaOppfolgingstilfelle,
) = Oppfolgingstilfelle(
    updatedAt = nowUTC(),
    generatedAt = this.createdAt,
    oppfolgingstilfelleStart = latestKafkaOppfolgingstilfelle.start,
    oppfolgingstilfelleEnd = latestKafkaOppfolgingstilfelle.end,
    oppfolgingstilfelleBitReferanseInntruffet = this.referanseTilfelleBitInntruffet,
    oppfolgingstilfelleBitReferanseUuid = UUID.fromString(this.referanseTilfelleBitUuid),
    virksomhetList = latestKafkaOppfolgingstilfelle.virksomhetsnummerList.map { virksomhetsnummer ->
        PersonOppfolgingstilfelleVirksomhet(
            uuid = UUID.randomUUID(),
            createdAt = nowUTC(),
            virksomhetsnummer = Virksomhetsnummer(virksomhetsnummer),
            virksomhetsnavn = null,
        )
    },
    antallSykedager = latestKafkaOppfolgingstilfelle.antallSykedager,
)
