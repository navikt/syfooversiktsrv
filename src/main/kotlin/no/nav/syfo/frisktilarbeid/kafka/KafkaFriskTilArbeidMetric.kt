package no.nav.syfo.frisktilarbeid.kafka

import io.micrometer.core.instrument.Counter
import no.nav.syfo.metric.METRICS_NS
import no.nav.syfo.metric.METRICS_REGISTRY

const val KAFKA_CONSUMER_FRISKTILARBEID_BASE = "${METRICS_NS}_kafka_consumer_frisktilarbeid"
const val KAFKA_CONSUMER_FRISKTILARBEID_READ = "${KAFKA_CONSUMER_FRISKTILARBEID_BASE}_read"
const val KAFKA_CONSUMER_FRISKTILARBEID_TOMBSTONE = "${KAFKA_CONSUMER_FRISKTILARBEID_BASE}_tombstone"
const val KAFKA_CONSUMER_FRISKTILARBEID_CREATE = "${KAFKA_CONSUMER_FRISKTILARBEID_BASE}_create"
const val KAFKA_CONSUMER_FRISKTILARBEID_UPDATE = "${KAFKA_CONSUMER_FRISKTILARBEID_BASE}_update"

val COUNT_KAFKA_CONSUMER_FRISKTILARBEID_READ: Counter =
    Counter.builder(KAFKA_CONSUMER_FRISKTILARBEID_READ)
        .description("Counts the number of reads from topic - isfrisktilarbeid-vedtak-status")
        .register(METRICS_REGISTRY)
val COUNT_KAFKA_CONSUMER_FRISKTILARBEID_TOMBSTONE: Counter =
    Counter.builder(KAFKA_CONSUMER_FRISKTILARBEID_TOMBSTONE)
        .description("Counts the number of tombstones received from topic - isfrisktilarbeid-vedtak-status")
        .register(METRICS_REGISTRY)
val COUNT_KAFKA_CONSUMER_FRISKTILARBEID_CREATED_PERSONOVERSIKT_STATUS: Counter =
    Counter.builder(KAFKA_CONSUMER_FRISKTILARBEID_CREATE)
        .description("Counts the number of personoversikt-status created from reading topic - isfrisktilarbeid-vedtak-status")
        .register(METRICS_REGISTRY)
val COUNT_KAFKA_CONSUMER_FRISKTILARBEID_UPDATED_PERSONOVERSIKT_STATUS: Counter =
    Counter.builder(KAFKA_CONSUMER_FRISKTILARBEID_UPDATE)
        .description("Counts the number of personoversikt-status updated from reading topic - isfrisktilarbeid-vedtak-status")
        .register(METRICS_REGISTRY)
