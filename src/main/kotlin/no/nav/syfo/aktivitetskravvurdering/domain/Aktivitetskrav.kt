package no.nav.syfo.aktivitetskravvurdering.domain

import no.nav.syfo.domain.PersonIdent
import no.nav.syfo.personstatus.domain.PersonOversiktStatus
import java.time.LocalDate
import java.time.OffsetDateTime

data class Aktivitetskrav(
    val personIdent: PersonIdent,
    val status: AktivitetskravStatus,
    val sistVurdert: OffsetDateTime?,
    val stoppunkt: LocalDate,
    val vurderingFrist: LocalDate?,
)

enum class AktivitetskravStatus {
    NY,
    NY_VURDERING,
    AVVENT,
    UNNTAK,
    OPPFYLT,
    FORHANDSVARSEL,
    AUTOMATISK_OPPFYLT,
    STANS,
    IKKE_OPPFYLT,
    IKKE_AKTUELL,
    LUKKET,
}

fun Aktivitetskrav.toPersonOversiktStatus() = PersonOversiktStatus(
    fnr = this.personIdent.value,
    aktivitetskrav = this.status,
    aktivitetskravStoppunkt = this.stoppunkt,
    aktivitetskravSistVurdert = this.sistVurdert,
    aktivitetskravVurderingFrist = this.vurderingFrist,
)
