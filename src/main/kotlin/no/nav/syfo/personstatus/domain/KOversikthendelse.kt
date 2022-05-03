package no.nav.syfo.personstatus.domain

import java.time.LocalDateTime

data class KOversikthendelse(
    val fnr: String,
    val hendelseId: String,
    val enhetId: String,
    val tidspunkt: LocalDateTime
)

fun KOversikthendelse.oversikthendelseType(): OversikthendelseType? =
    OversikthendelseType.values().firstOrNull {
        it.name == this.hendelseId
    }

fun KOversikthendelse.toPersonOversiktStatus(
    oversikthendelseType: OversikthendelseType,
): PersonOversiktStatus {
    val personOversiktStatus = PersonOversiktStatus(
        veilederIdent = null,
        fnr = this.fnr,
        navn = "",
        enhet = null,
        motebehovUbehandlet = null,
        moteplanleggerUbehandlet = null,
        oppfolgingsplanLPSBistandUbehandlet = null,
        dialogmotekandidat = null,
        latestOppfolgingstilfelle = null,
    )
    return personOversiktStatus.applyHendelse(oversikthendelseType = oversikthendelseType)
}
