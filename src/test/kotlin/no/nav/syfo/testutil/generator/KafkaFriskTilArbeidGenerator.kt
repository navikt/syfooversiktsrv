package no.nav.syfo.testutil.generator

import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.frisktilarbeid.kafka.FRISK_TIL_ARBEID_VEDTAK_TOPIC
import no.nav.syfo.frisktilarbeid.kafka.VedtakFattetRecord
import no.nav.syfo.testutil.UserConstants
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.TopicPartition
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

fun generateKafkaFriskTilArbeidVedtak(
    personIdent: PersonIdent,
    createdAt: OffsetDateTime,
    fom: LocalDate,
) = VedtakFattetRecord(
    uuid = UUID.randomUUID(),
    createdAt = createdAt,
    veilederident = UserConstants.VEILEDER_ID,
    personident = personIdent,
    begrunnelse = "",
    fom = fom,
    tom = fom.plusDays(90),
)

fun friskTilArbeidTopicPartition() = TopicPartition(
    FRISK_TIL_ARBEID_VEDTAK_TOPIC,
    0
)

fun friskTilArbeidConsumerRecord(
    vedtakFattetRecord: VedtakFattetRecord
) = ConsumerRecord(
    FRISK_TIL_ARBEID_VEDTAK_TOPIC,
    0,
    1,
    "key1",
    vedtakFattetRecord,
)
