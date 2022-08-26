package no.nav.syfo.personoppgavehendelse.kafka

import io.micrometer.core.instrument.Counter
import no.nav.syfo.metric.*

const val KAFKA_CONSUMER_PERSONOPPGAVEHENDELSE_BASE = "${METRICS_NS}_kafka_consumer_personoppgavehendelse"
const val KAFKA_CONSUMER_PERSONOPPGAVEHENDELSE_READ = "${KAFKA_CONSUMER_PERSONOPPGAVEHENDELSE_BASE}_read"
const val KAFKA_CONSUMER_PERSONOPPGAVEHENDELSE_TOMBSTONE = "${KAFKA_CONSUMER_PERSONOPPGAVEHENDELSE_BASE}_tombstone"
const val KAFKA_CONSUMER_PERSONOPPGAVEHENDELSE_CREATE = "${KAFKA_CONSUMER_PERSONOPPGAVEHENDELSE_BASE}_create"
const val KAFKA_CONSUMER_PERSONOPPGAVEHENDELSE_UPDATE = "${KAFKA_CONSUMER_PERSONOPPGAVEHENDELSE_BASE}_update"
const val KAFKA_CONSUMER_PERSONOPPGAVEHENDELSE_UKJENT_MOTTATT =
    "${KAFKA_CONSUMER_PERSONOPPGAVEHENDELSE_BASE}_ukjent_mottatt"

val COUNT_KAFKA_CONSUMER_PERSONOPPGAVEHENDELSE_READ: Counter =
    Counter.builder(KAFKA_CONSUMER_PERSONOPPGAVEHENDELSE_READ)
        .description("Counts the number of reads from topic - personoppgavehendelse")
        .register(METRICS_REGISTRY)
val COUNT_KAFKA_CONSUMER_PERSONOPPGAVEHENDELSE_TOMBSTONE: Counter =
    Counter.builder(KAFKA_CONSUMER_PERSONOPPGAVEHENDELSE_TOMBSTONE)
        .description("Counts the number of tombstones received from topic - personoppgavehendelse")
        .register(METRICS_REGISTRY)
val COUNT_KAFKA_CONSUMER_PERSONOPPGAVEHENDELSE_CREATED_PERSONOVERSIKT_STATUS: Counter =
    Counter.builder(KAFKA_CONSUMER_PERSONOPPGAVEHENDELSE_CREATE)
        .description("Counts the number of personoversikt-status created from reading topic - personoppgavehendelse")
        .register(METRICS_REGISTRY)
val COUNT_KAFKA_CONSUMER_PERSONOPPGAVEHENDELSE_UPDATED_PERSONOVERSIKT_STATUS: Counter =
    Counter.builder(KAFKA_CONSUMER_PERSONOPPGAVEHENDELSE_UPDATE)
        .description("Counts the number of personoversikt-status updated from reading topic - personoppgavehendelse")
        .register(METRICS_REGISTRY)

val COUNT_PERSONOPPGAVEHENDELSE_UKJENT_MOTTATT: Counter =
    Counter.builder(KAFKA_CONSUMER_PERSONOPPGAVEHENDELSE_UKJENT_MOTTATT)
        .description("Counts the number of personoppgavehendelse of unknown type received}")
        .register(METRICS_REGISTRY)
