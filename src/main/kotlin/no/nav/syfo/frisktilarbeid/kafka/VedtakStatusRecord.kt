package no.nav.syfo.frisktilarbeid.kafka

import no.nav.syfo.personstatus.domain.PersonOversiktStatus
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

data class VedtakStatusRecord(
    val uuid: UUID,
    val personident: String,
    val begrunnelse: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val status: Status,
    val statusAt: OffsetDateTime,
    val statusBy: String,
)

enum class Status {
    FATTET,
    FERDIG_BEHANDLET,
}

fun VedtakStatusRecord.toPersonOversiktStatus() = PersonOversiktStatus(
    veilederIdent = null,
    fnr = this.personident,
    navn = null,
    enhet = null,
    motebehovUbehandlet = null,
    oppfolgingsplanLPSBistandUbehandlet = null,
    dialogmotesvarUbehandlet = false,
    dialogmotekandidat = false,
    dialogmotekandidatGeneratedAt = null,
    motestatus = null,
    motestatusGeneratedAt = null,
    latestOppfolgingstilfelle = null,
    aktivitetskrav = null,
    aktivitetskravStoppunkt = null,
    aktivitetskravSistVurdert = null,
    aktivitetskravVurderingFrist = null,
    behandlerdialogSvarUbehandlet = false,
    behandlerdialogUbesvartUbehandlet = false,
    behandlerdialogAvvistUbehandlet = false,
    friskmeldingTilArbeidsformidlingFom = if (this.status == Status.FATTET) this.fom else null,
)
