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
        enhet = this.enhetId,
        motebehovUbehandlet = null,
        moteplanleggerUbehandlet = null,
        oppfolgingsplanLPSBistandUbehandlet = null,
        oppfolgingstilfeller = emptyList()
    )
    return personOversiktStatus.applyHendelse(oversikthendelseType = oversikthendelseType)
}

fun KOversikthendelse.toPersonOversiktStatus(
    oversikthendelseType: OversikthendelseType,
    personOversiktStatus: PersonOversiktStatus,
): PersonOversiktStatus {
    val newEnhetId = personOversiktStatus.enhet != this.enhetId
    val updatedPersonOversiktStatus = if (newEnhetId) {
        personOversiktStatus.copy(
            veilederIdent = null,
            enhet = this.enhetId,
        )
    } else {
        personOversiktStatus
    }
    return updatedPersonOversiktStatus.applyHendelse(oversikthendelseType = oversikthendelseType)
}
