package no.nav.syfo.client.pdl

import io.micrometer.core.instrument.Counter
import no.nav.syfo.metric.METRICS_NS
import no.nav.syfo.metric.METRICS_REGISTRY

const val CALL_PDL_PERSONBOLK_BASE = "${METRICS_NS}_call_pdl_person_bolk"
const val CALL_PDL_PERSONBOLK_SUCCESS = "${CALL_PDL_PERSONBOLK_BASE}_success_count"
const val CALL_PDL_PERSONBOLK_FAIL = "${CALL_PDL_PERSONBOLK_BASE}_fail_count"

val COUNT_CALL_PDL_PERSONBOLK_SUCCESS: Counter = Counter.builder(CALL_PDL_PERSONBOLK_SUCCESS)
    .description("Counts the number of successful calls to persondatalosning - personBolk")
    .register(METRICS_REGISTRY)
val COUNT_CALL_PDL_PERSONBOLK_FAIL: Counter = Counter.builder(CALL_PDL_PERSONBOLK_FAIL)
    .description("Counts the number of failed calls to persondatalosning - personBolk")
    .register(METRICS_REGISTRY)
