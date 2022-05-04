package no.nav.syfo.testutil

import no.nav.common.KafkaEnvironment
import no.nav.syfo.kafka.OVERSIKT_HENDELSE_TOPIC

fun testKafka(
    autoStart: Boolean = false,
    topicNames: List<String> = listOf(
        OVERSIKT_HENDELSE_TOPIC,
    )
) = KafkaEnvironment(
    autoStart = autoStart,
    topicNames = topicNames
)
