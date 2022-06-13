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
    moteplanleggerUbehandlet = null,
    oppfolgingsplanLPSBistandUbehandlet = null,
    dialogmotekandidat = this.kandidat,
    dialogmotekandidatGeneratedAt = this.createdAt,
    motestatus = null,
    motestatusGeneratedAt = null,
    latestOppfolgingstilfelle = null,
)
