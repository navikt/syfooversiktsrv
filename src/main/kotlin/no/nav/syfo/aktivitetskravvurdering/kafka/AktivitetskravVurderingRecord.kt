package no.nav.syfo.aktivitetskravvurdering.kafka

import no.nav.syfo.aktivitetskravvurdering.domain.Aktivitetskrav
import no.nav.syfo.aktivitetskravvurdering.domain.AktivitetskravStatus
import no.nav.syfo.personstatus.domain.PersonIdent
import java.time.LocalDate
import java.time.OffsetDateTime

data class AktivitetskravVurderingRecord(
    val uuid: String,
    val personIdent: String,
    val createdAt: OffsetDateTime,
    val status: String,
    val isFinal: Boolean,
    val stoppunktAt: LocalDate,
    val beskrivelse: String?,
    val sistVurdert: OffsetDateTime?,
    val frist: LocalDate?,
)

fun AktivitetskravVurderingRecord.toAktivitetskrav() = Aktivitetskrav(
    personIdent = PersonIdent(personIdent),
    status = AktivitetskravStatus.valueOf(this.status),
    stoppunkt = this.stoppunktAt,
    vurderingFrist = this.frist,
)
