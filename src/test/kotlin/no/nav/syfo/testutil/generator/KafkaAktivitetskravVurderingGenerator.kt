package no.nav.syfo.testutil.generator

import no.nav.syfo.infrastructure.kafka.aktivitetskrav.AKTIVITETSKRAV_VURDERING_TOPIC
import no.nav.syfo.infrastructure.kafka.aktivitetskrav.AktivitetskravVurderingRecord
import no.nav.syfo.testutil.UserConstants
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.TopicPartition
import java.util.*

fun generateKafkaAktivitetskravVurdering(
    isFinal: Boolean,
) = AktivitetskravVurderingRecord(
    uuid = UUID.randomUUID().toString(),
    personIdent = UserConstants.ARBEIDSTAKER_FNR,
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
