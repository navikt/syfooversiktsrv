package no.nav.syfo.metric

import io.prometheus.client.Counter
import io.prometheus.client.Histogram

const val METRICS_NS = "syfooversiktsrv"

const val CALL_TILGANGSKONTROLL_PERSONS_SUCCESS = "call_tilgangskontroll_persons_success_count"
const val CALL_TILGANGSKONTROLL_PERSONS_FAIL = "call_tilgangskontroll_persons_fail_count"
val COUNT_CALL_TILGANGSKONTROLL_PERSONS_SUCCESS: Counter = Counter.build()
        .namespace(METRICS_NS)
        .name(CALL_TILGANGSKONTROLL_PERSONS_SUCCESS)
        .help("Counts the number of successful calls to syfo-tilgangskontroll - persons")
        .register()
val COUNT_CALL_TILGANGSKONTROLL_PERSONS_FAIL: Counter = Counter.build()
        .namespace(METRICS_NS)
        .name(CALL_TILGANGSKONTROLL_PERSONS_FAIL)
        .help("Counts the number of failed calls to syfo-tilgangskontroll - persons")
        .register()


const val PERSONOVERSIKTSTATUS_ENHET_HENTET = "success_personoversikt_hentet_count"
val COUNT_PERSONOVERSIKTSTATUS_ENHET_HENTET: Counter = Counter.build()
        .namespace(METRICS_NS)
        .name(PERSONOVERSIKTSTATUS_ENHET_HENTET)
        .help("Counts database.oppdaterPersonMedMotebehovBehandlet(oversiktHendelse) the number of completed calls to personoversikt/enhet/{enhet}")
        .register()


const val METRIC_NAME_PERSONTILDELING_TILDEL = "call_persontildeling_tildel_count"
val COUNT_PERSONTILDELING_TILDEL: Counter = Counter.build()
        .namespace(METRICS_NS)
        .name(METRIC_NAME_PERSONTILDELING_TILDEL)
        .help("Counts the number of POST-calls to endpoint persontildeling/registrer")
        .register()


const val METRIC_NAME_PERSONTILDELING_TILDELT = "success_persontildeling_tildelt_count"
val COUNT_PERSONTILDELING_TILDELT: Counter = Counter.build()
        .namespace(METRICS_NS)
        .name(METRIC_NAME_PERSONTILDELING_TILDELT)
        .help("Counts the number of updated in db completed as a result of calls to persontildeling/registrer")
        .register()


const val OVERSIKTHENDELSE_UKJENT_MOTTATT = "oversikthendelse_ukjent_count"
val COUNT_OVERSIKTHENDELSE_UKJENT_MOTTATT: Counter = Counter.build()
        .namespace(METRICS_NS)
        .name(OVERSIKTHENDELSE_UKJENT_MOTTATT)
        .help("Counts the number of oversikthendelse of unknown type received}")
        .register()


const val OVERSIKTHENDELSE_MOTEBEHOV_SVAR_MOTTATT_OPPRETT = "oversikthendelse_motebehov_svar_mottatt_opprett_count"
const val OVERSIKTHENDELSE_MOTEBEHOV_SVAR_MOTTATT_OPPDATER = "oversikthendelse_motebehov_svar_mottatt_oppdater_count"
const val OVERSIKTHENDELSE_MOTEBEHOV_SVAR_MOTTATT_OPPDATER_ENHET = "oversikthendelse_motebehov_svar_mottatt_oppdater_enhet_count"

val COUNT_OVERSIKTHENDELSE_MOTEBEHOV_SVAR_MOTTATT_OPPRETT: Counter = Counter.build()
        .namespace(METRICS_NS)
        .name(OVERSIKTHENDELSE_MOTEBEHOV_SVAR_MOTTATT_OPPRETT)
        .help("Counts the number of oversikthendelse of type motebehovsvar-mottatt resulting in create}")
        .register()

val COUNT_OVERSIKTHENDELSE_MOTEBEHOV_SVAR_MOTTATT_OPPDATER: Counter = Counter.build()
        .namespace(METRICS_NS)
        .name(OVERSIKTHENDELSE_MOTEBEHOV_SVAR_MOTTATT_OPPDATER)
        .help("Counts the number of oversikthendelse of type motebehovsvar-mottatt resulting in update}")
        .register()

val COUNT_OVERSIKTHENDELSE_MOTEBEHOV_SVAR_MOTTATT_OPPDATER_ENHET: Counter = Counter.build()
        .namespace(METRICS_NS)
        .name(OVERSIKTHENDELSE_MOTEBEHOV_SVAR_MOTTATT_OPPDATER_ENHET)
        .help("Counts the number of oversikthendelse of type motebehovsvar-mottatt resulting in update with new enhetId}")
        .register()


const val OVERSIKTHENDELSE_MOTEBEHOV_SVAR_BEHANDLET = "oversikthendelse_motebehov_svar_behandlet_count"
const val OVERSIKTHENDELSE_MOTEBEHOV_SVAR_BEHANDLET_OPPDATER_ENHET = "oversikthendelse_motebehov_svar_behandlet_oppdater_enhet_count"
const val OVERSIKTHENDELSE_MOTEBEHOV_SVAR_BEHANDLET_FEILET = "oversikthendelse_motebehov_svar_behandlet_feilet_count"

val COUNT_OVERSIKTHENDELSE_MOTEBEHOVSSVAR_BEHANDLET: Counter = Counter.build()
        .namespace(METRICS_NS)
        .name(OVERSIKTHENDELSE_MOTEBEHOV_SVAR_BEHANDLET)
        .help("Counts the number of oversikthendelse of type motebehovsvar-behandlet resulting in update")
        .register()

val COUNT_OVERSIKTHENDELSE_MOTEBEHOVSSVAR_BEHANDLET_OPPDATER_ENHET: Counter = Counter.build()
        .namespace(METRICS_NS)
        .name(OVERSIKTHENDELSE_MOTEBEHOV_SVAR_BEHANDLET_OPPDATER_ENHET)
        .help("Counts the number of oversikthendelse of type motebehovsvar-behandlet resulting in update with new enhetId")
        .register()

val COUNT_OVERSIKTHENDELSE_MOTEBEHOVSSVAR_BEHANDLET_FEILET: Counter = Counter.build()
        .namespace(METRICS_NS)
        .name(OVERSIKTHENDELSE_MOTEBEHOV_SVAR_BEHANDLET_FEILET)
        .help("Counts the number of oversikthendelse of type motebehovsvar-behandlet that failed to update missing person")
        .register()


const val OVERSIKTHENDELSE_MOTEPLANLEGGER_ALLE_SVAR_MOTTATT_OPPRETT = "oversikthendelse_moteplanlegger_alle_svar_mottatt_opprett_count"
const val OVERSIKTHENDELSE_MOTEPLANLEGGER_ALLE_SVAR_MOTTATT_OPPDATER = "oversikthendelse_moteplanlegger_alle_svar_mottatt_oppdater_count"
const val OVERSIKTHENDELSE_MOTEPLANLEGGER_ALLE_SVAR_MOTTATT_OPPDATER_ENHET = "oversikthendelse_moteplanlegger_alle_svar_mottatt_oppdater_enhet_count"

val COUNT_OVERSIKTHENDELSE_MOTEPLANLEGGER_ALLE_SVAR_MOTTATT_OPPRETT: Counter = Counter.build()
        .namespace(METRICS_NS)
        .name(OVERSIKTHENDELSE_MOTEPLANLEGGER_ALLE_SVAR_MOTTATT_OPPRETT)
        .help("Counts the number of oversikthendelse of type moteplanlegger-alle-svar-mottatt resulting in create}")
        .register()
val COUNT_OVERSIKTHENDELSE_MOTEPLANLEGGER_ALLE_SVAR_MOTTATT_OPPDATER: Counter = Counter.build()
        .namespace(METRICS_NS)
        .name(OVERSIKTHENDELSE_MOTEPLANLEGGER_ALLE_SVAR_MOTTATT_OPPDATER)
        .help("Counts the number of oversikthendelse of type moteplanlegger-alle-svar-mottatt resulting in update}")
        .register()
val COUNT_OVERSIKTHENDELSE_MOTEPLANLEGGER_ALLE_SVAR_MOTTATT_OPPDATER_ENHET: Counter = Counter.build()
        .namespace(METRICS_NS)
        .name(OVERSIKTHENDELSE_MOTEPLANLEGGER_ALLE_SVAR_MOTTATT_OPPDATER_ENHET)
        .help("Counts the number of oversikthendelse of type moteplanlegger-alle-svar-mottatt resulting in update with new enhetId}")
        .register()


const val OVERSIKTHENDELSE_MOTEPLANLEGGER_ALLE_SVAR_BEHANDLET_OPPDATER = "oversikthendelse_moteplanlegger_alle_svar_behandlet_oppdater_count"
const val OVERSIKTHENDELSE_MOTEPLANLEGGER_ALLE_SVAR_BEHANDLET_OPPDATER_ENHET = "oversikthendelse_moteplanlegger_alle_svar_behandlet_oppdater_enhet_count"
const val OVERSIKTHENDELSE_MOTEPLANLEGGER_ALLE_SVAR_BEHANDLET_FEILET = "oversikthendelse_moteplanlegger_alle_svar_behandlet_feilet_count"

val COUNT_OVERSIKTHENDELSE_MOTEPLANLEGGER_ALLE_SVAR_BEHANDLET_OPPDATER: Counter = Counter.build()
        .namespace(METRICS_NS)
        .name(OVERSIKTHENDELSE_MOTEPLANLEGGER_ALLE_SVAR_BEHANDLET_OPPDATER)
        .help("Counts the number of oversikthendelse of type moteplanlegger-alle-svar-behandlet resulting in update}")
        .register()
val COUNT_OVERSIKTHENDELSE_MOTEPLANLEGGER_ALLE_SVAR_BEHANDLET_OPPDATER_ENHET: Counter = Counter.build()
        .namespace(METRICS_NS)
        .name(OVERSIKTHENDELSE_MOTEPLANLEGGER_ALLE_SVAR_BEHANDLET_OPPDATER_ENHET)
        .help("Counts the number of oversikthendelse of type moteplanlegger-alle-svar-behandlet resulting in update with new enhetId}")
        .register()
val COUNT_OVERSIKTHENDELSE_MOTEPLANLEGGER_ALLE_SVAR_BEHANDLET_FEILET: Counter = Counter.build()
        .namespace(METRICS_NS)
        .name(OVERSIKTHENDELSE_MOTEPLANLEGGER_ALLE_SVAR_BEHANDLET_FEILET)
        .help("Counts the number of oversikthendelse of type moteplanlegger-alle-svar-behandlet that failed to update missing person")
        .register()


const val OVERSIKTHENDELSETILFELLE_UGYLDIG_MOTTATT = "oversikthendelsetilfelle_ugyldig_mottatt"
val COUNT_OVERSIKTHENDELSETILFELLE_UGDYLGIG_MOTTATT: Counter = Counter.build()
        .namespace(METRICS_NS)
        .name(OVERSIKTHENDELSETILFELLE_UGYLDIG_MOTTATT)
        .help("Counts the number of oversikthendelsetilfeller skipped due to invalid data}")
        .register()


const val OVERSIKTHENDELSETILFELLE_INGEN_AKTIVITET_OPPRETT = "oversikthendelsetilfelle_ingen_aktivitet_opprett_count"
const val OVERSIKTHENDELSETILFELLE_INGEN_AKTIVITET_OPPDATER = "oversikthendelsetilfelle_ingen_aktivitet_oppdater_count"
const val OVERSIKTHENDELSETILFELLE_INGEN_AKTIVITET_ENHET = "oversikthendelsetilfelle_ingen_aktivitet_oppdater_enhet_count"

val COUNT_OVERSIKTHENDELSETILFELLE_INGEN_AKTIVITET_OPPRETT: Counter = Counter.build()
        .namespace(METRICS_NS)
        .name(OVERSIKTHENDELSETILFELLE_INGEN_AKTIVITET_OPPRETT)
        .help("Counts the number of oversikthendelsetilfeller  with Ingen Aktivitetresulting in creation of PERSON_OVERSIKT_STATUS row")
        .register()
val COUNT_OVERSIKTHENDELSETILFELLE_INGEN_AKTIVITET_OPPDATER: Counter = Counter.build()
        .namespace(METRICS_NS)
        .name(OVERSIKTHENDELSETILFELLE_INGEN_AKTIVITET_OPPDATER)
        .help("Counts the number of oversikthendelsetilfeller with Ingen Aktivitet resulting in update}")
        .register()
val COUNT_OVERSIKTHENDELSETILFELLE_INGEN_AKTIVITET_OPPDATER_ENHET: Counter = Counter.build()
        .namespace(METRICS_NS)
        .name(OVERSIKTHENDELSETILFELLE_INGEN_AKTIVITET_ENHET)
        .help("Counts the number of oversikthendelsetilfeller  with Ingen Aktivitet resulting in update with new enhetId}")
        .register()


const val OVERSIKTHENDELSETILFELLE_GRADERT_OPPRETT = "oversikthendelsetilfelle_gradert_opprett_count"
const val OVERSIKTHENDELSETILFELLE_GRADERT_OPPDATER = "oversikthendelsetilfelle_gradert_oppdater_count"
const val OVERSIKTHENDELSETILFELLE_GRADERT_OPPDATER_ENHET = "oversikthendelsetilfelle_gradert_oppdater_enhet_count"

val COUNT_OVERSIKTHENDELSETILFELLE_GRADERT_OPPRETT: Counter = Counter.build()
        .namespace(METRICS_NS)
        .name(OVERSIKTHENDELSETILFELLE_GRADERT_OPPRETT)
        .help("Counts the number of graderte oversikthendelsetilfeller resulting in creation of PERSON_OVERSIKT_STATUS row")
        .register()
val COUNT_OVERSIKTHENDELSETILFELLE_GRADERT_OPPDATER: Counter = Counter.build()
        .namespace(METRICS_NS)
        .name(OVERSIKTHENDELSETILFELLE_GRADERT_OPPDATER)
        .help("Counts the number of graderte oversikthendelsetilfeller resulting in update}")
        .register()
val COUNT_OVERSIKTHENDELSETILFELLE_GRADERT_OPPDATER_ENHET: Counter = Counter.build()
        .namespace(METRICS_NS)
        .name(OVERSIKTHENDELSETILFELLE_GRADERT_OPPDATER_ENHET)
        .help("Counts the number of graderte oversikthendelsetilfeller resulting in update with new enhetId}")
        .register()


const val SYFOTILGANGSKONTROLL_HISTOGRAM_ENHET = "syfotilgangskontroll_histogram_enhet"
const val SYFOTILGANGSKONTROLL_HISTOGRAM_PERSONER = "syfotilgangskontroll_histogram_personer"

const val PERSONOVERSIKT_HISTOGRAM_ENHET = "personoversikt_histogram_enhet"

val HISTOGRAM_SYFOTILGANGSKONTROLL_PERSONER: Histogram = Histogram.build()
        .namespace(METRICS_NS)
        .name(SYFOTILGANGSKONTROLL_HISTOGRAM_PERSONER)
        .help("Measure the current time it takes to get a response from Syfotilgangskontroll - personer")
        .register()

val HISTOGRAM_SYFOTILGANGSKONTROLL_ENHET: Histogram = Histogram.build()
        .namespace(METRICS_NS)
        .name(SYFOTILGANGSKONTROLL_HISTOGRAM_ENHET)
        .help("Measure the current time it takes to get a response from Syfotilgangskontroll - enhet ")
        .register()

val HISTOGRAM_PERSONOVERSIKT: Histogram = Histogram.build()
        .namespace(METRICS_NS)
        .name(PERSONOVERSIKT_HISTOGRAM_ENHET)
        .help("Measure the current time it takes to get a response from personoversikt")
        .register()
