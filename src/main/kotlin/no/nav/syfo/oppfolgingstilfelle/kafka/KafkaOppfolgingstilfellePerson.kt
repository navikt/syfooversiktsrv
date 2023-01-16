package no.nav.syfo.oppfolgingstilfelle.kafka

import no.nav.syfo.domain.Virksomhetsnummer
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
    val virksomhetsnummerList: List<String>,
)

fun KafkaOppfolgingstilfellePerson.toPersonOversiktStatus(
    latestKafkaOppfolgingstilfelle: KafkaOppfolgingstilfelle,
) = PersonOversiktStatus(
    veilederIdent = null,
    fnr = this.personIdentNumber,
    navn = null,
    enhet = null,
    motebehovUbehandlet = null,
    oppfolgingsplanLPSBistandUbehandlet = null,
    dialogmotesvarUbehandlet = false,
    dialogmotekandidat = null,
    dialogmotekandidatGeneratedAt = null,
    motestatus = null,
    motestatusGeneratedAt = null,
    latestOppfolgingstilfelle = this.toPersonOppfolgingstilfelle(
        latestKafkaOppfolgingstilfelle = latestKafkaOppfolgingstilfelle,
    ),
    aktivitetskrav = null,
    aktivitetskravStoppunkt = null,
    aktivitetskravSistVurdert = null,
)

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
)
