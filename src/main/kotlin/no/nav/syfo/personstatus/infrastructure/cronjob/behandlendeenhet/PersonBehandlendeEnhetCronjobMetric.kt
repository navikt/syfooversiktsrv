package no.nav.syfo.personstatus.infrastructure.cronjob.behandlendeenhet

import io.micrometer.core.instrument.Counter
import no.nav.syfo.personstatus.infrastructure.METRICS_NS
import no.nav.syfo.personstatus.infrastructure.METRICS_REGISTRY

const val CRONJOB_PERSON_BEHANDLENDE_ENHET_FAIL_BASE = "${METRICS_NS}_cronjob_person_behandlende_enhet"
const val CRONJOB_PERSON_BEHANDLENDE_ENHET_UPDATE = "${CRONJOB_PERSON_BEHANDLENDE_ENHET_FAIL_BASE}_update_count"
const val CRONJOB_PERSON_BEHANDLENDE_ENHET_FAIL = "${CRONJOB_PERSON_BEHANDLENDE_ENHET_FAIL_BASE}_fail_count"

val COUNT_CRONJOB_PERSON_BEHANDLENDE_ENHET_UPDATE: Counter = Counter
    .builder(CRONJOB_PERSON_BEHANDLENDE_ENHET_UPDATE)
    .description("Counts the number of updates in Cronjob - PersonBehandlendeenhetCronjob")
    .register(METRICS_REGISTRY)
val COUNT_CRONJOB_PERSON_BEHANDLENDE_ENHET_FAIL: Counter = Counter
    .builder(CRONJOB_PERSON_BEHANDLENDE_ENHET_FAIL)
    .description("Counts the number of failures in Cronjob - PersonBehandlendeenhetCronjob")
    .register(METRICS_REGISTRY)
