package no.nav.syfo.frisktilarbeid.kafka

import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.personstatus.domain.PersonOversiktStatus
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

data class VedtakFattetRecord(
    val uuid: UUID,
    val personident: PersonIdent,
    val veilederident: String,
    val createdAt: OffsetDateTime,
    val begrunnelse: String,
    val fom: LocalDate,
    val tom: LocalDate,
)

fun VedtakFattetRecord.toPersonOversiktStatus() = PersonOversiktStatus(
    veilederIdent = null,
    fnr = this.personident.value,
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
    friskmeldingTilArbeidsformidlingFom = this.fom,
)
