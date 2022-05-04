package no.nav.syfo.dialogmotekandidat.kafka

import io.micrometer.core.instrument.Counter
import no.nav.syfo.metric.METRICS_NS
import no.nav.syfo.metric.METRICS_REGISTRY

const val KAFKA_CONSUMER_DIALOGMOTEKANDIDAT_BASE = "${METRICS_NS}_kafka_consumer_dialogmotekandidat"
const val KAFKA_CONSUMER_DIALOGMOTEKANDIDAT_READ = "${KAFKA_CONSUMER_DIALOGMOTEKANDIDAT_BASE}_read"
const val KAFKA_CONSUMER_DIALOGMOTEKANDIDAT_TOMBSTONE = "${KAFKA_CONSUMER_DIALOGMOTEKANDIDAT_BASE}_tombstone"

val COUNT_KAFKA_CONSUMER_DIALOGMOTEKANDIDAT_READ: Counter =
    Counter.builder(KAFKA_CONSUMER_DIALOGMOTEKANDIDAT_READ)
        .description("Counts the number of reads from topic - dialogmotekandidat")
        .register(METRICS_REGISTRY)
val COUNT_KAFKA_CONSUMER_DIALOGMOTEKANDIDAT_TOMBSTONE: Counter =
    Counter.builder(KAFKA_CONSUMER_DIALOGMOTEKANDIDAT_TOMBSTONE)
        .description("Counts the number of tombstones received from topic - dialogmotekandidat")
        .register(METRICS_REGISTRY)
