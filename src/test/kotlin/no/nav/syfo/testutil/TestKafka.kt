package no.nav.syfo.testutil

import no.nav.common.KafkaEnvironment
import no.nav.syfo.identhendelse.kafka.PDL_AKTOR_TOPIC
import no.nav.syfo.personstatus.kafka.OVERSIKT_HENDELSE_TOPIC

fun testKafka(
    autoStart: Boolean = false,
    topicNames: List<String> = listOf(
        OVERSIKT_HENDELSE_TOPIC,
        PDL_AKTOR_TOPIC,
    )
) = KafkaEnvironment(
    autoStart = autoStart,
    topicNames = topicNames
)
