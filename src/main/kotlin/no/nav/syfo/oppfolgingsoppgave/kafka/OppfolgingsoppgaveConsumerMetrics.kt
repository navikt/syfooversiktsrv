package no.nav.syfo.oppfolgingsoppgave.kafka

import io.micrometer.core.instrument.Counter
import no.nav.syfo.personstatus.infrastructure.METRICS_NS
import no.nav.syfo.personstatus.infrastructure.METRICS_REGISTRY

const val KAFKA_CONSUMER_TRENGER_OPPFOLGING_BASE = "${METRICS_NS}_kafka_consumer_trenger_oppfolging"
const val KAFKA_CONSUMER_TRENGER_OPPFOLGING_READ = "${KAFKA_CONSUMER_TRENGER_OPPFOLGING_BASE}_read"

val COUNT_KAFKA_CONSUMER_TRENGER_OPPFOLGING_READ: Counter = Counter.builder(KAFKA_CONSUMER_TRENGER_OPPFOLGING_READ)
    .description("Counts the number of reads from topic - huskelapp")
    .register(METRICS_REGISTRY)
