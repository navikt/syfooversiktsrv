package no.nav.syfo.testutil.generator

import no.nav.syfo.personstatus.application.aktivitetskrav.AktivitetskravStatus
import no.nav.syfo.personstatus.infrastructure.kafka.aktivitetskrav.AKTIVITETSKRAV_VURDERING_TOPIC
import no.nav.syfo.personstatus.infrastructure.kafka.aktivitetskrav.AktivitetskravVurderingRecord
import no.nav.syfo.testutil.UserConstants
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.TopicPartition
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

fun generateKafkaAktivitetskravVurdering(
    status: AktivitetskravStatus,
    frist: LocalDate? = null,
    sistVurdert: OffsetDateTime? = null,
    beskrivelse: String? = null,
    isFinal: Boolean,
) = AktivitetskravVurderingRecord(
    uuid = UUID.randomUUID().toString(),
    personIdent = UserConstants.ARBEIDSTAKER_FNR,
    createdAt = OffsetDateTime.now(),
    status = status.name,
    stoppunktAt = LocalDate.now().plusWeeks(6),
    beskrivelse = beskrivelse,
    sistVurdert = sistVurdert,
    frist = frist,
    isFinal = isFinal
)

fun aktivitetskravVurderingTopicPartition() = TopicPartition(
    AKTIVITETSKRAV_VURDERING_TOPIC,
    0
)

fun aktivitetskravVurderingConsumerRecord(
    aktivitetskravVurderingRecord: AktivitetskravVurderingRecord,
) = ConsumerRecord(
    AKTIVITETSKRAV_VURDERING_TOPIC,
    0,
    1,
    "key1",
    aktivitetskravVurderingRecord
)
