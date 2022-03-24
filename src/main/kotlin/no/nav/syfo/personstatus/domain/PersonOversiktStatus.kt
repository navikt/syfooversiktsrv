package no.nav.syfo.personstatus.domain

import no.nav.syfo.personstatus.api.v2.PersonOversiktStatusDTO
import no.nav.syfo.personstatus.api.v2.toOppfolgingstilfelleDTO
import java.time.LocalDate
import java.time.OffsetDateTime

data class PersonOversiktStatus(
    val veilederIdent: String?,
    val fnr: String,
    val navn: String?,
    val enhet: String?,
    val motebehovUbehandlet: Boolean?,
    val moteplanleggerUbehandlet: Boolean?,
    val oppfolgingsplanLPSBistandUbehandlet: Boolean?,
    val oppfolgingstilfeller: List<Oppfolgingstilfelle>,
    val latestOppfolgingstilfelle: PersonOppfolgingstilfelle?
)

data class PersonOppfolgingstilfelle(
    val oppfolgingstilfelleUpdatedAt: OffsetDateTime,
    val oppfolgingstilfelleGeneratedAt: OffsetDateTime,
    val oppfolgingstilfelleStart: LocalDate,
    val oppfolgingstilfelleEnd: LocalDate,
    val oppfolgingstilfelleBitReferanseInntruffet: OffsetDateTime,
    val virksomhetList: List<PersonOppfolgingstilfelleVirskomhet>,
)

data class PersonOppfolgingstilfelleVirskomhet(
    val virksomhetsnummer: String,
    val virskomhetsnavn: String?,
)

fun List<PersonOversiktStatus>.addPersonName(
    personIdentNameMap: Map<String, String>,
): List<PersonOversiktStatus> {
    if (personIdentNameMap.isEmpty()) {
        return this
    }
    return this.map { personOversiktStatus ->
        val personIdent = personOversiktStatus.fnr
        if (personOversiktStatus.navn.isNullOrEmpty()) {
            personOversiktStatus.copy(
                navn = personIdentNameMap[personIdent]
            )
        } else {
            personOversiktStatus
        }
    }
}

fun PersonOversiktStatus.toPersonOversiktStatusDTO() =
    PersonOversiktStatusDTO(
        veilederIdent = this.veilederIdent,
        fnr = this.fnr,
        navn = this.navn ?: "",
        enhet = this.enhet ?: "",
        motebehovUbehandlet = this.motebehovUbehandlet,
        moteplanleggerUbehandlet = this.moteplanleggerUbehandlet,
        oppfolgingsplanLPSBistandUbehandlet = this.oppfolgingsplanLPSBistandUbehandlet,
        oppfolgingstilfeller = this.oppfolgingstilfeller.map { oppfolgingstilfelle ->
            oppfolgingstilfelle.toOppfolgingstilfelleDTO()
        }
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
