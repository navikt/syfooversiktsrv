package no.nav.syfo.personstatus.domain

import no.nav.syfo.domain.Virksomhetsnummer
import no.nav.syfo.personstatus.api.v2.*
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

data class PersonOversiktStatus(
    val veilederIdent: String?,
    val fnr: String,
    val navn: String?,
    val enhet: String?,
    val motebehovUbehandlet: Boolean?,
    val moteplanleggerUbehandlet: Boolean?,
    val oppfolgingsplanLPSBistandUbehandlet: Boolean?,
    val dialogmotekandidat: Boolean?,
    val latestOppfolgingstilfelle: PersonOppfolgingstilfelle?,
)

data class PersonOppfolgingstilfelle(
    val oppfolgingstilfelleUpdatedAt: OffsetDateTime,
    val oppfolgingstilfelleGeneratedAt: OffsetDateTime,
    val oppfolgingstilfelleStart: LocalDate,
    val oppfolgingstilfelleEnd: LocalDate,
    val oppfolgingstilfelleBitReferanseInntruffet: OffsetDateTime,
    val oppfolgingstilfelleBitReferanseUuid: UUID,
    val virksomhetList: List<PersonOppfolgingstilfelleVirksomhet>,
)

fun PersonOppfolgingstilfelle.toPersonOppfolgingstilfelleDTO() =
    PersonOppfolgingstilfelleDTO(
        oppfolgingstilfelleStart = this.oppfolgingstilfelleStart,
        oppfolgingstilfelleEnd = this.oppfolgingstilfelleEnd,
        virksomhetList = this.virksomhetList.toPersonOppfolgingstilfelleVirksomhetDTO()
    )

data class PersonOppfolgingstilfelleVirksomhet(
    val uuid: UUID,
    val createdAt: OffsetDateTime,
    val virksomhetsnummer: Virksomhetsnummer,
    val virksomhetsnavn: String?,
)

fun List<PersonOppfolgingstilfelleVirksomhet>.toPersonOppfolgingstilfelleVirksomhetDTO() =
    this.map { virksomhet ->
        PersonOppfolgingstilfelleVirksomhetDTO(
            virksomhetsnummer = virksomhet.virksomhetsnummer.value,
            virksomhetsnavn = virksomhet.virksomhetsnavn,
        )
    }

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
        dialogmotekandidat = this.dialogmotekandidat,
        latestOppfolgingstilfelle = this.latestOppfolgingstilfelle?.toPersonOppfolgingstilfelleDTO(),
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
