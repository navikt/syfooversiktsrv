package no.nav.syfo.personstatus.domain

enum class OversikthendelseType {
    MOTEBEHOV_SVAR_MOTTATT,
    MOTEBEHOV_SVAR_BEHANDLET,
    OPPFOLGINGSPLANLPS_BISTAND_MOTTATT,
    OPPFOLGINGSPLANLPS_BISTAND_BEHANDLET,
    DIALOGMOTESVAR_MOTTATT,
    DIALOGMOTESVAR_BEHANDLET,
    BEHANDLERDIALOG_SVAR_MOTTATT,
    BEHANDLERDIALOG_SVAR_BEHANDLET,
    BEHANDLERDIALOG_MELDING_UBESVART_MOTTATT,
    BEHANDLERDIALOG_MELDING_UBESVART_BEHANDLET,
    BEHANDLERDIALOG_MELDING_AVVIST_MOTTATT,
    BEHANDLERDIALOG_MELDING_AVVIST_BEHANDLET,
    AKTIVITETSKRAV_VURDER_STANS_MOTTATT,
    AKTIVITETSKRAV_VURDER_STANS_BEHANDLET,
    HUSKELAPP_MOTTATT,
    HUSKELAPP_BEHANDLET,
}
