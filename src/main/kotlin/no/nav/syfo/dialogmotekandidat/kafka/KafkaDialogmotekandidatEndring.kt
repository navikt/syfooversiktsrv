package no.nav.syfo.dialogmotekandidat.kafka

import no.nav.syfo.personstatus.domain.PersonOversiktStatus
import java.time.OffsetDateTime

data class KafkaDialogmotekandidatEndring(
    val uuid: String,
    val createdAt: OffsetDateTime,
    val personIdentNumber: String,
    val kandidat: Boolean,
    val arsak: String,
)

fun KafkaDialogmotekandidatEndring.toPersonOversiktStatus() = PersonOversiktStatus(
    veilederIdent = null,
    fnr = this.personIdentNumber,
    navn = null,
    enhet = null,
    motebehovUbehandlet = null,
    oppfolgingsplanLPSBistandUbehandlet = null,
    dialogmotesvarUbehandlet = false,
    dialogmotekandidat = this.kandidat,
    dialogmotekandidatGeneratedAt = this.createdAt,
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
)
