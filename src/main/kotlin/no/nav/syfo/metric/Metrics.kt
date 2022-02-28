package no.nav.syfo.metric

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

const val OVERSIKTHENDELSE_UKJENT_MOTTATT = "${METRICS_NS}_oversikthendelse_ukjent_count"

const val OVERSIKTHENDELSE_MOTEBEHOV_SVAR_MOTTATT_BASE = "${METRICS_NS}_oversikthendelse_motebehov_svar_mottatt"
const val OVERSIKTHENDELSE_MOTEBEHOV_SVAR_MOTTATT_OPPRETT = "${OVERSIKTHENDELSE_MOTEBEHOV_SVAR_MOTTATT_BASE}_opprett_count"
const val OVERSIKTHENDELSE_MOTEBEHOV_SVAR_MOTTATT_OPPDATER = "${OVERSIKTHENDELSE_MOTEBEHOV_SVAR_MOTTATT_BASE}_oppdater_count"

const val OVERSIKTHENDELSE_MOTEBEHOV_SVAR_BEHANDLET_BASE = "${METRICS_NS}_oversikthendelse_motebehov_svar_behandlet"
const val OVERSIKTHENDELSE_MOTEBEHOV_SVAR_BEHANDLET_OPPDATER = "${OVERSIKTHENDELSE_MOTEBEHOV_SVAR_BEHANDLET_BASE}_count"
const val OVERSIKTHENDELSE_MOTEBEHOV_SVAR_BEHANDLET_FEILET = "${OVERSIKTHENDELSE_MOTEBEHOV_SVAR_BEHANDLET_BASE}_feilet_count"

const val OVERSIKTHENDELSE_MOTEPLANLEGGER_ALLE_SVAR_MOTTATT_BASE = "${METRICS_NS}_oversikthendelse_moteplanlegger_alle_svar_mottatt"
const val OVERSIKTHENDELSE_MOTEPLANLEGGER_ALLE_SVAR_MOTTATT_OPPRETT = "${OVERSIKTHENDELSE_MOTEPLANLEGGER_ALLE_SVAR_MOTTATT_BASE}_opprett_count"
const val OVERSIKTHENDELSE_MOTEPLANLEGGER_ALLE_SVAR_MOTTATT_OPPDATER = "${OVERSIKTHENDELSE_MOTEPLANLEGGER_ALLE_SVAR_MOTTATT_BASE}_oppdater_count"

const val OVERSIKTHENDELSE_MOTEPLANLEGGER_ALLE_SVAR_BEHANDLET_BASE = "${METRICS_NS}_oversikthendelse_moteplanlegger_alle_svar_behandlet"
const val OVERSIKTHENDELSE_MOTEPLANLEGGER_ALLE_SVAR_BEHANDLET_OPPDATER = "${OVERSIKTHENDELSE_MOTEPLANLEGGER_ALLE_SVAR_BEHANDLET_BASE}_oppdater_count"
const val OVERSIKTHENDELSE_MOTEPLANLEGGER_ALLE_SVAR_BEHANDLET_FEILET = "${OVERSIKTHENDELSE_MOTEPLANLEGGER_ALLE_SVAR_BEHANDLET_BASE}_feilet_count"

const val OVERSIKTHENDELSE_OPPFOLGINGSPLANLPS_BISTAND_MOTTATT_BASE = "${METRICS_NS}_oversikthendelse_oppfolgingsplanlps_bistand_mottatt"
const val OVERSIKTHENDELSE_OPPFOLGINGSPLANLPS_BISTAND_MOTTATT_OPPRETT = "${OVERSIKTHENDELSE_OPPFOLGINGSPLANLPS_BISTAND_MOTTATT_BASE}_opprett_count"
const val OVERSIKTHENDELSE_OPPFOLGINGSPLANLPS_BISTAND_MOTTATT_OPPDATER = "${OVERSIKTHENDELSE_OPPFOLGINGSPLANLPS_BISTAND_MOTTATT_BASE}_oppdater_count"

const val OVERSIKTHENDELSE_OPPFOLGINGSPLANLPS_BISTAND_BEHANDLET_BASE = "${METRICS_NS}_oversikthendelse_oppfolgingsplanlps_bistand_behandlet"
const val OVERSIKTHENDELSE_OPPFOLGINGSPLANLPS_BISTAND_BEHANDLET_OPPDATER = "${OVERSIKTHENDELSE_OPPFOLGINGSPLANLPS_BISTAND_BEHANDLET_BASE}_oppdater_count"
const val OVERSIKTHENDELSE_OPPFOLGINGSPLANLPS_BISTAND_BEHANDLET_FEILET = "${OVERSIKTHENDELSE_OPPFOLGINGSPLANLPS_BISTAND_BEHANDLET_BASE}_feilet_count"

const val OVERSIKTHENDELSETILFELLE_UGYLDIG_MOTTATT = "${METRICS_NS}_oversikthendelsetilfelle_ugyldig_mottatt"

const val OVERSIKTHENDELSETILFELLE_INGEN_AKTIVITET_BASE = "${METRICS_NS}_oversikthendelsetilfelle_ingen_aktivitet"
const val OVERSIKTHENDELSETILFELLE_INGEN_AKTIVITET_OPPRETT = "${OVERSIKTHENDELSETILFELLE_INGEN_AKTIVITET_BASE}_opprett_count"
const val OVERSIKTHENDELSETILFELLE_INGEN_AKTIVITET_OPPDATER = "${OVERSIKTHENDELSETILFELLE_INGEN_AKTIVITET_BASE}_oppdater_count"
const val OVERSIKTHENDELSETILFELLE_INGEN_AKTIVITET_ENHET = "${OVERSIKTHENDELSETILFELLE_INGEN_AKTIVITET_BASE}_oppdater_enhet_count"

const val OVERSIKTHENDELSETILFELLE_GRADERT_BASE = "${METRICS_NS}_oversikthendelsetilfelle_gradert"
const val OVERSIKTHENDELSETILFELLE_GRADERT_OPPRETT = "${OVERSIKTHENDELSETILFELLE_GRADERT_BASE}_opprett_count"
const val OVERSIKTHENDELSETILFELLE_GRADERT_OPPDATER = "${OVERSIKTHENDELSETILFELLE_GRADERT_BASE}_oppdater_count"
const val OVERSIKTHENDELSETILFELLE_GRADERT_OPPDATER_ENHET = "${OVERSIKTHENDELSETILFELLE_GRADERT_BASE}_oppdater_enhet_count"

const val SYFOTILGANGSKONTROLL_HISTOGRAM_ENHET = "${METRICS_NS}_syfotilgangskontroll_histogram_enhet"
const val SYFOTILGANGSKONTROLL_HISTOGRAM_PERSONER = "${METRICS_NS}_syfotilgangskontroll_histogram_personer"

const val PERSONOVERSIKT_HISTOGRAM_ENHET = "${METRICS_NS}_personoversikt_histogram_enhet"

val COUNT_CALL_TILGANGSKONTROLL_PERSONS_SUCCESS: Counter = Counter.builder(CALL_TILGANGSKONTROLL_PERSONS_SUCCESS)
    .description("Counts the number of successful calls to syfo-tilgangskontroll - persons")
    .register(METRICS_REGISTRY)
val COUNT_CALL_TILGANGSKONTROLL_PERSONS_FAIL: Counter = Counter.builder(CALL_TILGANGSKONTROLL_PERSONS_FAIL)
    .description("Counts the number of failed calls to syfo-tilgangskontroll - persons")
    .register(METRICS_REGISTRY)

val COUNT_PERSONOVERSIKTSTATUS_ENHET_HENTET: Counter = Counter.builder(PERSONOVERSIKTSTATUS_ENHET_HENTET)
    .description("Counts database.oppdaterPersonMedMotebehovBehandlet(oversiktHendelse) the number of completed calls to personoversikt/enhet/{enhet}")
    .register(METRICS_REGISTRY)

val COUNT_PERSONTILDELING_TILDELT: Counter = Counter.builder(METRIC_NAME_PERSONTILDELING_TILDELT)
    .description("Counts the number of updated in db completed as a result of calls to persontildeling/registrer")
    .register(METRICS_REGISTRY)

val COUNT_OVERSIKTHENDELSE_UKJENT_MOTTATT: Counter = Counter.builder(OVERSIKTHENDELSE_UKJENT_MOTTATT)
    .description("Counts the number of oversikthendelse of unknown type received}")
    .register(METRICS_REGISTRY)

val COUNT_OVERSIKTHENDELSE_MOTEBEHOV_SVAR_MOTTATT_OPPRETT: Counter =
    Counter.builder(OVERSIKTHENDELSE_MOTEBEHOV_SVAR_MOTTATT_OPPRETT)
        .description("Counts the number of oversikthendelse of type motebehovsvar-mottatt resulting in create}")
        .register(METRICS_REGISTRY)

val COUNT_OVERSIKTHENDELSE_MOTEBEHOV_SVAR_MOTTATT_OPPDATER: Counter =
    Counter.builder(OVERSIKTHENDELSE_MOTEBEHOV_SVAR_MOTTATT_OPPDATER)
        .description("Counts the number of oversikthendelse of type motebehovsvar-mottatt resulting in update}")
        .register(METRICS_REGISTRY)

val COUNT_OVERSIKTHENDELSE_MOTEBEHOV_SVAR_BEHANDLET_OPPDATER: Counter =
    Counter.builder(OVERSIKTHENDELSE_MOTEBEHOV_SVAR_BEHANDLET_OPPDATER)
        .description("Counts the number of oversikthendelse of type motebehovsvar-behandlet resulting in update")
        .register(METRICS_REGISTRY)

val COUNT_OVERSIKTHENDELSE_MOTEBEHOVSSVAR_BEHANDLET_FEILET: Counter =
    Counter.builder(OVERSIKTHENDELSE_MOTEBEHOV_SVAR_BEHANDLET_FEILET)
        .description("Counts the number of oversikthendelse of type motebehovsvar-behandlet that failed to update missing person")
        .register(METRICS_REGISTRY)

val COUNT_OVERSIKTHENDELSE_MOTEPLANLEGGER_ALLE_SVAR_MOTTATT_OPPRETT: Counter =
    Counter.builder(OVERSIKTHENDELSE_MOTEPLANLEGGER_ALLE_SVAR_MOTTATT_OPPRETT)
        .description("Counts the number of oversikthendelse of type moteplanlegger-alle-svar-mottatt resulting in create}")
        .register(METRICS_REGISTRY)
val COUNT_OVERSIKTHENDELSE_MOTEPLANLEGGER_ALLE_SVAR_MOTTATT_OPPDATER: Counter =
    Counter.builder(OVERSIKTHENDELSE_MOTEPLANLEGGER_ALLE_SVAR_MOTTATT_OPPDATER)
        .description("Counts the number of oversikthendelse of type moteplanlegger-alle-svar-mottatt resulting in update}")
        .register(METRICS_REGISTRY)

val COUNT_OVERSIKTHENDELSE_MOTEPLANLEGGER_ALLE_SVAR_BEHANDLET_OPPDATER: Counter =
    Counter.builder(OVERSIKTHENDELSE_MOTEPLANLEGGER_ALLE_SVAR_BEHANDLET_OPPDATER)
        .description("Counts the number of oversikthendelse of type moteplanlegger-alle-svar-behandlet resulting in update}")
        .register(METRICS_REGISTRY)
val COUNT_OVERSIKTHENDELSE_MOTEPLANLEGGER_ALLE_SVAR_BEHANDLET_FEILET: Counter =
    Counter.builder(OVERSIKTHENDELSE_MOTEPLANLEGGER_ALLE_SVAR_BEHANDLET_FEILET)
        .description("Counts the number of oversikthendelse of type moteplanlegger-alle-svar-behandlet that failed to update missing person")
        .register(METRICS_REGISTRY)

val COUNT_OVERSIKTHENDELSE_OPPFOLGINGSPLANLPS_BISTAND_MOTTATT_OPPRETT: Counter =
    Counter.builder(OVERSIKTHENDELSE_OPPFOLGINGSPLANLPS_BISTAND_MOTTATT_OPPRETT)
        .description("Counts the number of oversikthendelse of type OPPFOLGINGSPLANLPS_BISTAND_MOTTATTt resulting in create}")
        .register(METRICS_REGISTRY)
val COUNT_OVERSIKTHENDELSE_OPPFOLGINGSPLANLPS_BISTAND_MOTTATT_OPPDATER: Counter =
    Counter.builder(OVERSIKTHENDELSE_OPPFOLGINGSPLANLPS_BISTAND_MOTTATT_OPPDATER)
        .description("Counts the number of oversikthendelse of type OPPFOLGINGSPLANLPS_BISTAND_MOTTATT resulting in update}")
        .register(METRICS_REGISTRY)

val COUNT_OVERSIKTHENDELSE_OPPFOLGINGSPLANLPS_BISTAND_BEHANDLET_OPPDATER: Counter =
    Counter.builder(OVERSIKTHENDELSE_OPPFOLGINGSPLANLPS_BISTAND_BEHANDLET_OPPDATER)
        .description("Counts the number of oversikthendelse of type OPPFOLGINGSPLANLPS_BISTAND_BEHANDLET resulting in update}")
        .register(METRICS_REGISTRY)
val COUNT_OVERSIKTHENDELSE_OPPFOLGINGSPLANLPS_BISTAND_BEHANDLET_FEILET: Counter =
    Counter.builder(OVERSIKTHENDELSE_OPPFOLGINGSPLANLPS_BISTAND_BEHANDLET_FEILET)
        .description("Counts the number of oversikthendelse of type OPPFOLGINGSPLANLPS_BISTAND_BEHANDLET that failed to update missing person")
        .register(METRICS_REGISTRY)

val COUNT_OVERSIKTHENDELSETILFELLE_UGDYLGIG_MOTTATT: Counter = Counter.builder(OVERSIKTHENDELSETILFELLE_UGYLDIG_MOTTATT)
    .description("Counts the number of oversikthendelsetilfeller skipped due to invalid data}")
    .register(METRICS_REGISTRY)

val COUNT_OVERSIKTHENDELSETILFELLE_INGEN_AKTIVITET_OPPRETT: Counter =
    Counter.builder(OVERSIKTHENDELSETILFELLE_INGEN_AKTIVITET_OPPRETT)
        .description("Counts the number of oversikthendelsetilfeller  with Ingen Aktivitetresulting in creation of PERSON_OVERSIKT_STATUS row")
        .register(METRICS_REGISTRY)
val COUNT_OVERSIKTHENDELSETILFELLE_INGEN_AKTIVITET_OPPDATER: Counter =
    Counter.builder(OVERSIKTHENDELSETILFELLE_INGEN_AKTIVITET_OPPDATER)
        .description("Counts the number of oversikthendelsetilfeller with Ingen Aktivitet resulting in update}")
        .register(METRICS_REGISTRY)
val COUNT_OVERSIKTHENDELSETILFELLE_INGEN_AKTIVITET_OPPDATER_ENHET: Counter =
    Counter.builder(OVERSIKTHENDELSETILFELLE_INGEN_AKTIVITET_ENHET)
        .description("Counts the number of oversikthendelsetilfeller  with Ingen Aktivitet resulting in update with new enhetId}")
        .register(METRICS_REGISTRY)

val COUNT_OVERSIKTHENDELSETILFELLE_GRADERT_OPPRETT: Counter = Counter.builder(OVERSIKTHENDELSETILFELLE_GRADERT_OPPRETT)
    .description("Counts the number of graderte oversikthendelsetilfeller resulting in creation of PERSON_OVERSIKT_STATUS row")
    .register(METRICS_REGISTRY)
val COUNT_OVERSIKTHENDELSETILFELLE_GRADERT_OPPDATER: Counter =
    Counter.builder(OVERSIKTHENDELSETILFELLE_GRADERT_OPPDATER)
        .description("Counts the number of graderte oversikthendelsetilfeller resulting in update}")
        .register(METRICS_REGISTRY)
val COUNT_OVERSIKTHENDELSETILFELLE_GRADERT_OPPDATER_ENHET: Counter =
    Counter.builder(OVERSIKTHENDELSETILFELLE_GRADERT_OPPDATER_ENHET)
        .description("Counts the number of graderte oversikthendelsetilfeller resulting in update with new enhetId}")
        .register(METRICS_REGISTRY)

val HISTOGRAM_SYFOTILGANGSKONTROLL_PERSONER: Timer = Timer.builder(SYFOTILGANGSKONTROLL_HISTOGRAM_PERSONER)
    .description("Measure the current time it takes to get a response from Syfotilgangskontroll - personer")
    .register(METRICS_REGISTRY)

val HISTOGRAM_SYFOTILGANGSKONTROLL_ENHET: Timer = Timer.builder(SYFOTILGANGSKONTROLL_HISTOGRAM_ENHET)
    .description("Measure the current time it takes to get a response from Syfotilgangskontroll - enhet ")
    .register(METRICS_REGISTRY)

val HISTOGRAM_PERSONOVERSIKT: Timer = Timer.builder(PERSONOVERSIKT_HISTOGRAM_ENHET)
    .description("Measure the current time it takes to get a response from personoversikt")
    .register(METRICS_REGISTRY)
