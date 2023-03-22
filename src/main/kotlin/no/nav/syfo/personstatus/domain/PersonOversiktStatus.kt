package no.nav.syfo.personstatus.domain

import no.nav.syfo.aktivitetskravvurdering.domain.AktivitetskravStatus
import no.nav.syfo.dialogmotestatusendring.domain.DialogmoteStatusendringType
import no.nav.syfo.domain.Virksomhetsnummer
import no.nav.syfo.personstatus.api.v2.*
import no.nav.syfo.util.*
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

data class PersonOversiktStatus(
    val veilederIdent: String?,
    val fnr: String,
    val navn: String?,
    val enhet: String?,
    val motebehovUbehandlet: Boolean?,
    val oppfolgingsplanLPSBistandUbehandlet: Boolean?,
    val dialogmotesvarUbehandlet: Boolean,
    val dialogmotekandidat: Boolean?,
    val dialogmotekandidatGeneratedAt: OffsetDateTime?,
    val motestatus: String?,
    val motestatusGeneratedAt: OffsetDateTime?,
    val latestOppfolgingstilfelle: Oppfolgingstilfelle?,
    val aktivitetskrav: AktivitetskravStatus?,
    val aktivitetskravStoppunkt: LocalDate?,
    val aktivitetskravSistVurdert: OffsetDateTime?,
    val aktivitetskravVurderingFrist: LocalDate?,
) {
    constructor(fnr: String) : this(
        null, fnr = fnr, null, null, null,
        null, false, null, null,
        null, null, null, null, null, null, null,
    )
}

fun PersonOversiktStatus.isDialogmotekandidat() =
    dialogmotekandidat == true &&
        latestOppfolgingstilfelle != null &&
        dialogmotekandidatGeneratedAt != null &&
        dialogmotekandidatGeneratedAt.toLocalDateOslo()
            .isAfter(latestOppfolgingstilfelle.oppfolgingstilfelleStart) &&
        dialogmotekandidatGeneratedAt.toLocalDateOslo().isBeforeOrEqual(LocalDate.now().minusDays(7)) &&
        noOpenDialogmoteInvitation()

fun PersonOversiktStatus.hasOpenDialogmoteInvitation() =
    motestatus == DialogmoteStatusendringType.INNKALT.name ||
        motestatus == DialogmoteStatusendringType.NYTT_TID_STED.name

fun PersonOversiktStatus.noOpenDialogmoteInvitation() = !hasOpenDialogmoteInvitation()

fun PersonOversiktStatus.isActiveAktivitetskrav(arenaCutoff: LocalDate) =
    (aktivitetskrav == AktivitetskravStatus.NY || aktivitetskrav == AktivitetskravStatus.AVVENT) &&
        aktivitetskravStoppunkt?.isAfter(arenaCutoff) ?: false

data class Oppfolgingstilfelle(
    val updatedAt: OffsetDateTime,
    val generatedAt: OffsetDateTime,
    val oppfolgingstilfelleStart: LocalDate,
    val oppfolgingstilfelleEnd: LocalDate,
    val oppfolgingstilfelleBitReferanseInntruffet: OffsetDateTime,
    val oppfolgingstilfelleBitReferanseUuid: UUID,
    val virksomhetList: List<PersonOppfolgingstilfelleVirksomhet>,
)

fun Oppfolgingstilfelle.toPersonOppfolgingstilfelleDTO() =
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

fun PersonOversiktStatus.toPersonOversiktStatusDTO(arenaCutoff: LocalDate) =
    PersonOversiktStatusDTO(
        veilederIdent = this.veilederIdent,
        fnr = this.fnr,
        navn = this.navn ?: "",
        enhet = this.enhet ?: "",
        motebehovUbehandlet = this.motebehovUbehandlet,
        oppfolgingsplanLPSBistandUbehandlet = this.oppfolgingsplanLPSBistandUbehandlet,
        dialogmotesvarUbehandlet = this.dialogmotesvarUbehandlet,
        dialogmotekandidat = this.dialogmotekandidat?.let { isDialogmotekandidat() },
        motestatus = this.motestatus,
        latestOppfolgingstilfelle = this.latestOppfolgingstilfelle?.toPersonOppfolgingstilfelleDTO(),
        aktivitetskrav = this.aktivitetskrav?.name,
        aktivitetskravStoppunkt = this.aktivitetskravStoppunkt,
        aktivitetskravSistVurdert = this.aktivitetskravSistVurdert?.toLocalDateTimeOslo(),
        aktivitetskravActive = isActiveAktivitetskrav(arenaCutoff = arenaCutoff),
        aktivitetskravVurderingFrist = this.aktivitetskravVurderingFrist,
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

        OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_MOTTATT -> this.copy(
            oppfolgingsplanLPSBistandUbehandlet = true,
        )

        OversikthendelseType.OPPFOLGINGSPLANLPS_BISTAND_BEHANDLET -> this.copy(
            oppfolgingsplanLPSBistandUbehandlet = false,
        )

        OversikthendelseType.DIALOGMOTESVAR_MOTTATT -> this.copy(
            dialogmotesvarUbehandlet = true,
        )

        OversikthendelseType.DIALOGMOTESVAR_BEHANDLET -> this.copy(
            dialogmotesvarUbehandlet = false,
        )
    }
