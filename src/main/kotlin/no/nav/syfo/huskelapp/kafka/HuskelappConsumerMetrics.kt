package no.nav.syfo.huskelapp.kafka

import io.micrometer.core.instrument.Counter
import no.nav.syfo.metric.METRICS_NS
import no.nav.syfo.metric.METRICS_REGISTRY

const val KAFKA_CONSUMER_HUSKELAPP_BASE = "${METRICS_NS}_kafka_consumer_huskelapp"
const val KAFKA_CONSUMER_HUSKELAPP_READ = "${KAFKA_CONSUMER_HUSKELAPP_BASE}_read"
const val KAFKA_CONSUMER_HUSKELAPP_TOMBSTONE = "${KAFKA_CONSUMER_HUSKELAPP_BASE}_tombstone"

val COUNT_KAFKA_CONSUMER_HUSKELAPP_READ: Counter = Counter.builder(KAFKA_CONSUMER_HUSKELAPP_READ)
    .description("Counts the number of reads from topic - huskelapp")
    .register(METRICS_REGISTRY)
val COUNT_KAFKA_CONSUMER_HUSKELAPP_TOMBSTONE: Counter = Counter.builder(KAFKA_CONSUMER_HUSKELAPP_TOMBSTONE)
    .description("Counts the number of tombstones received from topic - huskelapp")
    .register(METRICS_REGISTRY)
