package no.nav.syfo.infrastructure

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Timer
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry

const val METRICS_NS = "syfooversiktsrv"

val METRICS_REGISTRY = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

const val CALL_TILGANGSKONTROLL_PERSONS_BASE = "${METRICS_NS}_call_tilgangskontroll_persons"
const val CALL_TILGANGSKONTROLL_PERSONS_SUCCESS = "${CALL_TILGANGSKONTROLL_PERSONS_BASE}_success_count"
const val CALL_TILGANGSKONTROLL_PERSONS_FAIL = "${CALL_TILGANGSKONTROLL_PERSONS_BASE}_fail_count"

const val PERSONOVERSIKTSTATUS_ENHET_HENTET = "${METRICS_NS}_success_personoversikt_hentet_count"
const val METRIC_NAME_PERSONTILDELING_TILDELT = "${METRICS_NS}_success_persontildeling_tildelt_count"

const val ISTILGANGSKONTROLL_HISTOGRAM_ENHET = "${METRICS_NS}_istilgangskontroll_histogram_enhet"
const val ISTILGANGSKONTROLL_HISTOGRAM_PERSONER = "${METRICS_NS}_istilgangskontroll_histogram_personer"

const val PERSONOVERSIKT_HISTOGRAM_ENHET = "${METRICS_NS}_personoversikt_histogram_enhet"

const val KAFKA_CONSUMER_PDL_AKTOR_BASE = "${METRICS_NS}_kafka_consumer_pdl_aktor_v2"
const val KAFKA_CONSUMER_PDL_AKTOR_UPDATES = "${KAFKA_CONSUMER_PDL_AKTOR_BASE}_updates"
const val KAFKA_CONSUMER_PDL_AKTOR_TOMBSTONE = "${KAFKA_CONSUMER_PDL_AKTOR_BASE}_tombstone"

const val KAFKA_CONSUMER_PDL_PERSONHENDELSE_BASE = "${METRICS_NS}_kafka_consumer_pdl_leesah-v1"
const val KAFKA_CONSUMER_PDL_PERSONHENDELSE_UPDATES = "${KAFKA_CONSUMER_PDL_PERSONHENDELSE_BASE}_updates"
const val KAFKA_CONSUMER_PDL_PERSONHENDELSE_TOMBSTONE = "${KAFKA_CONSUMER_PDL_PERSONHENDELSE_BASE}_tombstone"

val COUNT_CALL_TILGANGSKONTROLL_PERSONS_SUCCESS: Counter = Counter.builder(CALL_TILGANGSKONTROLL_PERSONS_SUCCESS)
    .description("Counts the number of successful calls to tilgangskontroll - persons")
    .register(METRICS_REGISTRY)
val COUNT_CALL_TILGANGSKONTROLL_PERSONS_FAIL: Counter = Counter.builder(CALL_TILGANGSKONTROLL_PERSONS_FAIL)
    .description("Counts the number of failed calls to tilgangskontroll - persons")
    .register(METRICS_REGISTRY)

val COUNT_PERSONOVERSIKTSTATUS_ENHET_HENTET: Counter = Counter.builder(PERSONOVERSIKTSTATUS_ENHET_HENTET)
    .description("Counts database.oppdaterPersonMedMotebehovBehandlet(oversiktHendelse) the number of completed calls to personoversikt/enhet/{enhet}")
    .register(METRICS_REGISTRY)

val COUNT_PERSONTILDELING_TILDELT: Counter = Counter.builder(METRIC_NAME_PERSONTILDELING_TILDELT)
    .description("Counts the number of updated in db completed as a result of calls to persontildeling/registrer")
    .register(METRICS_REGISTRY)

val COUNT_KAFKA_CONSUMER_PDL_AKTOR_UPDATES: Counter =
    Counter.builder(KAFKA_CONSUMER_PDL_AKTOR_UPDATES)
        .description("Counts the number of updates in database based on identhendelse received from topic - pdl-aktor-v2")
        .register(METRICS_REGISTRY)
val COUNT_KAFKA_CONSUMER_PDL_AKTOR_TOMBSTONE: Counter =
    Counter.builder(KAFKA_CONSUMER_PDL_AKTOR_TOMBSTONE)
        .description("Counts the number of tombstones received from topic - pdl-aktor-v2")
        .register(METRICS_REGISTRY)
val COUNT_KAFKA_CONSUMER_PDL_PERSONHENDELSE_UPDATES: Counter =
    Counter.builder(KAFKA_CONSUMER_PDL_PERSONHENDELSE_UPDATES)
        .description("Counts the number of updates on names based on personhendelse received from topic - pdl.leesah-v1")
        .register(METRICS_REGISTRY)
val COUNT_KAFKA_CONSUMER_PDL_PERSONHENDELSE_TOMBSTONE: Counter =
    Counter.builder(KAFKA_CONSUMER_PDL_PERSONHENDELSE_TOMBSTONE)
        .description("Counts the number of tombstones received from topic - pdl.leesah-v1")
        .register(METRICS_REGISTRY)

val HISTOGRAM_ISTILGANGSKONTROLL_PERSONER: Timer = Timer.builder(ISTILGANGSKONTROLL_HISTOGRAM_PERSONER)
    .description("Measure the current time it takes to get a response from istilgangskontroll - personer")
    .register(METRICS_REGISTRY)

val HISTOGRAM_ISTILGANGSKONTROLL_ENHET: Timer = Timer.builder(ISTILGANGSKONTROLL_HISTOGRAM_ENHET)
    .description("Measure the current time it takes to get a response from istilgangskontroll - enhet ")
    .register(METRICS_REGISTRY)

val HISTOGRAM_PERSONOVERSIKT: Timer = Timer.builder(PERSONOVERSIKT_HISTOGRAM_ENHET)
    .description("Measure the current time it takes to get a response from personoversikt")
    .register(METRICS_REGISTRY)
