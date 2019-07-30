package no.nav.syfo.metric

import io.prometheus.client.Counter

const val METRICS_NS = "syfooversiktsrv"

const val METRIC_NAME_PERSONTILDELING_TILDEL = "call_persontildeling_tildel_count"
const val METRIC_NAME_PERSONTILDELING_TILDELT = "success_persontildeling_tildelt_count"

const val PERSONOVERSIKTSTATUS_ENHET_HENTET = "success_personoversikt_hentet_count"

const val OVERSIKTHENDELSE_MOTEBEHOV_SVAR_MOTTATT_OPPRETT = "oversikthendelse_motebehov_svar_mottatt_opprett_count"
const val OVERSIKTHENDELSE_MOTEBEHOV_SVAR_MOTTATT_OPPDATER = "oversikthendelse_motebehov_svar_mottatt_oppdater_count"
const val OVERSIKTHENDELSE_MOTEBEHOV_SVAR_MOTTATT_OPPDATER_ENHET = "oversikthendelse_motebehov_svar_mottatt_oppdater_enhet_count"

const val OVERSIKTHENDELSE_MOTEBEHOV_SVAR_BEHANDLET = "oversikthendelse_motebehov_svar_behandlet_count"
const val OVERSIKTHENDELSE_MOTEBEHOV_SVAR_BEHANDLET_OPPDATER_ENHET = "oversikthendelse_motebehov_svar_behandlet_oppdater_enhet_count"
const val OVERSIKTHENDELSE_MOTEBEHOV_SVAR_BEHANDLET_FEILET = "oversikthendelse_motebehov_svar_behandlet_feilet_count"

const val OVERSIKTHENDELSE_MOTEPLANLEGGER_ALLE_SVAR_MOTTATT_OPPRETT = "oversikthendelse_moteplanlegger_alle_svar_mottatt_opprett_count"
const val OVERSIKTHENDELSE_MOTEPLANLEGGER_ALLE_SVAR_MOTTATT_OPPDATER = "oversikthendelse_moteplanlegger_alle_svar_mottatt_oppdater_count"
const val OVERSIKTHENDELSE_MOTEPLANLEGGER_ALLE_SVAR_MOTTATT_OPPDATER_ENHET = "oversikthendelse_moteplanlegger_alle_svar_mottatt_oppdater_enhet_count"

const val OVERSIKTHENDELSE_MOTEPLANLEGGER_ALLE_SVAR_BEHANDLET_OPPDATER = "oversikthendelse_moteplanlegger_alle_svar_behandlet_oppdater_count"
const val OVERSIKTHENDELSE_MOTEPLANLEGGER_ALLE_SVAR_BEHANDLET_OPPDATER_ENHET = "oversikthendelse_moteplanlegger_alle_svar_behandlet_oppdater_enhet_count"
const val OVERSIKTHENDELSE_MOTEPLANLEGGER_ALLE_SVAR_BEHANDLET_FEILET = "oversikthendelse_moteplanlegger_alle_svar_behandlet_feilet_count"

const val OVERSIKTHENDELSE_UKJENT_MOTTATT = "oversikthendelse_ukjent_count"


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
        .help("Counts database.oppdaterPersonMedMotebehovBehandlet(oversiktHendelse) the number of completed calls to personoversikt/enhet/{enhet}")
        .register()

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

val COUNT_OVERSIKTHENDELSE_UKJENT_MOTTATT: Counter = Counter.build()
        .namespace(METRICS_NS)
        .name(OVERSIKTHENDELSE_UKJENT_MOTTATT)
        .help("Counts the number of oversikthendelse of unknown type received}")
        .register()


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
