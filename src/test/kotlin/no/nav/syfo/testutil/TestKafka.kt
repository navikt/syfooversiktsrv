package no.nav.syfo.testutil

import no.nav.common.KafkaEnvironment
import no.nav.syfo.identhendelse.kafka.PDL_AKTOR_TOPIC

fun testKafka(
    autoStart: Boolean = false,
    topicNames: List<String> = listOf(
        PDL_AKTOR_TOPIC,
    )
) = KafkaEnvironment(
    autoStart = autoStart,
    topicNames = topicNames
)
