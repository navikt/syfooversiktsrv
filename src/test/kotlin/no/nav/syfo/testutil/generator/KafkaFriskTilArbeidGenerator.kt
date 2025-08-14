package no.nav.syfo.testutil.generator

import no.nav.syfo.personstatus.infrastructure.kafka.frisktilarbeid.FRISK_TIL_ARBEID_VEDTAK_TOPIC
import no.nav.syfo.personstatus.infrastructure.kafka.frisktilarbeid.Status
import no.nav.syfo.personstatus.infrastructure.kafka.frisktilarbeid.VedtakStatusRecord
import no.nav.syfo.personstatus.domain.PersonIdent
import no.nav.syfo.testutil.UserConstants
import no.nav.syfo.util.nowUTC
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.TopicPartition
import java.time.LocalDate
import java.util.*

fun generateKafkaFriskTilArbeidVedtak(
    personIdent: PersonIdent,
    fom: LocalDate,
) = VedtakStatusRecord(
    uuid = UUID.randomUUID(),
    personident = personIdent.value,
    begrunnelse = "",
    fom = fom,
    tom = fom.plusDays(90),
    status = Status.FATTET,
    statusAt = nowUTC(),
    statusBy = UserConstants.VEILEDER_ID,
)

fun friskTilArbeidTopicPartition() = TopicPartition(
    FRISK_TIL_ARBEID_VEDTAK_TOPIC,
    0
)

fun friskTilArbeidConsumerRecord(
    vedtakStatusRecord: VedtakStatusRecord,
) = ConsumerRecord(
    FRISK_TIL_ARBEID_VEDTAK_TOPIC,
    0,
    1,
    "key1",
    vedtakStatusRecord,
)
