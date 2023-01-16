package no.nav.syfo.aktivitetskravvurdering.kafka

import no.nav.syfo.aktivitetskravvurdering.domain.Aktivitetskrav
import no.nav.syfo.aktivitetskravvurdering.domain.AktivitetskravStatus
import no.nav.syfo.domain.PersonIdent
import java.time.LocalDate
import java.time.OffsetDateTime

data class KafkaAktivitetskravVurdering(
    val uuid: String,
    val personIdent: String,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
    val status: String,
    val stoppunktAt: LocalDate,
    val beskrivelse: String?,
    val updatedBy: String?,
)

fun KafkaAktivitetskravVurdering.toAktivitetskrav() = Aktivitetskrav(
    personIdent = PersonIdent(personIdent),
    status = AktivitetskravStatus.valueOf(this.status),
    sistVurdert = this.updatedAt,
    stoppunkt = this.stoppunktAt,
)
