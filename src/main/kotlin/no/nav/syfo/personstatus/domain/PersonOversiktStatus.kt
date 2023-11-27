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
    val motebehovUbehandlet: Boolean? = null,
    val oppfolgingsplanLPSBistandUbehandlet: Boolean? = null,
    val dialogmotesvarUbehandlet: Boolean = false,
    val dialogmotekandidat: Boolean? = false,
    val dialogmotekandidatGeneratedAt: OffsetDateTime? = null,
    val motestatus: String? = null,
    val motestatusGeneratedAt: OffsetDateTime? = null,
    val latestOppfolgingstilfelle: Oppfolgingstilfelle? = null,
    val aktivitetskrav: AktivitetskravStatus? = null,
    val aktivitetskravStoppunkt: LocalDate? = null,
    val aktivitetskravSistVurdert: OffsetDateTime? = null,
    val aktivitetskravVurderingFrist: LocalDate? = null,
    val behandlerdialogSvarUbehandlet: Boolean = false,
    val behandlerdialogUbesvartUbehandlet: Boolean = false,
    val behandlerdialogAvvistUbehandlet: Boolean = false,
    val aktivitetskravVurderStansUbehandlet: Boolean = false,
    val trengerOppfolging: Boolean = false,
    val trengerOppfolgingFrist: LocalDate? = null,
    val behandlerBerOmBistandUbehandlet: Boolean = false,
) {
    constructor(fnr: String) : this(
        null, fnr = fnr, null, null, null,
        null, false, null, null, null,
        null, null, null, null, null, null,
        false, false, false, false, false,
        null, false,
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

fun PersonOversiktStatus.hasActiveOppgave(arenaCutoff: LocalDate): Boolean {
    return this.oppfolgingsplanLPSBistandUbehandlet == true ||
        this.dialogmotesvarUbehandlet ||
        this.isDialogmotekandidat() ||
        (this.motebehovUbehandlet == true && this.latestOppfolgingstilfelle != null) ||
        this.isActiveAktivitetskrav(arenaCutoff = arenaCutoff) ||
        hasActiveBehandlerdialogOppgave() ||
        this.aktivitetskravVurderStansUbehandlet ||
        this.trengerOppfolging || this.behandlerBerOmBistandUbehandlet
}

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
        veilederIdent = veilederIdent,
        fnr = fnr,
        navn = navn ?: "",
        enhet = enhet ?: "",
        motebehovUbehandlet = motebehovUbehandlet,
        oppfolgingsplanLPSBistandUbehandlet = oppfolgingsplanLPSBistandUbehandlet,
        dialogmotesvarUbehandlet = dialogmotesvarUbehandlet,
        dialogmotekandidat = dialogmotekandidat?.let { isDialogmotekandidat() },
        motestatus = motestatus,
        latestOppfolgingstilfelle = latestOppfolgingstilfelle?.toPersonOppfolgingstilfelleDTO(),
        aktivitetskrav = aktivitetskrav?.name,
        aktivitetskravStoppunkt = aktivitetskravStoppunkt,
        aktivitetskravSistVurdert = aktivitetskravSistVurdert?.toLocalDateTimeOslo(),
        aktivitetskravActive = isActiveAktivitetskrav(arenaCutoff = arenaCutoff),
        aktivitetskravVurderingFrist = aktivitetskravVurderingFrist,
        behandlerdialogUbehandlet = hasActiveBehandlerdialogOppgave(),
        aktivitetskravVurderStansUbehandlet = aktivitetskravVurderStansUbehandlet,
        trengerOppfolging = trengerOppfolging,
        trengerOppfolgingFrist = trengerOppfolgingFrist,
        behandlerBerOmBistandUbehandlet = behandlerBerOmBistandUbehandlet,
    )

fun PersonOversiktStatus.hasActiveBehandlerdialogOppgave(): Boolean {
    return this.behandlerdialogSvarUbehandlet ||
        this.behandlerdialogUbesvartUbehandlet ||
        this.behandlerdialogAvvistUbehandlet
}

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

        OversikthendelseType.BEHANDLERDIALOG_SVAR_MOTTATT -> this.copy(
            behandlerdialogSvarUbehandlet = true,
        )

        OversikthendelseType.BEHANDLERDIALOG_SVAR_BEHANDLET -> this.copy(
            behandlerdialogSvarUbehandlet = false,
        )

        OversikthendelseType.BEHANDLERDIALOG_MELDING_UBESVART_MOTTATT -> this.copy(
            behandlerdialogUbesvartUbehandlet = true,
        )

        OversikthendelseType.BEHANDLERDIALOG_MELDING_UBESVART_BEHANDLET -> this.copy(
            behandlerdialogUbesvartUbehandlet = false,
        )

        OversikthendelseType.BEHANDLERDIALOG_MELDING_AVVIST_MOTTATT -> this.copy(
            behandlerdialogAvvistUbehandlet = true,
        )

        OversikthendelseType.BEHANDLERDIALOG_MELDING_AVVIST_BEHANDLET -> this.copy(
            behandlerdialogAvvistUbehandlet = false,
        )

        OversikthendelseType.AKTIVITETSKRAV_VURDER_STANS_MOTTATT -> this.copy(
            aktivitetskravVurderStansUbehandlet = true,
        )

        OversikthendelseType.AKTIVITETSKRAV_VURDER_STANS_BEHANDLET -> this.copy(
            aktivitetskravVurderStansUbehandlet = false,
        )

        OversikthendelseType.BEHANDLER_BER_OM_BISTAND_MOTTATT -> this.copy(
            behandlerBerOmBistandUbehandlet = true
        )
        OversikthendelseType.BEHANDLER_BER_OM_BISTAND_BEHANDLET -> this.copy(
            behandlerBerOmBistandUbehandlet = false
        )
    }
