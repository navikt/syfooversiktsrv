package no.nav.syfo.testutil.generator

import no.nav.syfo.dialogmote.avro.KDialogmoteStatusEndring
import no.nav.syfo.domain.DialogmoteStatusendringType
import no.nav.syfo.infrastructure.kafka.dialogmotestatusendring.DIALOGMOTE_STATUSENDRING_TOPIC
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.TopicPartition
import java.time.OffsetDateTime

fun generateKafkaDialogmoteStatusendring(
    personIdent: String,
    type: DialogmoteStatusendringType,
    endringsTidspunkt: OffsetDateTime,
): KDialogmoteStatusEndring {
    val kDialogmoteStatusEndring = KDialogmoteStatusEndring()
    kDialogmoteStatusEndring.setPersonIdent(personIdent)
    kDialogmoteStatusEndring.setStatusEndringType(type.name)
    kDialogmoteStatusEndring.setStatusEndringTidspunkt(endringsTidspunkt.toInstant())

    return kDialogmoteStatusEndring
}

fun dialogmoteStatusendringTopicPartition() = TopicPartition(
    DIALOGMOTE_STATUSENDRING_TOPIC,
    0
)

fun dialogmoteStatusendringConsumerRecord(
    kafkaDialogmoteStatusendring: KDialogmoteStatusEndring,
) = ConsumerRecord(
    DIALOGMOTE_STATUSENDRING_TOPIC,
    0,
    1,
    "key1",
    kafkaDialogmoteStatusendring
)
