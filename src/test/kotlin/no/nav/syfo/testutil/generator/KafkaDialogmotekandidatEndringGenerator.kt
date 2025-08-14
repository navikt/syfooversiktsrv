package no.nav.syfo.testutil.generator

import no.nav.syfo.personstatus.infrastructure.kafka.dialogmotekandidat.DIALOGMOTEKANDIDAT_TOPIC
import no.nav.syfo.personstatus.infrastructure.kafka.dialogmotekandidat.KafkaDialogmotekandidatEndring
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.TopicPartition
import java.time.OffsetDateTime
import java.util.UUID

fun generateKafkaDialogmotekandidatEndringStoppunkt(
    personIdent: String,
    createdAt: OffsetDateTime,
) = KafkaDialogmotekandidatEndring(
    uuid = UUID.randomUUID().toString(),
    createdAt = createdAt,
    personIdentNumber = personIdent,
    kandidat = true,
    arsak = "STOPPUNKT"
)

fun generateKafkaDialogmotekandidatEndringUnntak(
    personIdent: String,
    createdAt: OffsetDateTime,
) = KafkaDialogmotekandidatEndring(
    uuid = UUID.randomUUID().toString(),
    createdAt = createdAt,
    personIdentNumber = personIdent,
    kandidat = false,
    arsak = "UNNTAK"
)

fun dialogmotekandidatEndringTopicPartition() = TopicPartition(
    DIALOGMOTEKANDIDAT_TOPIC,
    0
)

fun dialogmotekandidatEndringConsumerRecord(
    kafkaDialogmotekandidatEndring: KafkaDialogmotekandidatEndring,
) = ConsumerRecord(
    DIALOGMOTEKANDIDAT_TOPIC,
    0,
    1,
    "key1",
    kafkaDialogmotekandidatEndring
)
