package no.nav.syfo.personstatus.infrastructure.cronjob.virksomhetsnavn

import io.micrometer.core.instrument.Counter
import no.nav.syfo.metric.METRICS_NS
import no.nav.syfo.metric.METRICS_REGISTRY

const val CRONJOB_OPPFOLGINGSTILFELLE_VIRKSOMHETSNAVN_BASE = "${METRICS_NS}_cronjob_oppfolgingstilfelle_virksomhetsnavn"
const val CRONJOB_OPPFOLGINGSTILFELLE_VIRKSOMHETSNAVN_UPDATE = "${CRONJOB_OPPFOLGINGSTILFELLE_VIRKSOMHETSNAVN_BASE}_update_count"
const val CRONJOB_OPPFOLGINGSTILFELLE_VIRKSOMHETSNAVN_FAIL = "${CRONJOB_OPPFOLGINGSTILFELLE_VIRKSOMHETSNAVN_BASE}_fail_count"

val COUNT_CRONJOB_OPPFOLGINGSTILFELLE_VIRKSOMHETSNAVN_UPDATE: Counter = Counter
    .builder(CRONJOB_OPPFOLGINGSTILFELLE_VIRKSOMHETSNAVN_UPDATE)
    .description("Counts the number of updates in Cronjob - PersonOppfolgingstilfelleVirksomhetnavnCronjob")
    .register(METRICS_REGISTRY)
val COUNT_CRONJOB_OPPFOLGINGSTILFELLE_VIRKSOMHETSNAVN_FAIL: Counter = Counter
    .builder(CRONJOB_OPPFOLGINGSTILFELLE_VIRKSOMHETSNAVN_FAIL)
    .description("Counts the number of failures in Cronjob - PersonOppfolgingstilfelleVirksomhetnavnCronjob")
    .register(METRICS_REGISTRY)
