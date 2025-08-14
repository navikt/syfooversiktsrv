package no.nav.syfo.personstatus.infrastructure.kafka.dialogmotekandidat

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
    fnr = this.personIdentNumber,
    dialogmotekandidat = this.kandidat,
    dialogmotekandidatGeneratedAt = this.createdAt,
)
