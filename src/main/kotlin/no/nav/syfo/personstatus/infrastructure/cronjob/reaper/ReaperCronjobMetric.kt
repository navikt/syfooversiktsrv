package no.nav.syfo.personstatus.infrastructure.cronjob.reaper

import io.micrometer.core.instrument.Counter
import no.nav.syfo.metric.METRICS_NS
import no.nav.syfo.metric.METRICS_REGISTRY

const val CRONJOB_REAPER_FAIL_BASE = "${METRICS_NS}_cronjob_reaper"
const val CRONJOB_REAPER_UPDATE = "${CRONJOB_REAPER_FAIL_BASE}_update_count"
const val CRONJOB_REAPER_FAIL = "${CRONJOB_REAPER_FAIL_BASE}_fail_count"

val COUNT_CRONJOB_REAPER_UPDATE: Counter = Counter
    .builder(CRONJOB_REAPER_UPDATE)
    .description("Counts the number of updates in Cronjob - ReaperCronjob")
    .register(METRICS_REGISTRY)
val COUNT_CRONJOB_REAPER_FAIL: Counter = Counter
    .builder(CRONJOB_REAPER_FAIL)
    .description("Counts the number of failures in Cronjob - ReaperCronjob")
    .register(METRICS_REGISTRY)
