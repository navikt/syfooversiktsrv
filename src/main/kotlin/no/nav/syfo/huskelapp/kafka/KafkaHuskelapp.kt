package no.nav.syfo.huskelapp.kafka

import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.huskelapp.domain.Huskelapp
import java.time.OffsetDateTime
import java.util.UUID

data class KafkaHuskelapp(
    val uuid: UUID,
    val personIdent: String,
    val veilederIdent: String,
    val tekst: String,
    val isActive: Boolean,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
) {
    fun toHuskelapp() = Huskelapp(
        uuid = uuid,
        personIdent = PersonIdent(personIdent),
        isActive = isActive,
    )
}
