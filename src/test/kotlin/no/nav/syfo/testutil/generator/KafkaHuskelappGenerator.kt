package no.nav.syfo.testutil.generator

import no.nav.syfo.trengeroppfolging.kafka.HUSKELAPP_TOPIC
import no.nav.syfo.trengeroppfolging.kafka.KafkaHuskelapp
import no.nav.syfo.testutil.UserConstants
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.TopicPartition
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

fun generateKafkaHuskelapp(
    uuid: UUID = UUID.randomUUID(),
    personIdent: String = UserConstants.ARBEIDSTAKER_FNR,
    isActive: Boolean = true,
    frist: LocalDate?,
) = KafkaHuskelapp(
    uuid = uuid,
    personIdent = personIdent,
    createdAt = OffsetDateTime.now(),
    veilederIdent = UserConstants.VEILEDER_ID,
    isActive = isActive,
    tekst = "En huskelapp",
    frist = frist,
    oppfolgingsgrunner = emptyList(),
    updatedAt = OffsetDateTime.now(),
)

fun huskelappTopicPartition() = TopicPartition(
    HUSKELAPP_TOPIC,
    0
)

fun huskelappConsumerRecord(
    kafkaHuskelapp: KafkaHuskelapp,
) = ConsumerRecord(
    HUSKELAPP_TOPIC,
    0,
    1,
    "key1",
    kafkaHuskelapp
)
