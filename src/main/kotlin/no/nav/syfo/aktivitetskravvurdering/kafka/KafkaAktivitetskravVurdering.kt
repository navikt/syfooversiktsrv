package no.nav.syfo.aktivitetskravvurdering.kafka

import no.nav.syfo.aktivitetskravvurdering.domain.Aktivitetskrav
import no.nav.syfo.aktivitetskravvurdering.domain.AktivitetskravVurderingStatus
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
    status = AktivitetskravVurderingStatus.valueOf(this.status),
    updatedAt = this.updatedAt,
    stoppunkt = this.stoppunktAt,
)
