package no.nav.syfo.personstatus.domain

data class PersonOversiktStatus(
    val veilederIdent: String?,
    val fnr: String,
    val navn: String,
    val enhet: String,
    val motebehovUbehandlet: Boolean?,
    val moteplanleggerUbehandlet: Boolean?,
    val oppfolgingsplanLPSBistandUbehandlet: Boolean?,
    val oppfolgingstilfeller: List<Oppfolgingstilfelle>
)

fun PersonOversiktStatus.applyHendelse(
    oversikthendelseType: OversikthendelseType,
): PersonOversiktStatus =
    when (oversikthendelseType) {
        OversikthendelseType.MOTEBEHOV_SVAR_MOTTATT -> this.copy(
            motebehovUbehandlet = true,
        )
        OversikthendelseType.MOTEBEHOV_SVAR_BEHANDLET -> this.copy(
            motebehovUbehandlet = false,
        )
        OversikthendelseType.MOTEPLANLEGGER_ALLE_SVAR_MOTTATT -> this.copy(
            moteplanleggerUbehandlet = true,
        )
        OversikthendelseType.MOTEPLANLEGGER_ALLE_SVAR_BEHANDLET -> this.copy(
            moteplanleggerUbehandlet = false,
        )
        OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_MOTTATT -> this.copy(
            oppfolgingsplanLPSBistandUbehandlet = true,
        )
        OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_BEHANDLET -> this.copy(
            oppfolgingsplanLPSBistandUbehandlet = false,
        )
    }
