package no.nav.syfo.trengeroppfolging.kafka

import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.trengeroppfolging.domain.TrengerOppfolging
import java.time.OffsetDateTime
import java.util.UUID

data class KafkaHuskelapp(
    val uuid: UUID,
    val personIdent: String,
    val veilederIdent: String,
    val tekst: String?,
    val oppfolgingsgrunner: List<String>,
    val isActive: Boolean,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
) {
    fun toTrengerOppfolging() = TrengerOppfolging(
        uuid = uuid,
        personIdent = PersonIdent(personIdent),
        isActive = isActive,
    )
}
