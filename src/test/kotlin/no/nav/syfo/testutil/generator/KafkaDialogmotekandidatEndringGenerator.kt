package no.nav.syfo.testutil.generator

import no.nav.syfo.dialogmotekandidat.kafka.KafkaDialogmotekandidatEndring
import java.time.OffsetDateTime
import java.util.UUID

fun generateKafkaDialogmotekandidatEndringStoppunkt(
    personIdent: String,
    createdAt: OffsetDateTime,
) = KafkaDialogmotekandidatEndring(
    uuid = UUID.randomUUID().toString(),
    createdAt = createdAt,
    personIdentNumber = personIdent,
    kandidat = true,
    arsak = "STOPPUNKT"
)

fun generateKafkaDialogmotekandidatEndringUnntak(
    personIdent: String,
    createdAt: OffsetDateTime,
) = KafkaDialogmotekandidatEndring(
    uuid = UUID.randomUUID().toString(),
    createdAt = createdAt,
    personIdentNumber = personIdent,
    kandidat = false,
    arsak = "UNNTAK"
)
