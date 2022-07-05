package no.nav.syfo.personstatus.domain

enum class OversikthendelseType {
    MOTEBEHOV_SVAR_MOTTATT,
    MOTEBEHOV_SVAR_BEHANDLET,
    OPPFOLGINGSPLANLPS_BISTAND_MOTTATT,
    OPPFOLGINGSPLANLPS_BISTAND_BEHANDLET,
    DIALOGMOTESVAR_MOTTATT,
    DIALOGMOTESVAR_BEHANDLET,
}

fun OversikthendelseType.isNotBehandling() =
    this == OversikthendelseType.MOTEBEHOV_SVAR_MOTTATT ||
        this == OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_MOTTATT ||
        this == OversikthendelseType.DIALOGMOTESVAR_MOTTATT
