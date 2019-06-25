package no.nav.syfo.metric

import io.prometheus.client.Counter

const val METRICS_NS = "syfooversiktsrv"

const val METRIC_NAME_PERSONTILDELING_TILDEL = "call_persontildeling_tildel_count"
const val METRIC_NAME_PERSONTILDELING_TILDELT = "success_persontildeling_tildelt_count"

const val PERSONOVERSIKTSTATUS_ENHET_HENTET = "success_personoversikt_hentet_count"

val COUNT_PERSONTILDELING_TILDEL: Counter = Counter.build()
        .namespace(METRICS_NS)
        .name(METRIC_NAME_PERSONTILDELING_TILDEL)
        .help("Counts the number of POST-calls to endpoint persontildeling/registrer")
        .register()

val COUNT_PERSONTILDELING_TILDELT: Counter = Counter.build()
        .namespace(METRICS_NS)
        .name(METRIC_NAME_PERSONTILDELING_TILDELT)
        .help("Counts the number of updated in db completed as a result of calls to persontildeling/registrer")
        .register()

val COUNT_PERSONOVERSIKTSTATUS_ENHET_HENTET: Counter = Counter.build()
        .namespace(METRICS_NS)
        .name(PERSONOVERSIKTSTATUS_ENHET_HENTET)
        .help("Counts the number of completed calls to personoversikt/enhet/{enhet}")
        .register()
